/**
 * LinkID Client - Main client class for LinkID operations
 */

import {
  LinkIDClientConfig,
  LinkIDRecord,
  ResolutionOptions,
  ResolutionResult,
  RegistrationRequest,
  RegistrationResult,
  UpdateRequest,
  WithdrawalRequest,
  Cache
} from './types';
import { MemoryCache } from './cache';
import { ResolverDiscovery } from './discovery';
import {
  LinkIDError,
  LinkIDNotFoundError,
  LinkIDWithdrawnError,
  NetworkError,
  ValidationError
} from './errors';

/**
 * Main LinkID client class
 */
export class LinkIDClient {
  private readonly config: LinkIDClientConfig & {
    resolverUrl: string;
    timeout: number;
    retries: number;
    caching: boolean;
    cacheTTL: number;
    userAgent: string;
    headers: Record<string, string>;
    validateSSL: boolean;
  };
  private readonly cache: Cache;
  private readonly discovery: ResolverDiscovery;

  constructor(config: LinkIDClientConfig) {
    // Set default configuration
    this.config = {
      resolverUrl: config.resolverUrl || 'https://resolver.linkid.org',
      apiKey: config.apiKey,
      timeout: config.timeout || 10000,
      retries: config.retries || 3,
      caching: config.caching !== false,
      cacheTTL: config.cacheTTL || 3600,
      userAgent: config.userAgent || `LinkID-Client-JS/1.0.0`,
      headers: config.headers || {},
      validateSSL: config.validateSSL !== false
    };

    // Initialize cache
    this.cache = config.cache || new MemoryCache({
      maxSize: 1000,
      defaultTTL: this.config.cacheTTL
    });

    // Initialize resolver discovery
    this.discovery = new ResolverDiscovery();
  }

  /**
   * Resolve a LinkID to its current resource location
   *
   * @param linkId - The LinkID identifier to resolve
   * @param options - Resolution options
   * @returns Promise resolving to the resolution result
   *
   * @example
   * ```typescript
   * const result = await client.resolve('b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14', {
   *   format: 'pdf',
   *   language: 'en'
   * });
   * ```
   */
  async resolve(
    linkId: string,
    options: ResolutionOptions = {}
  ): Promise<ResolutionResult> {
    this.validateLinkID(linkId);

    const cacheKey = this.generateCacheKey(linkId, options);

    // Check cache first
    if (this.config.caching && !options.bypassCache) {
      const cached = await this.cache.get<ResolutionResult>(cacheKey);
      if (cached) {
        return cached;
      }
    }

    try {
      // Build request URL
      const url = this.buildResolveUrl(linkId, options);

      // Make request
      const response = await this.makeRequest(url, {
        method: 'GET',
        headers: this.buildHeaders(options.headers)
      });

      let result: ResolutionResult;

      if (response.redirected) {
        // Handle redirect response
        result = {
          type: 'redirect',
          uri: response.url,
          linkId,
          quality: this.parseQualityHeader(response.headers.get('X-LinkID-Quality')),
          cached: false,
          resolverUsed: response.headers.get('X-LinkID-Resolver') || this.config.resolverUrl
        };
      } else {
        // Handle metadata response
        const data: LinkIDRecord = await response.json();
        result = {
          type: 'metadata',
          data,
          linkId,
          cached: false,
          resolverUsed: response.headers.get('X-LinkID-Resolver') || this.config.resolverUrl
        };
      }

      // Cache the result
      if (this.config.caching && response.ok) {
        const ttl = this.parseCacheControlHeader(response.headers.get('Cache-Control'));
        await this.cache.set(cacheKey, result, ttl);
      }

      return result;

    } catch (error) {
      if (error instanceof Response) {
        await this.handleErrorResponse(error, linkId);
      }
      throw error;
    }
  }

  /**
   * Register a new LinkID
   *
   * @param request - Registration request data
   * @returns Promise resolving to the registration result
   *
   * @example
   * ```typescript
   * const result = await client.register({
   *   targetUri: 'https://example.org/document.pdf',
   *   mediaType: 'application/pdf',
   *   metadata: { title: 'My Document' }
   * });
   * ```
   */
  async register(request: RegistrationRequest): Promise<RegistrationResult> {
    this.validateRegistrationRequest(request);

    if (!this.config.apiKey) {
      throw new ValidationError('API key required for registration');
    }

    const url = `${this.config.resolverUrl}/register`;

    try {
      const response = await this.makeRequest(url, {
        method: 'POST',
        headers: {
          ...this.buildHeaders(),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        await this.handleErrorResponse(response);
      }

      return await response.json();

    } catch (error) {
      if (error instanceof Response) {
        await this.handleErrorResponse(error);
      }
      throw error;
    }
  }

  /**
   * Update an existing LinkID
   *
   * @param linkId - The LinkID to update
   * @param request - Update request data
   * @returns Promise resolving when update is complete
   */
  async update(linkId: string, request: UpdateRequest): Promise<void> {
    this.validateLinkID(linkId);

    if (!this.config.apiKey) {
      throw new ValidationError('API key required for updates');
    }

    const url = `${this.config.resolverUrl}/resolve/${linkId}`;

    try {
      const response = await this.makeRequest(url, {
        method: 'PUT',
        headers: {
          ...this.buildHeaders(),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        await this.handleErrorResponse(response, linkId);
      }

      // Invalidate cache
      if (this.config.caching) {
        await this.cache.delete(`linkid:${linkId}:*`);
      }

    } catch (error) {
      if (error instanceof Response) {
        await this.handleErrorResponse(error, linkId);
      }
      throw error;
    }
  }

  /**
   * Withdraw a LinkID
   *
   * @param linkId - The LinkID to withdraw
   * @param request - Withdrawal request data
   * @returns Promise resolving when withdrawal is complete
   */
  async withdraw(linkId: string, request: WithdrawalRequest = {}): Promise<void> {
    this.validateLinkID(linkId);

    if (!this.config.apiKey) {
      throw new ValidationError('API key required for withdrawal');
    }

    const url = `${this.config.resolverUrl}/resolve/${linkId}`;

    try {
      const response = await this.makeRequest(url, {
        method: 'DELETE',
        headers: {
          ...this.buildHeaders(),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        await this.handleErrorResponse(response, linkId);
      }

      // Invalidate cache
      if (this.config.caching) {
        await this.cache.delete(`linkid:${linkId}:*`);
      }

    } catch (error) {
      if (error instanceof Response) {
        await this.handleErrorResponse(error, linkId);
      }
      throw error;
    }
  }

  /**
   * Discover available resolvers for a domain
   *
   * @param domain - Domain to discover resolvers for
   * @returns Promise resolving to array of resolver URLs
   */
  async discoverResolvers(domain: string): Promise<string[]> {
    return this.discovery.discover(domain);
  }

  /**
   * Clear the client cache
   */
  async clearCache(): Promise<void> {
    await this.cache.clear();
  }

  /**
   * Get cache statistics
   */
  getCacheStats() {
    return this.cache.getStats();
  }

  /**
   * Validate LinkID format
   */
  private validateLinkID(linkId: string): void {
    if (!linkId || typeof linkId !== 'string') {
      throw new ValidationError('LinkID must be a non-empty string');
    }

    if (linkId.length < 32 || linkId.length > 64) {
      throw new ValidationError('LinkID must be 32-64 characters long');
    }

    if (!/^[A-Za-z0-9._~-]+$/.test(linkId)) {
      throw new ValidationError('LinkID contains invalid characters');
    }
  }

  /**
   * Validate registration request
   */
  private validateRegistrationRequest(request: RegistrationRequest): void {
    if (!request.targetUri || typeof request.targetUri !== 'string') {
      throw new ValidationError('targetUri is required and must be a string');
    }

    try {
      new URL(request.targetUri);
    } catch {
      throw new ValidationError('targetUri must be a valid URL');
    }
  }

  /**
   * Build resolve URL with query parameters
   */
  private buildResolveUrl(linkId: string, options: ResolutionOptions): string {
    const url = new URL(`/resolve/${linkId}`, this.config.resolverUrl);

    if (options.format) url.searchParams.set('format', options.format);
    if (options.language) url.searchParams.set('lang', options.language);
    if (options.version) url.searchParams.set('version', options.version.toString());
    if (options.timestamp) url.searchParams.set('at', options.timestamp);
    if (options.metadata) url.searchParams.set('metadata', 'true');

    return url.toString();
  }

  /**
   * Build request headers
   */
  private buildHeaders(additional: Record<string, string> = {}): Record<string, string> {
    const headers: Record<string, string> = {
      'User-Agent': this.config.userAgent,
      'Accept': 'application/linkid+json, application/json, */*',
      ...this.config.headers,
      ...additional
    };

    if (this.config.apiKey) {
      headers['Authorization'] = `ApiKey ${this.config.apiKey}`;
    }

    return headers;
  }

  /**
   * Make HTTP request with retry logic
   */
  private async makeRequest(url: string, options: RequestInit): Promise<Response> {
    let lastError: Error;

    for (let attempt = 1; attempt <= this.config.retries; attempt++) {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), this.config.timeout);

        const response = await fetch(url, {
          ...options,
          signal: controller.signal
        });

        clearTimeout(timeoutId);

        if (response.ok || response.status >= 400) {
          return response;
        }

        // Retry on 5xx errors
        if (response.status >= 500 && attempt < this.config.retries) {
          await this.delay(Math.pow(2, attempt - 1) * 1000);
          continue;
        }

        return response;

      } catch (error) {
        lastError = error as Error;

        if (attempt < this.config.retries) {
          await this.delay(Math.pow(2, attempt - 1) * 1000);
          continue;
        }
      }
    }

    throw new NetworkError(`Request failed after ${this.config.retries} attempts: ${lastError!.message}`);
  }

  /**
   * Handle error responses
   */
  private async handleErrorResponse(response: Response, linkId?: string): Promise<never> {
    let errorData: any;

    try {
      errorData = await response.json();
    } catch {
      errorData = { error: 'Unknown error' };
    }

    switch (response.status) {
      case 404:
        throw new LinkIDNotFoundError(linkId || 'unknown', errorData.error || 'LinkID not found');

      case 410:
        throw new LinkIDWithdrawnError(
          linkId || 'unknown',
          errorData.error || 'LinkID withdrawn',
          errorData.tombstone
        );

      case 400:
      case 422:
        throw new ValidationError(errorData.error || 'Validation error');

      case 401:
        throw new LinkIDError(errorData.error || 'Authentication required', 'UNAUTHORIZED');

      case 403:
        throw new LinkIDError(errorData.error || 'Access forbidden', 'FORBIDDEN');

      case 429:
        throw new LinkIDError(errorData.error || 'Rate limit exceeded', 'RATE_LIMITED');

      default:
        throw new LinkIDError(
          errorData.error || `HTTP ${response.status}: ${response.statusText}`,
          'HTTP_ERROR'
        );
    }
  }

  /**
   * Generate cache key for resolution
   */
  private generateCacheKey(linkId: string, options: ResolutionOptions): string {
    const parts = [
      'linkid',
      linkId,
      options.format || '',
      options.language || '',
      options.version || '',
      options.metadata ? '1' : '0'
    ];

    return parts.join(':');
  }

  /**
   * Parse quality header value
   */
  private parseQualityHeader(header: string | null): number | undefined {
    if (!header) return undefined;
    const quality = parseFloat(header);
    return isNaN(quality) ? undefined : quality;
  }

  /**
   * Parse Cache-Control header for TTL
   */
  private parseCacheControlHeader(header: string | null): number {
    if (!header) return this.config.cacheTTL;

    const match = header.match(/max-age=(\d+)/);
    if (match) {
      return parseInt(match[1], 10);
    }

    return this.config.cacheTTL;
  }

  /**
   * Simple delay utility
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}