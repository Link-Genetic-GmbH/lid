"""
LinkID Resolver - FastAPI Implementation

A high-performance async HTTP resolver for LinkID persistent identifiers.
Implements the LinkID resolution specification with caching, authentication,
and federated resolver discovery.
"""

import os
import time
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any, List

import structlog
import uvicorn
from fastapi import FastAPI, HTTPException, Depends, Request, Response, Query, Path
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.responses import RedirectResponse, JSONResponse
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, ValidationError
from prometheus_client import Counter, Histogram, generate_latest

from services.resolver_service import ResolverService
from services.cache_service import CacheService
from services.registry_service import RegistryService
from services.auth_service import AuthService
from models import LinkIDRecord, ResolutionRequest, RegistrationRequest
from config import settings
from seed import load_seed

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    wrapper_class=structlog.stdlib.BoundLogger,
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()

# Prometheus metrics
REQUEST_COUNT = Counter(
    'linkid_requests_total',
    'Total LinkID resolution requests',
    ['method', 'endpoint', 'status']
)

REQUEST_DURATION = Histogram(
    'linkid_request_duration_seconds',
    'LinkID request duration',
    ['method', 'endpoint']
)

RESOLUTION_SUCCESS = Counter(
    'linkid_resolutions_successful_total',
    'Successful LinkID resolutions'
)

RESOLUTION_FAILURE = Counter(
    'linkid_resolutions_failed_total',
    'Failed LinkID resolutions',
    ['error_type']
)

CACHE_HITS = Counter(
    'linkid_cache_hits_total',
    'Cache hits'
)

# Global services
resolver_service: Optional[ResolverService] = None
cache_service: Optional[CacheService] = None
registry_service: Optional[RegistryService] = None
auth_service: Optional[AuthService] = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    global resolver_service, cache_service, registry_service, auth_service

    logger.info("Starting LinkID Resolver")

    # Initialize services
    cache_service = CacheService()
    registry_service = RegistryService()
    auth_service = AuthService()
    resolver_service = ResolverService(registry_service, cache_service, logger)

    # Connect to databases
    await registry_service.connect()
    await cache_service.connect()

    # Optional: load seed data when configured
    if settings.seed_file:
        try:
            created_ids = await load_seed(registry_service, settings.seed_file)
            logger.info("Seed loaded", count=len(created_ids))
        except Exception as e:
            logger.error("Seed load failed", error=str(e))

    logger.info("LinkID Resolver started successfully")

    yield

    # Cleanup
    logger.info("Shutting down LinkID Resolver")
    await cache_service.cleanup()
    await registry_service.cleanup()
    logger.info("LinkID Resolver shutdown complete")

# Create FastAPI app
app = FastAPI(
    title="LinkID Resolver",
    description="A persistent identifier resolver for the Web",
    version="1.0.0",
    docs_url="/docs" if settings.environment == "development" else None,
    redoc_url="/redoc" if settings.environment == "development" else None,
    lifespan=lifespan
)

# Add middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["*"],
)

app.add_middleware(GZipMiddleware, minimum_size=1000)

# Security
security = HTTPBearer(auto_error=False)

# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()

    # Process request
    response = await call_next(request)

    # Log request
    duration = time.time() - start_time

    REQUEST_COUNT.labels(
        method=request.method,
        endpoint=request.url.path,
        status=response.status_code
    ).inc()

    REQUEST_DURATION.labels(
        method=request.method,
        endpoint=request.url.path
    ).observe(duration)

    logger.info(
        "HTTP request",
        method=request.method,
        path=request.url.path,
        status_code=response.status_code,
        duration_ms=round(duration * 1000, 2),
        user_agent=request.headers.get("user-agent"),
        client_ip=request.client.host if request.client else None
    )

    return response

# Health check endpoint
@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": time.time(),
        "version": "1.0.0",
        "services": {
            "registry": await registry_service.health_check() if registry_service else False,
            "cache": await cache_service.health_check() if cache_service else False
        }
    }

# Metrics endpoint
@app.get("/metrics")
async def metrics():
    """Prometheus metrics endpoint"""
    return Response(generate_latest(), media_type="text/plain")

# Well-known endpoint for resolver discovery
@app.get("/.well-known/linkid-resolver")
async def well_known_resolver(request: Request):
    """Resolver discovery endpoint"""
    base_url = f"{request.url.scheme}://{request.headers.get('host', request.client.host)}"

    return {
        "resolver": {
            "version": "1.0",
            "endpoints": {
                "resolve": f"{base_url}/resolve/{{id}}",
                "register": f"{base_url}/register",
                "update": f"{base_url}/resolve/{{id}}",
                "withdraw": f"{base_url}/resolve/{{id}}"
            },
            "capabilities": [
                "content-negotiation",
                "caching",
                "authentication",
                "rate-limiting",
                "federation",
                "semantic-resolution"
            ],
            "supportedFormats": [
                "application/linkid+json",
                "application/json",
                "text/html"
            ],
            "rateLimits": {
                "perMinute": settings.rate_limit_per_minute,
                "perHour": settings.rate_limit_per_hour
            }
        }
    }

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Extract and validate user from authorization header"""
    if not credentials:
        return None

    return await auth_service.authenticate(credentials.credentials)

# Main resolution endpoint
@app.get("/resolve/{linkid}")
async def resolve_linkid(
    linkid: str = Path(..., regex=r"^[A-Za-z0-9._~-]{32,64}$"),
    format: Optional[str] = Query(None, regex=r"^(pdf|html|json|xml|txt)$"),
    lang: Optional[str] = Query(None, regex=r"^[a-z]{2}(-[A-Z]{2})?$"),
    version: Optional[int] = Query(None, ge=1),
    at: Optional[str] = Query(None),
    metadata: Optional[bool] = Query(False),
    request: Request = None
):
    """
    Resolve a LinkID to its current resource location

    - **linkid**: The LinkID identifier (32-64 chars, alphanumeric + ._~-)
    - **format**: Preferred format (pdf, html, json, xml, txt)
    - **lang**: Preferred language (ISO 639-1 code)
    - **version**: Specific version number
    - **at**: Timestamp for historical resolution
    - **metadata**: Return metadata instead of redirect
    """
    try:
        request_params = ResolutionRequest(
            format=format,
            language=lang,
            version=version,
            timestamp=at,
            accept_header=request.headers.get("accept", "*/*"),
            accept_language=request.headers.get("accept-language"),
            prefer_redirect=not metadata
        )

        logger.info("Resolving LinkID", linkid=linkid, params=request_params.dict())

        result = await resolver_service.resolve(linkid, request_params)

        if result.type == "redirect":
            RESOLUTION_SUCCESS.inc()

            response = RedirectResponse(
                url=result.uri,
                status_code=301 if result.permanent else 302
            )

            response.headers.update({
                "Cache-Control": f"max-age={result.cache_ttl or 3600}",
                "Link": f'<https://w3id.org/linkid/{linkid}>; rel="canonical"',
                "X-LinkID-Resolver": request.headers.get("host", "resolver.linkid.org"),
                "X-LinkID-Quality": str(result.quality or 1.0)
            })

            return response

        elif result.type == "metadata":
            RESOLUTION_SUCCESS.inc()

            return JSONResponse(
                content=result.data,
                headers={
                    "Content-Type": "application/linkid+json",
                    "Cache-Control": f"max-age={result.cache_ttl or 1800}",
                    "ETag": result.etag,
                    "Vary": "Accept, Accept-Language"
                }
            )

    except ValueError as e:
        RESOLUTION_FAILURE.labels(error_type="validation").inc()
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        error_type = getattr(e, 'code', 'unknown')
        RESOLUTION_FAILURE.labels(error_type=error_type).inc()

        logger.error("Resolution error", linkid=linkid, error=str(e))

        if hasattr(e, 'code'):
            if e.code == "LINKID_NOT_FOUND":
                raise HTTPException(
                    status_code=404,
                    detail={"error": "LinkID not found", "linkId": linkid}
                )
            elif e.code == "LINKID_WITHDRAWN":
                raise HTTPException(
                    status_code=410,
                    detail={
                        "error": "LinkID withdrawn",
                        "linkId": linkid,
                        "tombstone": getattr(e, 'tombstone', None)
                    }
                )

        raise HTTPException(status_code=500, detail="Internal resolver error")

# Registration endpoint
@app.post("/register", status_code=201)
async def register_linkid(
    registration: RegistrationRequest,
    user = Depends(get_current_user)
):
    """
    Register a new LinkID

    Requires authentication with write permissions.
    """
    if not user:
        raise HTTPException(status_code=401, detail="Authentication required")

    if not auth_service.has_scope(user, "write"):
        raise HTTPException(status_code=403, detail="Write permission required")

    try:
        result = await resolver_service.register({
            "target_uri": registration.target_uri,
            "media_type": registration.media_type,
            "language": registration.language,
            "metadata": registration.metadata,
            "issuer": user.sub
        })

        logger.info("Registered LinkID", linkid=result.id, issuer=user.sub)

        return {
            "id": result.id,
            "uri": f"https://w3id.org/linkid/{result.id}",
            "created": result.created
        }

    except Exception as e:
        logger.error("Registration error", error=str(e), user=user.sub)
        raise HTTPException(status_code=500, detail="Registration failed")

# Update endpoint
@app.put("/resolve/{linkid}")
async def update_linkid(
    linkid: str = Path(..., regex=r"^[A-Za-z0-9._~-]{32,64}$"),
    updates: Dict[str, Any] = None,
    user = Depends(get_current_user)
):
    """
    Update an existing LinkID

    Requires authentication and ownership of the LinkID.
    """
    if not user:
        raise HTTPException(status_code=401, detail="Authentication required")

    try:
        await resolver_service.update(linkid, updates, user.sub)

        logger.info("Updated LinkID", linkid=linkid, user=user.sub)

        return {
            "id": linkid,
            "updated": time.time()
        }

    except Exception as e:
        logger.error("Update error", linkid=linkid, error=str(e), user=user.sub)

        if hasattr(e, 'code'):
            if e.code == "LINKID_NOT_FOUND":
                raise HTTPException(status_code=404, detail="LinkID not found")
            elif e.code == "UNAUTHORIZED":
                raise HTTPException(status_code=403, detail="Not authorized")

        raise HTTPException(status_code=500, detail="Update failed")

# Withdrawal endpoint
@app.delete("/resolve/{linkid}")
async def withdraw_linkid(
    linkid: str = Path(..., regex=r"^[A-Za-z0-9._~-]{32,64}$"),
    withdrawal_data: Dict[str, Any] = None,
    user = Depends(get_current_user)
):
    """
    Withdraw a LinkID

    Requires authentication and ownership of the LinkID.
    """
    if not user:
        raise HTTPException(status_code=401, detail="Authentication required")

    try:
        await resolver_service.withdraw(linkid, withdrawal_data, user.sub)

        logger.info("Withdrew LinkID", linkid=linkid, user=user.sub)

        return {
            "id": linkid,
            "withdrawn": time.time(),
            "reason": withdrawal_data.get("reason") if withdrawal_data else None
        }

    except Exception as e:
        logger.error("Withdrawal error", linkid=linkid, error=str(e), user=user.sub)

        if hasattr(e, 'code'):
            if e.code == "LINKID_NOT_FOUND":
                raise HTTPException(status_code=404, detail="LinkID not found")
            elif e.code == "UNAUTHORIZED":
                raise HTTPException(status_code=403, detail="Not authorized")

        raise HTTPException(status_code=500, detail="Withdrawal failed")

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.environment == "development",
        log_level=settings.log_level.lower(),
        access_log=True
    )