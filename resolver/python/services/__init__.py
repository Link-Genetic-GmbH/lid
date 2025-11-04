"""Service package for the Python LinkID demo resolver.

Provides minimal in-memory implementations for:
- registry (records storage, versioning, tombstones)
- cache (simple ephemeral cache hooks)
- resolver (resolution logic using registry + cache)
- auth (very simple bearer token handling)
"""

__all__ = [
    "auth_service",
    "cache_service",
    "registry_service",
    "resolver_service",
]


