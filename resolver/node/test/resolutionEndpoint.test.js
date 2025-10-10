// Mocks
jest.mock('../src/services/AuthService', () => {
  return jest.fn().mockImplementation(() => ({
    authenticate: (req, _res, next) => { req.user = { sub: 'user-1' }; next(); }
  }));
});

const resolveMock = jest.fn();
jest.mock('../src/services/ResolverService', () => {
  return jest.fn().mockImplementation(() => ({
    resolve: (...args) => resolveMock(...args),
    register: jest.fn(),
    update: jest.fn(),
    withdraw: jest.fn()
  }));
});

jest.mock('../src/services/RegistryService', () => {
  return jest.fn().mockImplementation(() => ({
    get: jest.fn(), create: jest.fn(), update: jest.fn(), delete: jest.fn()
  }));
});

jest.mock('../src/services/CacheService', () => {
  return jest.fn().mockImplementation(() => ({
    get: jest.fn().mockResolvedValue(null), set: jest.fn(), delete: jest.fn(), invalidatePattern: jest.fn()
  }));
});

const request = require('supertest');
const app = require('../src/index');

describe('Resolution endpoint', () => {
  beforeEach(() => {
    resolveMock.mockReset();
  });

  test('redirects by default with headers', async () => {
    const id = 'A'.repeat(32);
    resolveMock.mockResolvedValueOnce({
      type: 'redirect',
      uri: `https://example.org/resource/${id}`,
      quality: 0.9,
      cacheTTL: 3600,
      permanent: false,
    });

    const res = await request(app).get(`/resolve/${id}`);
    expect([301, 302]).toContain(res.status);
    expect(res.headers.location).toBe(`https://example.org/resource/${id}`);
    expect(res.headers['x-linkid-resolver']).toBeDefined();
    expect(res.headers['x-linkid-quality']).toBe('0.9');
    expect(res.headers.link).toContain('rel="canonical"');
  });

  test('metadata variant returns JSON', async () => {
    const id = 'B'.repeat(32);
    resolveMock.mockResolvedValueOnce({
      type: 'metadata',
      data: { id, target: 'https://example.org/x' },
      cacheTTL: 120,
      etag: '"abc"'
    });

    const res = await request(app).get(`/resolve/${id}?metadata=true`);
    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toMatch(/application\/linkid\+json/);
    expect(res.headers.vary).toContain('Accept');
    expect(res.body.id).toBe(id);
    expect(res.body.target).toBe('https://example.org/x');
  });

  test('returns 404 when not found', async () => {
    const id = 'C'.repeat(32);
    const err = new Error('LinkID not found');
    err.code = 'LINKID_NOT_FOUND';
    resolveMock.mockRejectedValueOnce(err);

    const res = await request(app).get(`/resolve/${id}`);
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('LinkID not found');
  });

  test('returns 410 when withdrawn', async () => {
    const id = 'D'.repeat(32);
    const err = new Error('LinkID withdrawn');
    err.code = 'LINKID_WITHDRAWN';
    err.tombstone = { reason: 'Withdrawn by owner' };
    resolveMock.mockRejectedValueOnce(err);

    const res = await request(app).get(`/resolve/${id}`);
    expect(res.status).toBe(410);
    expect(res.body.error).toBe('LinkID withdrawn');
    expect(res.body.tombstone).toBeDefined();
  });

  test('returns 400 on invalid id or params', async () => {
    const badId = 'short';
    const res = await request(app).get(`/resolve/${badId}?format=bad`);
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('Invalid request parameters');
    expect(Array.isArray(res.body.details)).toBe(true);
  });
});


