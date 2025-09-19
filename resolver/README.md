# LinkID Resolver Architecture

## Overview

The LinkID resolver architecture is designed to provide scalable, reliable, and secure resolution of persistent identifiers to current resource locations. The architecture supports federated deployments, caching, and intelligent fallback mechanisms.

## Core Components

### 1. Resolver Service
HTTP service that implements the LinkID resolution API:
- **Endpoint**: `/resolve/{id}[?parameters]`
- **Methods**: GET (resolution), POST (registration), PUT (update), DELETE (withdrawal)
- **Authentication**: JWT tokens for write operations
- **Rate Limiting**: Configurable per-client and global limits

### 2. Registry Database
Persistent storage for identifier mappings:
- **Primary Key**: LinkID identifier
- **Record Structure**: Resolution metadata with versioning
- **Indexing**: Hash indexes on ID, content hash, and URI
- **Replication**: Master-slave with eventual consistency

### 3. Cache Layer
Multi-tier caching for performance:
- **L1 Cache**: In-memory (Redis) for hot identifiers
- **L2 Cache**: CDN edge caching for global distribution
- **L3 Cache**: Client-side caching with TTL
- **Invalidation**: Event-driven cache purging

### 4. Discovery Service
Resolver endpoint discovery mechanism:
- **Well-known URI**: `/.well-known/linkid-resolver`
- **DNS SRV records**: `_linkid._https.{domain}`
- **Bootstrap registry**: Fallback resolver list

## API Specification

### Resolution Endpoint

```http
GET /resolve/{id}?format={format}&lang={lang}&version={version}
Host: resolver.linkid.org
Accept: application/linkid+json, application/json, */*
Authorization: Bearer {optional-jwt-token}
```

**Response Types:**

1. **Redirect Response** (default):
```http
HTTP/1.1 302 Found
Location: https://content.example.org/document.pdf
Cache-Control: max-age=3600
Link: <https://w3id.org/linkid/{id}>; rel="canonical"
X-LinkID-Resolver: resolver.linkid.org
X-LinkID-Quality: 0.95
```

2. **Metadata Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/linkid+json
Cache-Control: max-age=1800
ETag: "abc123def456"

{
  "id": "b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14",
  "status": "active",
  "created": "2025-01-15T09:30:00Z",
  "updated": "2025-07-10T14:22:30Z",
  "issuer": "https://registry.example.org",
  "records": [
    {
      "uri": "https://content.example.org/v3/document.pdf",
      "status": "active",
      "mediaType": "application/pdf",
      "language": "en",
      "quality": 0.95,
      "validFrom": "2025-07-10T00:00:00Z",
      "validUntil": null,
      "checksum": {
        "algorithm": "sha256",
        "value": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
      },
      "size": 2047583,
      "lastModified": "2025-07-09T16:45:00Z",
      "metadata": {
        "title": "LinkID Technical Specification",
        "version": "3.0",
        "author": "Link Genetic GmbH"
      }
    }
  ],
  "alternates": [
    {
      "scheme": "doi",
      "identifier": "10.1000/182"
    }
  ],
  "policy": {
    "preferredFormat": "pdf",
    "cacheTTL": 3600,
    "fallbackResolvers": [
      "https://backup.resolver.org/"
    ]
  }
}
```

### Management Endpoints

#### Register New Identifier
```http
POST /register
Content-Type: application/linkid+json
Authorization: Bearer {jwt-token}

{
  "targetUri": "https://example.org/document.pdf",
  "mediaType": "application/pdf",
  "metadata": {
    "title": "Example Document",
    "author": "John Doe"
  }
}
```

#### Update Existing Identifier
```http
PUT /resolve/{id}
Content-Type: application/linkid+json
Authorization: Bearer {jwt-token}

{
  "records": [
    {
      "uri": "https://newlocation.example.org/document.pdf",
      "status": "active",
      "mediaType": "application/pdf"
    }
  ]
}
```

#### Withdraw Identifier
```http
DELETE /resolve/{id}
Authorization: Bearer {jwt-token}

{
  "reason": "Content no longer available",
  "tombstone": {
    "withdrawnAt": "2025-09-18T12:00:00Z",
    "contact": "admin@example.org"
  }
}
```

## Resolution Algorithm

The resolver implements the following algorithm for identifier resolution:

```python
def resolve_linkid(id, request_params, client_prefs):
    # 1. Validate identifier format
    if not is_valid_linkid(id):
        return error(400, "Invalid LinkID format")

    # 2. Check cache
    cached_result = cache.get(id, request_params)
    if cached_result and not cached_result.expired():
        return cached_result

    # 3. Query registry
    record = registry.get(id)
    if not record:
        return error(404, "LinkID not found")

    if record.status == "withdrawn":
        return error(410, "LinkID withdrawn", record.tombstone)

    # 4. Filter candidates by request parameters
    candidates = filter_records(record.records, request_params)
    if not candidates:
        return error(404, "No matching records")

    # 5. Rank candidates by quality, freshness, preference
    ranked = rank_candidates(candidates, client_prefs)

    # 6. Return response
    if client_prefs.prefer_redirect:
        response = redirect(302, ranked[0].uri)
        response.headers['X-LinkID-Quality'] = ranked[0].quality
    else:
        response = json_response(200, record)

    # 7. Cache result
    cache.set(id, request_params, response, ttl=record.policy.cacheTTL)

    return response
```

## Security Architecture

### Authentication & Authorization
- **API Keys**: For programmatic access
- **JWT Tokens**: For user authentication with scopes
- **mTLS**: For resolver-to-resolver communication
- **Rate Limiting**: Per-client quotas and global throttling

### Data Integrity
- **Digital Signatures**: Ed25519 signatures on resolution records
- **Content Verification**: SHA-256 checksums for target resources
- **Audit Logging**: Immutable logs for all operations
- **Version Control**: Complete history of identifier changes

### Privacy Protection
- **Data Minimization**: Only store necessary metadata
- **Access Controls**: Private identifiers with ACLs
- **Anonymization**: No personal data in identifiers
- **GDPR Compliance**: Data portability and deletion rights

## Deployment Architecture

### Single Resolver Deployment
```
Internet → Load Balancer → Resolver Service → Cache → Database
                       ↘ CDN Edge Cache
```

### Federated Deployment
```
Client → Discovery Service → Primary Resolver
                         → Institutional Resolver
                         → Archive Resolver
                         → Mirror Resolver
```

### High Availability Setup
```
Region A: LB → Resolver → DB Master
Region B: LB → Resolver → DB Replica
Global CDN: Edge Caches
```

## Performance Characteristics

### Target Metrics
- **Resolution Latency**: < 100ms p95 globally
- **Availability**: 99.9% uptime SLA
- **Throughput**: 10,000 resolutions/second per instance
- **Cache Hit Rate**: > 90% for popular identifiers

### Scaling Strategy
- **Horizontal Scaling**: Stateless resolver instances
- **Database Sharding**: Partition by identifier hash
- **CDN Integration**: Global edge caching
- **Read Replicas**: Geographic distribution

## Error Handling

### HTTP Status Codes
- `200 OK`: Successful metadata response
- `302 Found`: Successful redirect
- `400 Bad Request`: Invalid identifier format
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Access denied
- `404 Not Found`: Identifier not found
- `410 Gone`: Identifier withdrawn
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Resolver failure
- `503 Service Unavailable`: Temporary unavailability

### Fallback Mechanisms
1. **Secondary Resolvers**: Automatic failover to backup resolvers
2. **Archived Content**: Redirect to web archive snapshots
3. **Alternative Identifiers**: Cross-reference DOI/ARK/Handle
4. **Semantic Search**: AI-assisted content discovery

## Monitoring & Observability

### Key Metrics
- **Resolution Success Rate**: Percentage of successful resolutions
- **Response Time Distribution**: P50, P95, P99 latencies
- **Cache Performance**: Hit rates, eviction rates
- **Error Rates**: By status code and error type
- **Resource Utilization**: CPU, memory, storage, network

### Logging
- **Access Logs**: All resolution requests with timing
- **Error Logs**: Failed resolutions with diagnostics
- **Audit Logs**: Administrative operations
- **Security Logs**: Authentication failures, suspicious activity

### Alerting
- **SLA Violations**: Latency or availability breaches
- **Error Rate Spikes**: Sudden increase in failures
- **Resource Exhaustion**: High CPU/memory/disk usage
- **Security Events**: Potential attacks or breaches