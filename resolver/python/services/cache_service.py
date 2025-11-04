from __future__ import annotations

import time
from typing import Any, Dict, Optional, Tuple


class CacheService:
    """Very small ephemeral cache for demo purposes."""

    def __init__(self) -> None:
        self._store: Dict[str, Tuple[Any, float]] = {}

    async def connect(self) -> bool:
        return True

    async def cleanup(self) -> bool:
        self._store.clear()
        return True

    async def health_check(self) -> bool:
        return True

    def get(self, key: str) -> Optional[Any]:
        entry = self._store.get(key)
        if not entry:
            return None
        value, expires_at = entry
        if expires_at < time.time():
            self._store.pop(key, None)
            return None
        return value

    def set(self, key: str, value: Any, ttl: int = 300) -> None:
        self._store[key] = (value, time.time() + float(ttl))

    def clear(self) -> None:
        self._store.clear()


