def test_health_endpoint(client):
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body.get("status") == "healthy"
    assert "services" in body
    assert body["services"].get("registry") is True
    assert body["services"].get("cache") is True


def test_well_known_endpoint(client):
    r = client.get("/.well-known/linkid-resolver")
    assert r.status_code == 200
    body = r.json()
    assert body["resolver"]["version"] == "1.0"
    eps = body["resolver"]["endpoints"]
    assert "resolve" in eps and "register" in eps


def test_metrics_endpoint(client):
    # hit a request to ensure counters increment
    client.get("/health")
    r = client.get("/metrics")
    assert r.status_code == 200
    text = r.text
    assert "linkid_requests_total" in text
    assert "linkid_request_duration_seconds" in text

