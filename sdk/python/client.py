"""Sample LinkID client for Python.

This module demonstrates how to interact with a LinkID resolver as
specified in the project documentation. It covers the common operations:
resolution, registration, update, and withdrawal.
"""

from __future__ import annotations

import re
import time
from dataclasses import dataclass, replace
from typing import Any, Dict, Optional, Tuple, Union

import requests
from requests import Response, Session
from requests.exceptions import RequestException, Timeout


class LinkIDError(Exception):
    """Base error for LinkID client issues."""

    def __init__(self, message: str, code: Optional[str] = None) -> None:
        super().__init__(message)
        self.code = code


class NetworkError(LinkIDError):
    """Raised when the client cannot reach the resolver."""


class ValidationError(LinkIDError):
    """Raised when request parameters are invalid."""


class LinkIDNotFoundError(LinkIDError):
    """Raised when the resolver cannot find the requested LinkID."""

    def __init__(self, link_id: str, message: str) -> None:
        super().__init__(message, code="NOT_FOUND")
        self.link_id = link_id


class LinkIDWithdrawnError(LinkIDError):
    """Raised when the requested LinkID has been withdrawn."""

    def __init__(self, link_id: str, message: str, tombstone: Optional[Dict[str, Any]] = None) -> None:
        super().__init__(message, code="WITHDRAWN")
        self.link_id = link_id
        self.tombstone = tombstone or {}


@dataclass
class RedirectResolution:
    """Represents a resolver redirect response."""

    link_id: str
    uri: str
    resolver: str
    cached: bool = False
    quality: Optional[float] = None


@dataclass
class MetadataResolution:
    """Represents a resolver metadata response."""

    link_id: str
    data: Dict[str, Any]
    resolver: str
    cached: bool = False


ResolutionResult = Union[RedirectResolution, MetadataResolution]


class TTLCache:
    """Very small in-memory cache with TTL semantics."""

    def __init__(self, max_size: int = 1024, default_ttl: int = 3600) -> None:
        self.max_size = max_size
        self.default_ttl = default_ttl
        self._store: Dict[str, Tuple[ResolutionResult, float]] = {}

    def get(self, key: str) -> Optional[ResolutionResult]:
        entry = self._store.get(key)
        if not entry:
            return None
        value, expires_at = entry
        if expires_at < time.time():
            self._store.pop(key, None)
            return None
        return value

    def set(self, key: str, value: ResolutionResult, ttl: Optional[int] = None) -> None:
        if len(self._store) >= self.max_size:
            # naive eviction strategy for sample purposes
            self._store.pop(next(iter(self._store)))
        expiry = time.time() + float(ttl if ttl is not None else self.default_ttl)
        self._store[key] = (value, expiry)

    def clear(self) -> None:
        self._store.clear()


class LinkIDClient:
    """Small convenience client for LinkID resolvers."""

    def __init__(
        self,
        resolver_url: str = "https://resolver.linkid.org",
        *,
        api_key: Optional[str] = None,
        timeout: float = 10.0,
        retries: int = 3,
        caching: bool = True,
        cache_ttl: int = 3600,
        session: Optional[Session] = None,
        headers: Optional[Dict[str, str]] = None,
    ) -> None:
        self.resolver_url = resolver_url.rstrip("/")
        self.api_key = api_key
        self.timeout = timeout
        self.retries = max(1, retries)
        self.caching = caching
        self.cache = TTLCache(default_ttl=cache_ttl)
        self.session = session or requests.Session()
        self.default_headers = headers or {}

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------
    def resolve(
        self,
        link_id: str,
        *,
        format: Optional[str] = None,
        language: Optional[str] = None,
        version: Optional[int] = None,
        timestamp: Optional[str] = None,
        metadata: bool = False,
        bypass_cache: bool = False,
        headers: Optional[Dict[str, str]] = None,
    ) -> ResolutionResult:
        """Resolve a LinkID to its current target or metadata."""

        self._validate_link_id(link_id)
        cache_key = self._cache_key(
            link_id,
            {
                "format": format,
                "language": language,
                "version": version,
                "timestamp": timestamp,
                "metadata": metadata,
            },
        )

        if self.caching and not bypass_cache:
            cached = self.cache.get(cache_key)
            if cached is not None:
                return replace(cached, cached=True)

        url = f"{self.resolver_url}/resolve/{link_id}"
        params = self._build_params(format, language, version, timestamp, metadata)
        request_headers = self._build_headers(headers)

        response = self._send_request("GET", url, params=params, headers=request_headers, allow_redirects=False)

        if response.status_code in {301, 302, 303, 307, 308}:
            location = response.headers.get("Location") or response.headers.get("location")
            if not location:
                raise LinkIDError("Resolver returned redirect without Location header")

            result = RedirectResolution(
                link_id=link_id,
                uri=location,
                resolver=self._resolver_used(response),
                quality=self._parse_quality(response.headers.get("X-LinkID-Quality")),
                cached=False,
            )
        elif response.status_code == 200:
            payload = response.json()
            result = MetadataResolution(
                link_id=link_id,
                data=payload,
                resolver=self._resolver_used(response),
                cached=False,
            )
        else:
            self._handle_error(response, link_id)

        if self.caching:
            ttl = self._cache_ttl(response.headers.get("Cache-Control"))
            self.cache.set(cache_key, result, ttl=ttl)

        return result

    def register(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """Register a new LinkID."""

        self._require_api_key()
        self._validate_registration_request(request)

        url = f"{self.resolver_url}/register"
        response = self._send_request("POST", url, json=request, headers=self._build_headers())

        if response.status_code not in {200, 201}:
            self._handle_error(response)
        return response.json()

    def update(self, link_id: str, request: Dict[str, Any]) -> None:
        """Update an existing LinkID record."""

        self._require_api_key()
        self._validate_link_id(link_id)

        url = f"{self.resolver_url}/resolve/{link_id}"
        response = self._send_request("PUT", url, json=request, headers=self._build_headers())

        if not response.ok:
            self._handle_error(response, link_id)
        if self.caching:
            self.cache.clear()

    def withdraw(self, link_id: str, request: Optional[Dict[str, Any]] = None) -> None:
        """Withdraw a LinkID and create a tombstone record."""

        self._require_api_key()
        self._validate_link_id(link_id)

        url = f"{self.resolver_url}/resolve/{link_id}"
        response = self._send_request("DELETE", url, json=request or {}, headers=self._build_headers())

        if not response.ok:
            self._handle_error(response, link_id)
        if self.caching:
            self.cache.clear()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------
    def _send_request(
        self,
        method: str,
        url: str,
        *,
        params: Optional[Dict[str, Any]] = None,
        json: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
        allow_redirects: bool = True,
    ) -> Response:
        last_error: Optional[Exception] = None

        for attempt in range(1, self.retries + 1):
            try:
                response = self.session.request(
                    method,
                    url,
                    params=params,
                    json=json,
                    headers=headers,
                    timeout=self.timeout,
                    allow_redirects=allow_redirects,
                )
                return response
            except Timeout as exc:
                last_error = exc
            except RequestException as exc:
                last_error = exc

            if attempt < self.retries:
                time.sleep(2 ** (attempt - 1))

        raise NetworkError(f"Request to {url} failed: {last_error}")

    def _handle_error(self, response: Response, link_id: Optional[str] = None) -> None:
        try:
            payload = response.json()
        except ValueError:
            payload = {"error": response.text or f"HTTP {response.status_code}"}

        message = payload.get("error") or payload.get("message") or f"HTTP {response.status_code}"
        code = payload.get("code")

        if response.status_code == 404:
            raise LinkIDNotFoundError(link_id or "unknown", message)
        if response.status_code == 410:
            raise LinkIDWithdrawnError(link_id or "unknown", message, payload.get("tombstone"))
        if response.status_code in {400, 422}:
            raise ValidationError(message)
        if response.status_code == 401:
            raise LinkIDError(message, code or "UNAUTHORIZED")
        if response.status_code == 403:
            raise LinkIDError(message, code or "FORBIDDEN")
        if response.status_code == 429:
            raise LinkIDError(message, code or "RATE_LIMITED")

        raise LinkIDError(message, code or "HTTP_ERROR")

    def _cache_ttl(self, cache_control: Optional[str]) -> int:
        if not cache_control:
            return self.cache.default_ttl
        match = re.search(r"max-age=(\d+)", cache_control)
        if match:
            return int(match.group(1))
        return self.cache.default_ttl

    def _cache_key(self, link_id: str, options: Dict[str, Any]) -> str:
        parts = ["linkid", link_id]
        for key in sorted(options):
            value = options[key]
            parts.append(f"{key}={value}")
        return "|".join(parts)

    def _build_headers(self, extra: Optional[Dict[str, str]] = None) -> Dict[str, str]:
        headers = {
            "User-Agent": "LinkID-Python-Sample/1.0",
            "Accept": "application/linkid+json, application/json, */*",
            **self.default_headers,
        }
        if self.api_key:
            headers["Authorization"] = f"ApiKey {self.api_key}"
        if extra:
            headers.update(extra)
        return headers

    def _build_params(
        self,
        format: Optional[str],
        language: Optional[str],
        version: Optional[int],
        timestamp: Optional[str],
        metadata: bool,
    ) -> Dict[str, Any]:
        params: Dict[str, Any] = {}
        if format:
            params["format"] = format
        if language:
            params["lang"] = language
        if version is not None:
            params["version"] = str(version)
        if timestamp:
            params["at"] = timestamp
        if metadata:
            params["metadata"] = "true"
        return params

    def _resolver_used(self, response: Response) -> str:
        return response.headers.get("X-LinkID-Resolver", self.resolver_url)

    def _parse_quality(self, header: Optional[str]) -> Optional[float]:
        if not header:
            return None
        try:
            return float(header)
        except ValueError:
            return None

    def _require_api_key(self) -> None:
        if not self.api_key:
            raise LinkIDError("API key required for this operation", code="AUTH_REQUIRED")

    def _validate_link_id(self, link_id: str) -> None:
        if not isinstance(link_id, str) or not link_id:
            raise ValidationError("LinkID must be a non-empty string")
        if not 32 <= len(link_id) <= 64:
            raise ValidationError("LinkID must be between 32 and 64 characters")
        if not re.match(r"^[A-Za-z0-9._~-]+$", link_id):
            raise ValidationError("LinkID contains invalid characters")

    def _validate_registration_request(self, request: Dict[str, Any]) -> None:
        target = request.get("targetUri")
        if not isinstance(target, str) or not target:
            raise ValidationError("'targetUri' is required and must be a string")
        if not self._looks_like_url(target):
            raise ValidationError("'targetUri' must be an absolute URL")

    @staticmethod
    def _looks_like_url(value: str) -> bool:
        return bool(re.match(r"^https?://", value))


__all__ = [
    "LinkIDClient",
    "LinkIDError",
    "LinkIDNotFoundError",
    "LinkIDWithdrawnError",
    "ValidationError",
    "NetworkError",
    "ResolutionResult",
    "RedirectResolution",
    "MetadataResolution",
]
