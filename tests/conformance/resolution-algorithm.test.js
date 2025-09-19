/**
 * LinkID Resolution Algorithm Conformance Tests
 *
 * Tests the normative resolution algorithm specified in the W3C LinkID specification.
 * These tests validate that resolvers implement the correct behavior for:
 * - Identifier validation
 * - Cache checking
 * - Registry queries
 * - Candidate filtering and ranking
 * - Response generation
 * - Error handling
 */

const { expect } = require('chai');
const { LinkIDClient } = require('../../sdk/js/dist');
const { MockResolver } = require('../mocks/resolver');
const { TestData } = require('../data/test-data');

describe('LinkID Resolution Algorithm Conformance', () => {
  let client;
  let mockResolver;

  beforeEach(() => {
    mockResolver = new MockResolver();
    client = new LinkIDClient({
      resolverUrl: mockResolver.url,
      caching: false // Disable caching for algorithm tests
    });
  });

  afterEach(async () => {
    await mockResolver.stop();
  });

  describe('Step 1: Identifier Validation', () => {
    it('should reject empty identifier', async () => {
      try {
        await client.resolve('');
        expect.fail('Should have thrown validation error');
      } catch (error) {
        expect(error.code).to.equal('VALIDATION_ERROR');
        expect(error.message).to.include('non-empty');
      }
    });

    it('should reject identifier shorter than 32 characters', async () => {
      try {
        await client.resolve('short');
        expect.fail('Should have thrown validation error');
      } catch (error) {
        expect(error.code).to.equal('VALIDATION_ERROR');
        expect(error.message).to.include('32-64 characters');
      }
    });

    it('should reject identifier longer than 64 characters', async () => {
      const longId = 'a'.repeat(65);
      try {
        await client.resolve(longId);
        expect.fail('Should have thrown validation error');
      } catch (error) {
        expect(error.code).to.equal('VALIDATION_ERROR');
        expect(error.message).to.include('32-64 characters');
      }
    });

    it('should reject identifier with invalid characters', async () => {
      try {
        await client.resolve('invalid@#$%identifier123456789012');
        expect.fail('Should have thrown validation error');
      } catch (error) {
        expect(error.code).to.equal('VALIDATION_ERROR');
        expect(error.message).to.include('invalid characters');
      }
    });

    it('should accept valid identifier formats', async () => {
      const validIds = [
        'b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14', // UUID without hyphens
        'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', // SHA-256
        'test.identifier_with-valid~chars123'  // Mixed valid characters
      ];

      for (const id of validIds) {
        mockResolver.addRecord(id, TestData.activeRecord);
        const result = await client.resolve(id);
        expect(result.linkId).to.equal(id);
      }
    });
  });

  describe('Step 2: Registry Query', () => {
    it('should return 404 for non-existent identifier', async () => {
      try {
        await client.resolve(TestData.nonExistentId);
        expect.fail('Should have thrown not found error');
      } catch (error) {
        expect(error.code).to.equal('LINKID_NOT_FOUND');
        expect(error.linkId).to.equal(TestData.nonExistentId);
      }
    });

    it('should return 410 for withdrawn identifier', async () => {
      mockResolver.addRecord(TestData.withdrawnId, TestData.withdrawnRecord);

      try {
        await client.resolve(TestData.withdrawnId);
        expect.fail('Should have thrown withdrawn error');
      } catch (error) {
        expect(error.code).to.equal('LINKID_WITHDRAWN');
        expect(error.linkId).to.equal(TestData.withdrawnId);
        expect(error.tombstone).to.exist;
      }
    });

    it('should retrieve active record from registry', async () => {
      mockResolver.addRecord(TestData.activeId, TestData.activeRecord);

      const result = await client.resolve(TestData.activeId, { metadata: true });
      expect(result.type).to.equal('metadata');
      expect(result.data.id).to.equal(TestData.activeId);
      expect(result.data.status).to.equal('active');
    });
  });

  describe('Step 3: Candidate Filtering', () => {
    it('should filter by media type', async () => {
      const record = {
        ...TestData.multiFormatRecord,
        records: [
          { uri: 'https://example.org/doc.pdf', mediaType: 'application/pdf', status: 'active' },
          { uri: 'https://example.org/doc.html', mediaType: 'text/html', status: 'active' },
          { uri: 'https://example.org/doc.json', mediaType: 'application/json', status: 'active' }
        ]
      };

      mockResolver.addRecord(TestData.multiFormatId, record);

      const result = await client.resolve(TestData.multiFormatId, { format: 'pdf' });
      expect(result.uri).to.include('.pdf');
    });

    it('should filter by language', async () => {
      const record = {
        ...TestData.multiLanguageRecord,
        records: [
          { uri: 'https://example.org/doc-en.html', language: 'en', status: 'active' },
          { uri: 'https://example.org/doc-de.html', language: 'de', status: 'active' },
          { uri: 'https://example.org/doc-fr.html', language: 'fr', status: 'active' }
        ]
      };

      mockResolver.addRecord(TestData.multiLanguageId, record);

      const result = await client.resolve(TestData.multiLanguageId, { language: 'de' });
      expect(result.uri).to.include('-de.html');
    });

    it('should filter by validity period', async () => {
      const now = new Date();
      const future = new Date(now.getTime() + 86400000); // +1 day
      const past = new Date(now.getTime() - 86400000); // -1 day

      const record = {
        ...TestData.activeRecord,
        records: [
          {
            uri: 'https://example.org/future.html',
            status: 'active',
            validFrom: future.toISOString()
          },
          {
            uri: 'https://example.org/current.html',
            status: 'active',
            validFrom: past.toISOString(),
            validUntil: future.toISOString()
          }
        ]
      };

      mockResolver.addRecord(TestData.activeId, record);

      const result = await client.resolve(TestData.activeId);
      expect(result.uri).to.include('current.html');
    });

    it('should exclude inactive records', async () => {
      const record = {
        ...TestData.activeRecord,
        records: [
          { uri: 'https://example.org/inactive.html', status: 'inactive' },
          { uri: 'https://example.org/active.html', status: 'active' }
        ]
      };

      mockResolver.addRecord(TestData.activeId, record);

      const result = await client.resolve(TestData.activeId);
      expect(result.uri).to.include('active.html');
    });
  });

  describe('Step 4: Candidate Ranking', () => {
    it('should rank by quality score', async () => {
      const record = {
        ...TestData.activeRecord,
        records: [
          { uri: 'https://example.org/low.html', quality: 0.3, status: 'active' },
          { uri: 'https://example.org/high.html', quality: 0.9, status: 'active' },
          { uri: 'https://example.org/medium.html', quality: 0.6, status: 'active' }
        ]
      };

      mockResolver.addRecord(TestData.activeId, record);

      const result = await client.resolve(TestData.activeId);
      expect(result.uri).to.include('high.html');
      expect(result.quality).to.equal(0.9);
    });

    it('should rank by freshness when quality is equal', async () => {
      const older = new Date('2023-01-01').toISOString();
      const newer = new Date('2023-12-01').toISOString();

      const record = {
        ...TestData.activeRecord,
        records: [
          {
            uri: 'https://example.org/old.html',
            quality: 0.8,
            lastModified: older,
            status: 'active'
          },
          {
            uri: 'https://example.org/new.html',
            quality: 0.8,
            lastModified: newer,
            status: 'active'
          }
        ]
      };

      mockResolver.addRecord(TestData.activeId, record);

      const result = await client.resolve(TestData.activeId);
      expect(result.uri).to.include('new.html');
    });

    it('should prefer language match in ranking', async () => {
      const record = {
        ...TestData.activeRecord,
        records: [
          {
            uri: 'https://example.org/en.html',
            language: 'en',
            quality: 0.7,
            status: 'active'
          },
          {
            uri: 'https://example.org/de.html',
            language: 'de',
            quality: 0.8,
            status: 'active'
          }
        ]
      };

      mockResolver.addRecord(TestData.activeId, record);

      const result = await client.resolve(TestData.activeId, { language: 'en' });
      expect(result.uri).to.include('en.html');
    });
  });

  describe('Step 5: Response Generation', () => {
    it('should return redirect response by default', async () => {
      mockResolver.addRecord(TestData.activeId, TestData.activeRecord);

      const result = await client.resolve(TestData.activeId);
      expect(result.type).to.equal('redirect');
      expect(result.uri).to.be.a('string');
      expect(result.uri).to.match(/^https?:\/\//);
    });

    it('should return metadata response when requested', async () => {
      mockResolver.addRecord(TestData.activeId, TestData.activeRecord);

      const result = await client.resolve(TestData.activeId, { metadata: true });
      expect(result.type).to.equal('metadata');
      expect(result.data).to.be.an('object');
      expect(result.data.id).to.equal(TestData.activeId);
    });

    it('should include quality score in redirect response', async () => {
      const record = {
        ...TestData.activeRecord,
        records: [{
          ...TestData.activeRecord.records[0],
          quality: 0.85
        }]
      };

      mockResolver.addRecord(TestData.activeId, record);

      const result = await client.resolve(TestData.activeId);
      expect(result.quality).to.equal(0.85);
    });

    it('should include resolver information', async () => {
      mockResolver.addRecord(TestData.activeId, TestData.activeRecord);

      const result = await client.resolve(TestData.activeId);
      expect(result.resolverUsed).to.be.a('string');
      expect(result.resolverUsed).to.include(mockResolver.url);
    });
  });

  describe('Step 6: Cache Behavior', () => {
    it('should respect cache headers', async () => {
      const cacheClient = new LinkIDClient({
        resolverUrl: mockResolver.url,
        caching: true
      });

      mockResolver.addRecord(TestData.activeId, TestData.activeRecord, {
        'Cache-Control': 'max-age=300'
      });

      // First request
      const result1 = await cacheClient.resolve(TestData.activeId);
      expect(result1.cached).to.be.false;

      // Second request should be cached
      const result2 = await cacheClient.resolve(TestData.activeId);
      expect(result2.cached).to.be.true;

      await cacheClient.clearCache();
    });

    it('should vary cache by request parameters', async () => {
      const cacheClient = new LinkIDClient({
        resolverUrl: mockResolver.url,
        caching: true
      });

      mockResolver.addRecord(TestData.multiFormatId, TestData.multiFormatRecord);

      // Different format requests should not share cache
      await cacheClient.resolve(TestData.multiFormatId, { format: 'pdf' });
      const result = await cacheClient.resolve(TestData.multiFormatId, { format: 'html' });
      expect(result.cached).to.be.false;

      await cacheClient.clearCache();
    });
  });

  describe('Error Handling and Edge Cases', () => {
    it('should handle malformed JSON responses', async () => {
      mockResolver.addMalformedResponse(TestData.activeId);

      try {
        await client.resolve(TestData.activeId, { metadata: true });
        expect.fail('Should have thrown error for malformed response');
      } catch (error) {
        expect(error.code).to.equal('NETWORK_ERROR');
      }
    });

    it('should handle network timeouts', async () => {
      const timeoutClient = new LinkIDClient({
        resolverUrl: mockResolver.url,
        timeout: 100 // Very short timeout
      });

      mockResolver.addSlowResponse(TestData.activeId, 200); // 200ms delay

      try {
        await timeoutClient.resolve(TestData.activeId);
        expect.fail('Should have thrown timeout error');
      } catch (error) {
        expect(error.code).to.equal('NETWORK_ERROR');
        expect(error.message).to.include('timeout');
      }
    });

    it('should retry on 5xx errors', async () => {
      mockResolver.addErrorResponse(TestData.activeId, 500, 2); // Fail twice, then succeed

      const result = await client.resolve(TestData.activeId);
      expect(result.type).to.equal('redirect');
      expect(mockResolver.getRequestCount(TestData.activeId)).to.equal(3);
    });

    it('should not retry on 4xx errors', async () => {
      mockResolver.addErrorResponse(TestData.activeId, 404, 10); // Always fail

      try {
        await client.resolve(TestData.activeId);
        expect.fail('Should have thrown not found error');
      } catch (error) {
        expect(error.code).to.equal('LINKID_NOT_FOUND');
        expect(mockResolver.getRequestCount(TestData.activeId)).to.equal(1);
      }
    });
  });
});