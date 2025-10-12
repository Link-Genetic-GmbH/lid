// Mocks
let authenticateImpl;
jest.mock('../src/services/AuthService', () => {
  return jest.fn().mockImplementation(() => ({
    authenticate: (req, res, next) => authenticateImpl(req, res, next)
  }));
});

const resolverMock = {
  resolve: jest.fn(),
  register: jest.fn(),
  update: jest.fn(),
  withdraw: jest.fn()
};
jest.mock('../src/services/ResolverService', () => {
  return jest.fn().mockImplementation(() => resolverMock);
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

describe('Mutation endpoints', () => {
  beforeEach(() => {
    resolverMock.register.mockReset();
    resolverMock.update.mockReset();
    resolverMock.withdraw.mockReset();
    // default auth: inject user
    authenticateImpl = (req, _res, next) => { req.user = { sub: 'user-1' }; next(); };
  });

  test('POST /register success', async () => {
    const created = { id: 'a'.repeat(32), created: new Date().toISOString() };
    resolverMock.register.mockResolvedValueOnce(created);

    const res = await request(app)
      .post('/register')
      .set('Authorization', 'Bearer token')
      .send({ targetUri: 'https://example.org/resource', mediaType: 'text/html', metadata: { k: 'v' } });

    expect(res.status).toBe(201);
    expect(res.body.id).toBe(created.id);
    expect(res.body.uri).toBe(`https://w3id.org/linkid/${created.id}`);
  });

  test('POST /register requires auth', async () => {
    authenticateImpl = (_req, res, _next) => res.status(401).json({ error: 'Authorization header required' });
    const res = await request(app).post('/register').send({ targetUri: 'https://x' });
    expect(res.status).toBe(401);
  });

  test('PUT /resolve/:id success', async () => {
    const id = 'b'.repeat(32);
    resolverMock.update.mockResolvedValueOnce();
    const res = await request(app)
      .put(`/resolve/${id}`)
      .set('Authorization', 'Bearer token')
      .send({ records: [] });
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(id);
    expect(res.body.updated).toBeDefined();
  });

  test('DELETE /resolve/:id success', async () => {
    const id = 'c'.repeat(32);
    resolverMock.withdraw.mockResolvedValueOnce();
    const res = await request(app)
      .delete(`/resolve/${id}`)
      .set('Authorization', 'Bearer token')
      .send({ reason: 'owner request' });
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(id);
    expect(res.body.reason).toBe('owner request');
  });

  test('PUT /resolve/:id requires auth', async () => {
    authenticateImpl = (_req, res, _next) => res.status(401).json({ error: 'Authorization header required' });
    const id = 'd'.repeat(32);
    const res = await request(app).put(`/resolve/${id}`).send({ targetUri: 'https://x' });
    expect(res.status).toBe(401);
  });

  test('DELETE /resolve/:id requires auth', async () => {
    authenticateImpl = (_req, res, _next) => res.status(401).json({ error: 'Authorization header required' });
    const id = 'e'.repeat(32);
    const res = await request(app).delete(`/resolve/${id}`);
    expect(res.status).toBe(401);
  });

  test('PUT /resolve/:id unauthorized yields 403', async () => {
    const id = 'f'.repeat(32);
    const err = new Error('Not authorized');
    err.code = 'UNAUTHORIZED';
    resolverMock.update.mockRejectedValueOnce(err);
    const res = await request(app)
      .put(`/resolve/${id}`)
      .set('Authorization', 'Bearer token')
      .send({ targetUri: 'https://x' });
    expect(res.status).toBe(403);
  });

  test('DELETE /resolve/:id unauthorized yields 403', async () => {
    const id = 'g'.repeat(32);
    const err = new Error('Not authorized');
    err.code = 'UNAUTHORIZED';
    resolverMock.withdraw.mockRejectedValueOnce(err);
    const res = await request(app)
      .delete(`/resolve/${id}`)
      .set('Authorization', 'Bearer token')
      .send({ reason: 'x' });
    expect(res.status).toBe(403);
  });
});


