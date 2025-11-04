from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, Optional

from .registry_service import RegistryService, LinkIdNotFoundError, LinkIdWithdrawnError
from .cache_service import CacheService


@dataclass
class ResolutionResult:
    type: str  # "redirect" | "metadata"
    uri: Optional[str] = None
    permanent: bool = False
    cache_ttl: Optional[int] = None
    quality: Optional[float] = None
    data: Optional[Dict[str, Any]] = None
    etag: Optional[str] = None


class ResolverService:
    def __init__(self, registry: RegistryService, cache: CacheService, logger) -> None:
        self.registry = registry
        self.cache = cache
        self.logger = logger

    async def resolve(self, linkid: str, params) -> ResolutionResult:
        try:
            target_uri, record = await self.registry.resolve(linkid)
        except LinkIdNotFoundError as e:
            raise e
        except LinkIdWithdrawnError as e:
            raise e
        except Exception as e:  # pragma: no cover - safety net
            raise e

        if getattr(params, "prefer_redirect", True):
            return ResolutionResult(
                type="redirect",
                uri=target_uri,
                permanent=False,
                cache_ttl=300,
                quality=1.0,
            )

        # metadata response
        data = {
            "id": record.id,
            "target": record.target_uri,
            "mediaType": record.media_type,
            "language": record.language,
            "created": record.created,
            "updated": record.updated,
            "version": record.version,
            "metadata": record.metadata,
        }
        return ResolutionResult(
            type="metadata",
            data=data,
            cache_ttl=120,
            etag=f'W/"{record.version}-{int(record.updated)}"',
        )

    async def register(self, payload: Dict[str, Any]):
        issuer = payload.get("issuer") or "unknown"
        record = await self.registry.create(payload, issuer)

        # For demo purposes, return a simple struct with required fields
        class _Result:
            def __init__(self, id: str, created: float) -> None:
                self.id = id
                self.created = created

        return _Result(record.id, record.created)

    async def update(self, linkid: str, updates: Dict[str, Any], user_sub: str) -> None:
        await self.registry.update(linkid, updates or {}, user_sub)

    async def withdraw(self, linkid: str, data: Optional[Dict[str, Any]], user_sub: str) -> None:
        await self.registry.withdraw(linkid, data or {}, user_sub)


