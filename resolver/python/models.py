from __future__ import annotations

from typing import Any, Dict, Optional

from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings


class LinkIDRecord(BaseModel):
    id: str


class ResolutionRequest(BaseModel):
    format: Optional[str] = None
    language: Optional[str] = None
    version: Optional[int] = None
    timestamp: Optional[str] = None
    accept_header: Optional[str] = None
    accept_language: Optional[str] = None
    prefer_redirect: bool = True

    # Compatibility shim for code paths calling .dict() (Pydantic v1 style)
    def dict(self, *args, **kwargs):  # type: ignore[override]
        try:
            return self.model_dump(*args, **kwargs)  # Pydantic v2
        except Exception:  # pragma: no cover
            return super().dict(*args, **kwargs)


class RegistrationRequest(BaseModel):
    # Accept both target_uri and targetUri
    target_uri: str = Field(alias="targetUri")
    media_type: Optional[str] = Field(default=None, alias="mediaType")
    language: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None

    class Config:
        populate_by_name = True


