#!/usr/bin/env python3
"""Minimal example using the LinkID Python client against the local resolver.

Usage:
  python examples/resolve.py
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

# Ensure SDK is importable
ROOT = Path(__file__).resolve().parents[1]
SDK_PY = ROOT / "sdk" / "python"
sys.path.insert(0, str(SDK_PY))

from client import LinkIDClient  # type: ignore


def main() -> None:
    base_url = os.environ.get("BASE_URL", "http://localhost:8080")
    api_key = os.environ.get("API_KEY", "demo")

    client = LinkIDClient(resolver_url=base_url, api_key=api_key)

    print("Registering...")
    reg = client.register({
        "targetUri": "https://example.org/resource",
        "mediaType": "text/html",
        "language": "en",
    })
    link_id = reg["id"]
    print("Registered:", reg)

    print("Resolve (metadata)...")
    meta = client.resolve(link_id, metadata=True)
    print(meta)


if __name__ == "__main__":
    main()


