const ResolverService = require('../../src/services/ResolverService');

// Lightweight stubs for dependencies
class StubRegistry {
  async get() { return null; }
  async create() {}
  async update() {}
}

class StubCache {
  async get() { return null; }
  async set() {}
  async invalidatePattern() {}
}

const logger = { info: jest.fn(), debug: jest.fn(), error: jest.fn() };

describe('ResolverService unit', () => {
  let svc;
  beforeEach(() => {
    svc = new ResolverService(new StubRegistry(), new StubCache(), logger);
  });

  test('isValidLinkID validates length and charset', () => {
    expect(svc.isValidLinkID('a'.repeat(32))).toBe(true);
    expect(svc.isValidLinkID('A._~0'.repeat(8))).toBe(true);
    expect(svc.isValidLinkID('short')).toBe(false);
    expect(svc.isValidLinkID('x'.repeat(65))).toBe(false);
    expect(svc.isValidLinkID('invalid!chars')).toBe(false);
  });

  test('generateCacheKey is deterministic and prefixed', () => {
    const id = 'a'.repeat(32);
    const key1 = svc.generateCacheKey(id, { format: 'pdf', language: 'en', preferRedirect: true });
    const key2 = svc.generateCacheKey(id, { format: 'pdf', language: 'en', preferRedirect: true });
    const key3 = svc.generateCacheKey(id, { format: 'html', language: 'en', preferRedirect: true });
    expect(key1).toEqual(key2);
    expect(key1).toMatch(/^linkid:a{32}:/);
    expect(key1).not.toEqual(key3);
  });

  test('filterCandidates respects status and validity period', () => {
    const now = new Date();
    const recs = [
      { status: 'active', validFrom: new Date(now.getTime() - 1000).toISOString(), uri: 'x', mediaType: 'text/html', language: 'en' },
      { status: 'inactive', uri: 'x' },
      { status: 'active', validFrom: new Date(now.getTime() + 60000).toISOString(), uri: 'x' },
      { status: 'active', validUntil: new Date(now.getTime() - 60000).toISOString(), uri: 'x' }
    ];
    const out = svc.filterCandidates(recs, {});
    expect(out).toHaveLength(1);
  });

  test('filterCandidates uses format->mediaType and language', () => {
    const recs = [
      { status: 'active', uri: 'x', mediaType: 'application/pdf', language: 'en' },
      { status: 'active', uri: 'y', mediaType: 'text/html', language: 'fr' },
    ];
    const out1 = svc.filterCandidates(recs, { format: 'pdf' });
    expect(out1.map(r => r.uri)).toEqual(['x']);
    const out2 = svc.filterCandidates(recs, { language: 'fr' });
    expect(out2.map(r => r.uri)).toEqual(['y']);
  });

  test('rankCandidates sorts by quality then freshness then language', () => {
    const base = new Date('2024-01-01T00:00:00Z');
    const candidates = [
      { uri: 'a', quality: 0.8, lastModified: new Date(base.getTime() + 1000).toISOString(), language: 'en' },
      { uri: 'b', quality: 0.9, lastModified: new Date(base.getTime() - 1000).toISOString(), language: 'en' },
      { uri: 'c', quality: 0.9, lastModified: new Date(base.getTime() + 2000).toISOString(), language: 'fr' },
    ];
    const ranked = svc.rankCandidates(candidates, { language: 'en' });
    expect(ranked[0].uri).toBe('c'); // highest quality and freshest wins
  });

  test('createRedirectResponse and createMetadataResponse set fields', () => {
    const record = { policy: { cacheTTL: 123 } };
    const resp = svc.createRedirectResponse({ uri: 'https://x', quality: 1 }, record);
    expect(resp.type).toBe('redirect');
    expect(resp.cacheTTL).toBe(123);

    const meta = svc.createMetadataResponse({ updated: new Date().toISOString(), policy: { cacheTTL: 77 } }, []);
    expect(meta.type).toBe('metadata');
    expect(meta.etag).toMatch(/^"[a-f0-9]{16}"$/);
    expect(meta.cacheTTL).toBe(77);
  });
});


