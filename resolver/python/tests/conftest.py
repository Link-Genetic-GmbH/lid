import sys
import types
from typing import Any, Dict, Optional

import pytest


def _install_stub_modules():
    """Install stub modules for services, models, and config before importing main.

    The production repo doesn't ship Python implementations for these, so we
    provide minimal in-memory stubs sufficient for exercising the FastAPI app.
    """

    # Create parent packages
    services_pkg = types.ModuleType("services")
    sys.modules.setdefault("services", services_pkg)

    # services.resolver_service
    resolver_service_mod = types.ModuleType("services.resolver_service")

    class ResolutionResultStub:
        def __init__(
            self,
            type: str,
            uri: Optional[str] = None,
            permanent: bool = False,
            cache_ttl: Optional[int] = None,
            quality: Optional[float] = None,
            data: Optional[Dict[str, Any]] = None,
            etag: Optional[str] = None,
        ) -> None:
            self.type = type
            self.uri = uri
            self.permanent = permanent
            self.cache_ttl = cache_ttl
            self.quality = quality
            self.data = data
            self.etag = etag

    class ResolverService:
        async def resolve(self, linkid, request_params):
            # Default behavior: redirect to a synthetic location
            return ResolutionResultStub(
                type="redirect",
                uri=f"https://example.org/resource/{linkid}",
                permanent=False,
                cache_ttl=300,
                quality=1.0,
            )

        async def register(self, payload: Dict[str, Any]):
            class _RegisterResult:
                def __init__(self) -> None:
                    self.id = "A" * 32
                    self.created = 1700000000

            return _RegisterResult()

        async def update(self, linkid: str, updates: Dict[str, Any], user_sub: str):
            return None

        async def withdraw(self, linkid: str, data: Optional[Dict[str, Any]], user_sub: str):
            return None

    resolver_service_mod.ResolverService = ResolverService
    sys.modules["services.resolver_service"] = resolver_service_mod

    # services.cache_service
    cache_service_mod = types.ModuleType("services.cache_service")

    class CacheService:
        async def connect(self):
            return True

        async def cleanup(self):
            return True

        async def health_check(self):
            return True

    cache_service_mod.CacheService = CacheService
    sys.modules["services.cache_service"] = cache_service_mod

    # services.registry_service
    registry_service_mod = types.ModuleType("services.registry_service")

    class RegistryService:
        async def connect(self):
            return True

        async def cleanup(self):
            return True

        async def health_check(self):
            return True

    registry_service_mod.RegistryService = RegistryService
    sys.modules["services.registry_service"] = registry_service_mod

    # services.auth_service
    auth_service_mod = types.ModuleType("services.auth_service")

    class _User:
        def __init__(self, sub: str, scopes: Optional[list[str]] = None) -> None:
            self.sub = sub
            self.scopes = scopes or []

    class AuthService:
        async def authenticate(self, token: str):
            if token == "unauthorized" or token is None:
                return None
            # Default: authenticated user with read+write scopes
            return _User(sub="user-1", scopes=["read", "write"])

        def has_scope(self, user: _User, scope: str) -> bool:
            return scope in getattr(user, "scopes", [])

    auth_service_mod.AuthService = AuthService
    sys.modules["services.auth_service"] = auth_service_mod

    # models
    models_mod = types.ModuleType("models")
    try:
        from pydantic import BaseModel
    except Exception:  # pragma: no cover - pydantic is in requirements
        class BaseModel:  # type: ignore
            pass

    class LinkIDRecord(BaseModel):
        id: str  # minimal placeholder

    class ResolutionRequest(BaseModel):
        format: Optional[str] = None
        language: Optional[str] = None
        version: Optional[int] = None
        timestamp: Optional[str] = None
        accept_header: Optional[str] = None
        accept_language: Optional[str] = None
        prefer_redirect: bool = True
        # Compatibility for code paths calling .dict() (Pydantic v1 style)
        def dict(self, *args, **kwargs):  # type: ignore[override]
            try:
                return self.model_dump(*args, **kwargs)  # Pydantic v2
            except Exception:  # pragma: no cover
                return super().dict(*args, **kwargs)  # Pydantic v1

    class RegistrationRequest(BaseModel):
        target_uri: str
        media_type: Optional[str] = None
        language: Optional[str] = None
        metadata: Optional[Dict[str, Any]] = None

    models_mod.LinkIDRecord = LinkIDRecord
    models_mod.ResolutionRequest = ResolutionRequest
    models_mod.RegistrationRequest = RegistrationRequest
    sys.modules["models"] = models_mod

    # config
    config_mod = types.ModuleType("config")

    class _Settings:
        environment: str = "development"
        allowed_origins = ["*"]
        rate_limit_per_minute: int = 600
        rate_limit_per_hour: int = 3600
        host: str = "127.0.0.1"
        port: int = 8080
        log_level: str = "INFO"

    config_mod.settings = _Settings()
    sys.modules["config"] = config_mod


@pytest.fixture(scope="session")
def app_module():
    """Import the FastAPI app module (`main`) with stubbed dependencies installed."""
    _install_stub_modules()
    import importlib

    # Ensure resolver/python is on sys.path so `import main` works
    from pathlib import Path
    resolver_dir = Path(__file__).resolve().parents[1]
    if str(resolver_dir) not in sys.path:
        sys.path.insert(0, str(resolver_dir))

    main = importlib.import_module("main")
    return main


@pytest.fixture()
def client(app_module):
    """Yield a TestClient with lifespan events executed."""
    from fastapi.testclient import TestClient

    with TestClient(app_module.app) as c:
        yield c


