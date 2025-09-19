/**
 * ResolverService - Core resolution logic for LinkID identifiers
 */

const crypto = require('crypto');

class ResolverService {
  constructor(registryService, cacheService, logger) {
    this.registry = registryService;
    this.cache = cacheService;
    this.logger = logger;
  }

  /**
   * Resolve a LinkID to its current resource location
   * @param {string} id - The LinkID identifier
   * @param {Object} requestParams - Request parameters for content negotiation
   * @returns {Object} Resolution result
   */
  async resolve(id, requestParams) {
    // 1. Validate identifier format
    if (!this.isValidLinkID(id)) {
      const error = new Error('Invalid LinkID format');
      error.code = 'INVALID_LINKID';
      throw error;
    }

    // 2. Check cache first
    const cacheKey = this.generateCacheKey(id, requestParams);
    const cached = await this.cache.get(cacheKey);
    if (cached) {
      this.logger.debug(`Cache hit for LinkID: ${id}`);
      return cached;
    }

    // 3. Query registry
    const record = await this.registry.get(id);
    if (!record) {
      const error = new Error('LinkID not found');
      error.code = 'LINKID_NOT_FOUND';
      throw error;
    }

    // 4. Check if withdrawn
    if (record.status === 'withdrawn') {
      const error = new Error('LinkID withdrawn');
      error.code = 'LINKID_WITHDRAWN';
      error.tombstone = record.tombstone;
      throw error;
    }

    // 5. Filter and rank candidates
    const candidates = this.filterCandidates(record.records, requestParams);
    if (candidates.length === 0) {
      const error = new Error('No matching records found');
      error.code = 'NO_MATCHING_RECORDS';
      throw error;
    }

    const rankedCandidates = this.rankCandidates(candidates, requestParams);

    // 6. Determine response type
    const result = requestParams.preferRedirect
      ? this.createRedirectResponse(rankedCandidates[0], record)
      : this.createMetadataResponse(record, rankedCandidates);

    // 7. Cache the result
    const cacheTTL = record.policy?.cacheTTL || 3600;
    await this.cache.set(cacheKey, result, cacheTTL);

    return result;
  }

  /**
   * Register a new LinkID
   * @param {Object} data - Registration data
   * @returns {Object} Registration result
   */
  async register(data) {
    const id = this.generateLinkID();
    const now = new Date().toISOString();

    const record = {
      id,
      status: 'active',
      created: now,
      updated: now,
      issuer: data.issuer,
      records: [
        {
          uri: data.targetUri,
          status: 'active',
          mediaType: data.mediaType || 'text/html',
          language: data.language || 'en',
          quality: 1.0,
          validFrom: now,
          validUntil: null,
          lastModified: now,
          metadata: data.metadata || {}
        }
      ],
      policy: {
        cacheTTL: 3600,
        allowUpdates: true
      }
    };

    await this.registry.create(record);

    this.logger.info(`Registered LinkID: ${id}`, {
      targetUri: data.targetUri,
      issuer: data.issuer
    });

    return {
      id,
      created: now
    };
  }

  /**
   * Update an existing LinkID
   * @param {string} id - LinkID to update
   * @param {Object} updates - Update data
   * @param {string} userId - User performing the update
   */
  async update(id, updates, userId) {
    const record = await this.registry.get(id);
    if (!record) {
      const error = new Error('LinkID not found');
      error.code = 'LINKID_NOT_FOUND';
      throw error;
    }

    if (record.issuer !== userId) {
      const error = new Error('Not authorized to update this LinkID');
      error.code = 'UNAUTHORIZED';
      throw error;
    }

    const updatedRecord = {
      ...record,
      updated: new Date().toISOString(),
      ...updates
    };

    await this.registry.update(id, updatedRecord);

    // Invalidate cache
    await this.cache.invalidatePattern(`linkid:${id}:*`);

    this.logger.info(`Updated LinkID: ${id}`, { userId });
  }

  /**
   * Withdraw a LinkID
   * @param {string} id - LinkID to withdraw
   * @param {Object} tombstoneData - Tombstone information
   * @param {string} userId - User performing the withdrawal
   */
  async withdraw(id, tombstoneData, userId) {
    const record = await this.registry.get(id);
    if (!record) {
      const error = new Error('LinkID not found');
      error.code = 'LINKID_NOT_FOUND';
      throw error;
    }

    if (record.issuer !== userId) {
      const error = new Error('Not authorized to withdraw this LinkID');
      error.code = 'UNAUTHORIZED';
      throw error;
    }

    const withdrawnRecord = {
      ...record,
      status: 'withdrawn',
      updated: new Date().toISOString(),
      tombstone: {
        withdrawnAt: new Date().toISOString(),
        reason: tombstoneData.reason,
        contact: tombstoneData.contact || record.issuer,
        ...tombstoneData.tombstone
      }
    };

    await this.registry.update(id, withdrawnRecord);

    // Invalidate cache
    await this.cache.invalidatePattern(`linkid:${id}:*`);

    this.logger.info(`Withdrew LinkID: ${id}`, { userId, reason: tombstoneData.reason });
  }

  /**
   * Validate LinkID format
   * @param {string} id - LinkID to validate
   * @returns {boolean} True if valid
   */
  isValidLinkID(id) {
    if (!id || typeof id !== 'string') return false;
    if (id.length < 32 || id.length > 64) return false;
    return /^[A-Za-z0-9._~-]+$/.test(id);
  }

  /**
   * Generate a new LinkID using UUID v4
   * @returns {string} New LinkID
   */
  generateLinkID() {
    return crypto.randomUUID().replace(/-/g, '').toLowerCase();
  }

  /**
   * Generate cache key for resolution results
   * @param {string} id - LinkID
   * @param {Object} params - Request parameters
   * @returns {string} Cache key
   */
  generateCacheKey(id, params) {
    const keyData = {
      id,
      format: params.format,
      language: params.language,
      version: params.version,
      preferRedirect: params.preferRedirect
    };
    const keyString = JSON.stringify(keyData);
    const hash = crypto.createHash('sha256').update(keyString).digest('hex');
    return `linkid:${id}:${hash.substring(0, 16)}`;
  }

  /**
   * Filter candidates based on request parameters
   * @param {Array} records - Available records
   * @param {Object} params - Request parameters
   * @returns {Array} Filtered records
   */
  filterCandidates(records, params) {
    return records.filter(record => {
      // Filter by status
      if (record.status !== 'active') return false;

      // Filter by validity period
      const now = new Date();
      if (record.validFrom && new Date(record.validFrom) > now) return false;
      if (record.validUntil && new Date(record.validUntil) < now) return false;

      // Filter by media type
      if (params.format) {
        const expectedMediaType = this.formatToMediaType(params.format);
        if (record.mediaType && !record.mediaType.includes(expectedMediaType)) {
          return false;
        }
      }

      // Filter by language
      if (params.language) {
        if (record.language && !record.language.startsWith(params.language)) {
          return false;
        }
      }

      return true;
    });
  }

  /**
   * Rank candidates by quality, freshness, and other factors
   * @param {Array} candidates - Filtered candidates
   * @param {Object} params - Request parameters
   * @returns {Array} Ranked candidates
   */
  rankCandidates(candidates, params) {
    return candidates.sort((a, b) => {
      // Primary: Quality score
      const qualityDiff = (b.quality || 0) - (a.quality || 0);
      if (qualityDiff !== 0) return qualityDiff;

      // Secondary: Freshness (last modified)
      const aModified = new Date(a.lastModified || a.validFrom || 0);
      const bModified = new Date(b.lastModified || b.validFrom || 0);
      const freshnessDiff = bModified.getTime() - aModified.getTime();
      if (freshnessDiff !== 0) return freshnessDiff;

      // Tertiary: Language preference
      if (params.language) {
        const aLangMatch = a.language?.startsWith(params.language) ? 1 : 0;
        const bLangMatch = b.language?.startsWith(params.language) ? 1 : 0;
        const langDiff = bLangMatch - aLangMatch;
        if (langDiff !== 0) return langDiff;
      }

      return 0;
    });
  }

  /**
   * Create redirect response
   * @param {Object} candidate - Best candidate record
   * @param {Object} record - Full LinkID record
   * @returns {Object} Redirect response
   */
  createRedirectResponse(candidate, record) {
    return {
      type: 'redirect',
      uri: candidate.uri,
      quality: candidate.quality,
      permanent: false, // Use 302 by default
      cacheTTL: record.policy?.cacheTTL || 3600
    };
  }

  /**
   * Create metadata response
   * @param {Object} record - Full LinkID record
   * @param {Array} rankedCandidates - Ranked candidate records
   * @returns {Object} Metadata response
   */
  createMetadataResponse(record, rankedCandidates) {
    const etag = crypto
      .createHash('sha256')
      .update(JSON.stringify(record) + record.updated)
      .digest('hex')
      .substring(0, 16);

    return {
      type: 'metadata',
      data: {
        ...record,
        records: rankedCandidates
      },
      etag: `"${etag}"`,
      cacheTTL: record.policy?.cacheTTL || 1800
    };
  }

  /**
   * Convert format parameter to media type
   * @param {string} format - Format parameter
   * @returns {string} Media type
   */
  formatToMediaType(format) {
    const formatMap = {
      'pdf': 'application/pdf',
      'html': 'text/html',
      'json': 'application/json',
      'xml': 'application/xml',
      'txt': 'text/plain'
    };
    return formatMap[format] || format;
  }
}

module.exports = ResolverService;