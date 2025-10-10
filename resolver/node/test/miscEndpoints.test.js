// Mock services before requiring app
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

describe('Misc endpoints', () => {
  test('GET /health returns healthy status', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('healthy');
    expect(res.body).toHaveProperty('version');
    expect(res.body).toHaveProperty('timestamp');
    expect(res.body).toHaveProperty('uptime');
  });

  test('GET /.well-known/linkid-resolver returns discovery doc', async () => {
    const res = await request(app).get('/.well-known/linkid-resolver');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('resolver');
    expect(res.body.resolver.version).toBe('1.0');
    const eps = res.body.resolver.endpoints;
    expect(eps).toHaveProperty('resolve');
    expect(eps).toHaveProperty('register');
    expect(eps).toHaveProperty('update');
    expect(eps).toHaveProperty('withdraw');
  });
});


