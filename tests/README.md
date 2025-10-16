# LinkID Test Suite

Comprehensive test suite for LinkID resolver implementations and client SDKs. This test suite validates conformance to the LinkID specification and ensures interoperability between different implementations.

## Test Categories

### 1. Specification Conformance Tests
- **URI Scheme Validation**: Tests for valid LinkID formats and URI scheme compliance
- **Resolution Algorithm**: Tests for the normative resolution algorithm
- **HTTP API Compliance**: Tests for required HTTP status codes and headers
- **Metadata Format**: Tests for JSON schema validation

### 2. Functional Tests
- **Basic Resolution**: Test successful resolution of active LinkIDs
- **Content Negotiation**: Test format and language preferences
- **Error Handling**: Test 404, 410, and other error conditions
- **Caching Behavior**: Test cache headers and client-side caching
- **Authentication**: Test API key and JWT authentication

### 3. Performance Tests
- **Resolution Latency**: Measure p95 resolution times
- **Throughput**: Test concurrent resolution capacity
- **Cache Performance**: Measure cache hit rates and response times
- **Memory Usage**: Monitor memory consumption under load

### 4. Integration Tests
- **Resolver Federation**: Test multiple resolver discovery and failover
- **DOI Integration**: Test cross-referencing with DOI system
- **Archive Integration**: Test integration with web archives
- **CMS Integration**: Test WordPress and other CMS plugins

### 5. Security Tests
- **Input Validation**: Test malformed requests and injection attacks
- **Rate Limiting**: Test rate limiting enforcement
- **Authentication**: Test various authentication methods
- **HTTPS Enforcement**: Test SSL/TLS requirements

## Test Data

The test suite uses a standardized set of test LinkIDs:

```
# Active LinkIDs
test001: b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14  # PDF document
test002: c3a7e1d8e8d45f4f9b5f1b7c3a0d0e25  # HTML page
test003: d4b8f2e9f9e56060ac601c8d4b1e1f36  # JSON data
test004: e5c9030a0af67171bd712d9e5c2f2047  # Multi-format resource

# Withdrawn LinkIDs
test101: f6da141b1b078282ce823e0f6d303158  # Withdrawn with tombstone
test102: 07eb252c2c189393df934f107e414269  # Withdrawn without tombstone

# Special Test Cases
test201: 18fc363d3d29a4a4e0a450218f52537a  # Very long cache TTL
test202: 29fd474e4e3ab5b5f1b561329063648b  # Multiple language versions
test203: 3a0e585f5f4bc6c602c67243a174759c  # Content with checksums
```

## Running Tests

### Prerequisites
- Node.js 18+ (for JavaScript tests)
- Python 3.8+ (for Python tests)
- Java 17+ (for Java tests)
- Docker (for integration tests)

### Running All Tests
```bash
# Install dependencies
npm install
pip install -r requirements.txt
mvn install

# Run full test suite
npm run test:all
```

### Running Specific Test Categories
```bash
# Specification conformance
npm run test:conformance

# Performance tests
npm run test:performance

# Integration tests
docker-compose up -d
npm run test:integration

# Security tests
npm run test:security
```

## Test Environment

The test suite includes a complete test environment with:

- **Mock Resolver**: Lightweight resolver implementation for testing
- **Test Registry**: In-memory registry with predefined test data
- **Mock Services**: Simulated DOI, ARK, and archive services
- **Load Generator**: Tool for performance and stress testing

### Test Environment Setup
```bash
# Start test environment
docker-compose -f tests/docker-compose.test.yml up -d

# Run environment health check
npm run test:health

# Stop test environment
docker-compose -f tests/docker-compose.test.yml down
```

## Test Reports

Test results are generated in multiple formats:

- **JUnit XML**: For CI/CD integration
- **HTML Reports**: Human-readable test results
- **JSON Results**: Machine-readable test data
- **Coverage Reports**: Code coverage analysis

Reports are saved to the `tests/reports/` directory.

## Continuous Integration

The test suite integrates with popular CI/CD platforms:

- **GitHub Actions**: `.github/workflows/test.yml`
- **GitLab CI**: `.gitlab-ci.yml`
- **Jenkins**: `Jenkinsfile`
- **CircleCI**: `.circleci/config.yml`

### Test Matrix

Tests run against multiple configurations:

| Platform | Resolver | Client | Database | Cache |
|----------|----------|--------|----------|-------|
| Linux    | Node.js  | JS/TS  | MongoDB  | Redis |
| Linux    | Python   | Python | MongoDB  | Redis |
| Linux    | Java     | Java   | MongoDB  | Redis |
| Windows  | Node.js  | JS/TS  | MongoDB  | Memory|
| macOS    | Python   | Python | MongoDB  | Memory|

## Contributing Tests

When adding new tests:

1. Follow the naming convention: `test-category-description.spec.js`
2. Include test documentation in the test file header
3. Use the standard test data set when possible
4. Add performance benchmarks for new features
5. Update this README with new test categories

### Test Structure
```javascript
describe('LinkID Resolution', () => {
  describe('Basic Resolution', () => {
    it('should resolve active LinkID to target URI', async () => {
      // Test implementation
    });

    it('should return 404 for non-existent LinkID', async () => {
      // Test implementation
    });
  });
});
```

## Compliance Validation

The test suite validates compliance with:

- **RFC 3986**: URI Generic Syntax
- **RFC 7595**: URI Scheme Registration
- **W3C LinkID Specification**: Core specification
- **IETF Internet-Draft**: linkid: URI scheme

### Compliance Report
Run `npm run test:compliance` to generate a detailed compliance report showing which requirements are tested and validated.