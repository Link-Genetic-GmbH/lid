from __future__ import annotations

import time
import uuid
from typing import Any, Dict, Optional, Tuple


class Tombstone:
    def __init__(self, reason: Optional[str] = None, at: Optional[float] = None) -> None:
        self.reason = reason
        self.at = at or time.time()


class RegistryRecord:
    def __init__(
        self,
        link_id: str,
        target_uri: str,
        media_type: Optional[str] = None,
        language: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> None:
        self.id = link_id
        self.target_uri = target_uri
        self.media_type = media_type
        self.language = language
        self.metadata = metadata or {}
        self.created = time.time()
        self.updated = self.created
        self.version = 1
        self.withdrawn: Optional[Tombstone] = None


class LinkIdNotFoundError(Exception):
    def __init__(self, link_id: str) -> None:
        self.code = "LINKID_NOT_FOUND"
        self.link_id = link_id


class LinkIdWithdrawnError(Exception):
    def __init__(self, link_id: str, tombstone: Tombstone) -> None:
        self.code = "LINKID_WITHDRAWN"
        self.link_id = link_id
        self.tombstone = {"reason": tombstone.reason, "at": tombstone.at}


class UnauthorizedError(Exception):
    def __init__(self) -> None:
        self.code = "UNAUTHORIZED"


class RegistryService:
    """In-memory registry for demo purposes.

    Stores LinkID records and supports basic versioning and withdrawal.
    """

    def __init__(self) -> None:
        self._store: Dict[str, RegistryRecord] = {}

    async def connect(self) -> bool:
        return True

    async def cleanup(self) -> bool:
        return True

    async def health_check(self) -> bool:
        return True

    def _generate_link_id(self) -> str:
        # 32-char uppercase hex, matches allowed charset
        return uuid.uuid4().hex.upper()

    async def create(self, payload: Dict[str, Any], issuer: str) -> RegistryRecord:
        link_id = self._generate_link_id()
        record = RegistryRecord(
            link_id=link_id,
            target_uri=payload.get("target_uri") or payload.get("targetUri"),
            media_type=payload.get("media_type") or payload.get("mediaType"),
            language=payload.get("language"),
            metadata=payload.get("metadata") or {},
        )
        record.metadata.setdefault("issuer", issuer)
        self._store[link_id] = record
        return record

    async def get(self, link_id: str) -> RegistryRecord:
        record = self._store.get(link_id)
        if not record:
            raise LinkIdNotFoundError(link_id)
        if record.withdrawn is not None:
            raise LinkIdWithdrawnError(link_id, record.withdrawn)
        return record

    async def get_any(self, link_id: str) -> RegistryRecord:
        record = self._store.get(link_id)
        if not record:
            raise LinkIdNotFoundError(link_id)
        return record

    async def update(self, link_id: str, updates: Dict[str, Any], user_sub: str) -> None:
        record = await self.get_any(link_id)
        # Ownership check (best-effort for demo)
        issuer = record.metadata.get("issuer")
        if issuer and issuer != user_sub:
            raise UnauthorizedError()

        changed = False
        if "target_uri" in updates or "targetUri" in updates:
            record.target_uri = updates.get("target_uri") or updates.get("targetUri")
            changed = True
        if "media_type" in updates or "mediaType" in updates:
            record.media_type = updates.get("media_type") or updates.get("mediaType")
            changed = True
        if "language" in updates:
            record.language = updates["language"]
            changed = True
        if "metadata" in updates and isinstance(updates["metadata"], dict):
            record.metadata.update(updates["metadata"]) 
            changed = True

        if changed:
            record.version += 1
            record.updated = time.time()

    async def withdraw(self, link_id: str, data: Optional[Dict[str, Any]], user_sub: str) -> None:
        record = await self.get_any(link_id)
        issuer = record.metadata.get("issuer")
        if issuer and issuer != user_sub:
            raise UnauthorizedError()
        record.withdrawn = Tombstone(reason=(data or {}).get("reason"))

    async def resolve(self, link_id: str) -> Tuple[str, RegistryRecord]:
        record = await self.get(link_id)
        return record.target_uri, record


