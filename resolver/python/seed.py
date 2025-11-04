from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List

from services.registry_service import RegistryService


async def load_seed(registry: RegistryService, seed_path: str) -> List[str]:
    """Load seed records from a JSON file into the registry.

    Returns list of created link IDs.
    """
    path = Path(seed_path)
    if not path.exists():
        return []

    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    created: List[str] = []
    for entry in data:
        payload: Dict[str, Any] = {
            "target_uri": entry.get("target_uri") or entry.get("targetUri"),
            "media_type": entry.get("media_type") or entry.get("mediaType"),
            "language": entry.get("language"),
            "metadata": entry.get("metadata") or {},
        }
        record = await registry.create(payload, issuer=entry.get("issuer") or "seed")
        created.append(record.id)

        if entry.get("withdrawn"):
            await registry.withdraw(record.id, {"reason": entry.get("withdrawal_reason")}, user_sub=record.metadata.get("issuer", "seed"))

    return created


