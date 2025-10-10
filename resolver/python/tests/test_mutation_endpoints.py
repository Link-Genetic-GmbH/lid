from typing import Dict, Any


def _auth_headers(token: str = "valid-token") -> Dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_register_success(client):
    payload = {
        "target_uri": "https://example.org/resource",
        "media_type": "text/html",
        "language": "en",
        "metadata": {"k": "v"},
    }
    r = client.post("/register", json=payload, headers=_auth_headers())
    assert r.status_code == 201
    data = r.json()
    assert "id" in data and len(data["id"]) >= 32
    assert data["uri"].startswith("https://w3id.org/linkid/")


def test_register_requires_auth(client):
    r = client.post("/register", json={"target_uri": "https://x"})
    assert r.status_code == 401


def test_register_requires_write_scope(client, monkeypatch):
    # Patch AuthService.has_scope to deny write
    from services.auth_service import AuthService

    def deny_write(self, user, scope: str) -> bool:
        if scope == "write":
            return False
        return True

    monkeypatch.setattr(AuthService, "has_scope", deny_write)

    r = client.post("/register", json={"target_uri": "https://x"}, headers=_auth_headers())
    assert r.status_code == 403


def test_update_success(client):
    linkid = "E" * 32
    updates: Dict[str, Any] = {"target_uri": "https://example.org/new"}
    r = client.put(f"/resolve/{linkid}", json=updates, headers=_auth_headers())
    assert r.status_code == 200
    body = r.json()
    assert body["id"] == linkid
    assert "updated" in body


def test_withdraw_success(client):
    linkid = "F" * 32
    r = client.delete(
        f"/resolve/{linkid}",
        json={"reason": "owner request"},
        headers=_auth_headers(),
    )
    assert r.status_code == 200
    body = r.json()
    assert body["id"] == linkid
    assert body.get("reason") == "owner request"


def test_update_requires_auth(client):
    linkid = "G" * 32
    r = client.put(f"/resolve/{linkid}", json={"target_uri": "https://x"})
    assert r.status_code == 401


def test_withdraw_requires_auth(client):
    linkid = "H" * 32
    r = client.delete(f"/resolve/{linkid}")
    assert r.status_code == 401

