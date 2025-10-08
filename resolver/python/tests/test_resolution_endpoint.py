import types


def test_resolve_redirect_default(client, app_module, monkeypatch):
    linkid = "A" * 32
    r = client.get(f"/resolve/{linkid}")
    assert r.status_code in (301, 302)
    assert r.headers.get("Location") == f"https://example.org/resource/{linkid}"
    assert "X-LinkID-Resolver" in r.headers
    assert "X-LinkID-Quality" in r.headers


def test_resolve_metadata_variant(client, app_module, monkeypatch):
    # Override resolver_service.resolve to return metadata
    class MetaResult:
        def __init__(self):
            self.type = "metadata"
            self.data = {"id": "A" * 32, "target": "https://example.org/x"}
            self.cache_ttl = 120
            self.etag = "W/\"abc\""

    async def fake_resolve(self, linkid, params):
        return MetaResult()

    # monkeypatch the ResolverService.resolve method
    from services.resolver_service import ResolverService
    monkeypatch.setattr(ResolverService, "resolve", fake_resolve)

    linkid = "B" * 32
    r = client.get(f"/resolve/{linkid}?metadata=true")
    assert r.status_code == 200
    assert r.headers.get("Content-Type") == "application/linkid+json"
    body = r.json()
    assert body["id"] == "B" * 32
    assert body["target"] == "https://example.org/x"


def test_resolve_not_found(client, app_module, monkeypatch):
    class NotFoundError(Exception):
        def __init__(self):
            self.code = "LINKID_NOT_FOUND"

    async def fake_resolve(self, linkid, params):
        raise NotFoundError()

    from services.resolver_service import ResolverService
    monkeypatch.setattr(ResolverService, "resolve", fake_resolve)

    linkid = "C" * 32
    r = client.get(f"/resolve/{linkid}")
    assert r.status_code == 404
    assert r.json()["detail"]["error"] == "LinkID not found"


def test_resolve_withdrawn(client, app_module, monkeypatch):
    class WithdrawnError(Exception):
        def __init__(self):
            self.code = "LINKID_WITHDRAWN"
            self.tombstone = {"reason": "Withdrawn by owner"}

    async def fake_resolve(self, linkid, params):
        raise WithdrawnError()

    from services.resolver_service import ResolverService
    monkeypatch.setattr(ResolverService, "resolve", fake_resolve)

    linkid = "D" * 32
    r = client.get(f"/resolve/{linkid}")
    assert r.status_code == 410
    body = r.json()
    assert body["detail"]["error"] == "LinkID withdrawn"
    assert body["detail"]["tombstone"]["reason"] == "Withdrawn by owner"

