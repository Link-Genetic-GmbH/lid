# LinkID Resolver - Java Implementation

A high-performance, enterprise-grade Java implementation of the LinkID resolution specification using Spring Boot 3.2.

## Features

- **Complete LinkID Resolution**: Implements the full LinkID specification with redirect and metadata responses
- **High Performance**: Multi-tier caching with Redis and Spring Cache
- **Enterprise Ready**: MongoDB persistence, JWT authentication, rate limiting
- **Production Ready**: Health checks, metrics, monitoring, configurable security
- **Standards Compliant**: Follows LinkID spec for resolution algorithm and API endpoints

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- MongoDB 5.0+
- Redis 6.0+

### Build

```bash
mvn clean package
```

### Run

```bash
# Development mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production mode
java -jar target/linkid-resolver-1.0.0.jar --spring.profiles.active=prod
```

### Docker

```bash
docker build -t linkid-resolver .
docker run -p 8080:8080 linkid-resolver
```

## API Endpoints

### Resolution
- `GET /resolve/{id}` - Resolve LinkID to current resource
- `GET /resolve/{id}?format=metadata` - Get metadata instead of redirect
- `GET /resolve/{id}?lang=en&format=pdf` - Content negotiation

### Management
- `POST /register` - Register new LinkID (requires auth)
- `PUT /resolve/{id}` - Update LinkID records (requires auth)
- `DELETE /resolve/{id}` - Withdraw LinkID (requires auth)

### Monitoring
- `GET /health` - Basic health check
- `GET /health/detailed` - Detailed health with components
- `GET /stats` - Registry statistics
- `GET /.well-known/linkid-resolver` - Service discovery

## Configuration

### Environment Variables

```bash
# Database
MONGODB_URI=mongodb://localhost:27017/linkid
REDIS_URL=redis://localhost:6379

# Security
JWT_SECRET=your-secret-key
ADMIN_PASSWORD=secure-password

# Service
LINKID_BASE_URL=https://w3id.org/linkid
PORT=8080
```

### Application Profiles

- `dev` - Development with debug logging
- `prod` - Production optimized settings
- `test` - Testing configuration

## Architecture

### Package Structure
```
org.linkgenetic.resolver/
├── controller/     # REST API endpoints
├── service/        # Business logic
├── model/          # Data models and DTOs
├── repository/     # Data access layer
├── config/         # Spring configuration
├── exception/      # Exception handling
└── util/           # Utilities and validators
```

### Core Components

- **ResolverService**: Core resolution algorithm implementation
- **RegistryService**: LinkID record management
- **CacheService**: Multi-tier caching strategy
- **ValidationService**: LinkID format validation
- **AuthenticationService**: JWT-based security

### Resolution Algorithm

1. Validate LinkID format (32-64 alphanumeric characters)
2. Check L1 cache (Redis)
3. Query MongoDB registry
4. Filter candidates by request parameters
5. Rank by quality score and freshness
6. Return redirect (302) or metadata JSON
7. Cache result with configurable TTL

## Testing

```bash
# Unit tests
mvn test

# Integration tests with TestContainers
mvn test -P integration

# Coverage report
mvn jacoco:report
```

## Performance

### Target Metrics
- Resolution latency: <100ms p95
- Throughput: 10,000 requests/second
- Cache hit rate: >90%
- Availability: 99.9% SLA

### Scaling
- Stateless design for horizontal scaling
- Redis cluster support
- MongoDB sharding ready
- CDN integration for global caching

## Security

- JWT authentication for write operations
- Rate limiting (configurable per client)
- CORS support
- Security headers
- Input validation and sanitization

## Monitoring

### Metrics (Prometheus)
- Resolution success rate
- Response time distribution
- Cache hit/miss rates
- Error rates by type
- Resource utilization

### Health Checks
- Database connectivity
- Cache availability
- External service dependencies

## Development

### Code Style
- Java 17 features
- Spring Boot best practices
- Clean architecture principles
- Comprehensive error handling

### Contributing
1. Fork the repository
2. Create feature branch
3. Write tests
4. Submit pull request

## License

Licensed under the same terms as the main LinkID project.