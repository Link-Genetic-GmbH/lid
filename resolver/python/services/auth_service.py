from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional


@dataclass
class User:
    sub: str
    scopes: List[str]


class AuthService:
    """Very simple token-based auth for demo purposes.

    - "demo" token → read+write
    - "readonly" token → read only
    - "unauthorized" or missing token → None
    Any other token is treated as a valid user with read+write scopes.
    """

    async def authenticate(self, token: Optional[str]) -> Optional[User]:
        if not token or token == "unauthorized":
            return None
        if token == "readonly":
            return User(sub="demo-user", scopes=["read"]) 
        # default: allow
        return User(sub="demo-user", scopes=["read", "write"]) 

    def has_scope(self, user: User, scope: str) -> bool:
        return scope in getattr(user, "scopes", [])


