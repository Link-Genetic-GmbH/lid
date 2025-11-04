from __future__ import annotations

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    environment: str = "development"
    allowed_origins: list[str] = ["*"]
    rate_limit_per_minute: int = 600
    rate_limit_per_hour: int = 3600
    host: str = "127.0.0.1"
    port: int = 8080
    log_level: str = "INFO"
    seed_file: str | None = None

    class Config:
        env_prefix = "LINKID_"
        case_sensitive = False


settings = Settings()


