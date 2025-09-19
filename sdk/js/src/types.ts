/**
 * TypeScript type definitions for LinkID Client SDK
 */

/**
 * Client configuration options
 */
export interface LinkIDClientConfig {
  /** Base URL of the LinkID resolver */
  resolverUrl?: string;
  /** API key for authenticated operations */
  apiKey?: string;
  /** Request timeout in milliseconds */
  timeout?: number;
  /** Number of retry attempts for failed requests */
  retries?: number;
  /** Enable client-side caching */
  caching?: boolean;
  /** Default cache TTL in seconds */
  cacheTTL?: number;
  /** Custom User-Agent string */
  userAgent?: string;
  /** Additional headers to include with requests */
  headers?: Record<string, string>;
  /** Validate SSL certificates */
  validateSSL?: boolean;
  /** Custom cache implementation */
  cache?: Cache;
}

/**
 * Cache interface
 */
export interface Cache {
  get<T>(key: string): Promise<T | null>;
  set<T>(key: string, value: T, ttl?: number): Promise<void>;
  delete(pattern: string): Promise<void>;
  clear(): Promise<void>;
  getStats(): CacheStats;
}

/**
 * Cache statistics
 */
export interface CacheStats {
  hits: number;
  misses: number;
  size: number;
  hitRate: number;
}

/**
 * Cache configuration options
 */
export interface CacheOptions {
  maxSize?: number;
  defaultTTL?: number;
}

/**
 * Resolution options
 */
export interface ResolutionOptions {
  /** Preferred format (pdf, html, json, etc.) */
  format?: string;
  /** Preferred language (ISO 639-1 code) */
  language?: string;
  /** Specific version number */
  version?: number;
  /** Timestamp for historical resolution */
  timestamp?: string;
  /** Return metadata instead of redirect */
  metadata?: boolean;
  /** Bypass cache for this request */
  bypassCache?: boolean;
  /** Additional headers for this request */
  headers?: Record<string, string>;
}

/**
 * Resolution result for redirect responses
 */
export interface RedirectResolutionResult {
  type: 'redirect';
  /** Target URI */
  uri: string;
  /** Original LinkID */
  linkId: string;
  /** Quality score (0-1) */
  quality?: number;
  /** Whether result came from cache */
  cached: boolean;
  /** Resolver that provided the result */
  resolverUsed: string;
}

/**
 * Resolution result for metadata responses
 */
export interface MetadataResolutionResult {
  type: 'metadata';
  /** Full LinkID record */
  data: LinkIDRecord;
  /** Original LinkID */
  linkId: string;
  /** Whether result came from cache */
  cached: boolean;
  /** Resolver that provided the result */
  resolverUsed: string;
}

/**
 * Union type for resolution results
 */
export type ResolutionResult = RedirectResolutionResult | MetadataResolutionResult;

/**
 * LinkID record structure
 */
export interface LinkIDRecord {
  /** LinkID identifier */
  id: string;
  /** Record status */
  status: 'active' | 'withdrawn' | 'pending';
  /** Creation timestamp */
  created: string;
  /** Last update timestamp */
  updated: string;
  /** Issuer identifier */
  issuer: string;
  /** Resolution records */
  records: ResolutionRecord[];
  /** Alternative identifiers */
  alternates?: AlternateIdentifier[];
  /** Resolution policy */
  policy?: ResolutionPolicy;
  /** Tombstone information (if withdrawn) */
  tombstone?: Tombstone;
  /** Digital signatures */
  signatures?: DigitalSignature[];
  /** Telemetry data */
  telemetry?: TelemetryData;
}

/**
 * Individual resolution record
 */
export interface ResolutionRecord {
  /** Target URI */
  uri: string;
  /** Record status */
  status: 'active' | 'inactive' | 'deprecated';
  /** Media type */
  mediaType?: string;
  /** Language */
  language?: string;
  /** Quality score (0-1) */
  quality?: number;
  /** Valid from timestamp */
  validFrom?: string;
  /** Valid until timestamp */
  validUntil?: string;
  /** Content checksum */
  checksum?: Checksum;
  /** Content size in bytes */
  size?: number;
  /** Last modified timestamp */
  lastModified?: string;
  /** Additional metadata */
  metadata?: Record<string, any>;
}

/**
 * Content checksum
 */
export interface Checksum {
  /** Hash algorithm */
  algorithm: 'sha256' | 'sha3-256' | 'blake2b';
  /** Hash value */
  value: string;
}

/**
 * Alternative identifier
 */
export interface AlternateIdentifier {
  /** Identifier scheme */
  scheme: 'doi' | 'ark' | 'handle' | 'isbn' | 'issn';
  /** Identifier value */
  identifier: string;
}

/**
 * Resolution policy
 */
export interface ResolutionPolicy {
  /** Cache TTL in seconds */
  cacheTTL?: number;
  /** Allow updates */
  allowUpdates?: boolean;
  /** Fallback resolvers */
  fallbackResolvers?: string[];
  /** Preferred format */
  preferredFormat?: string;
}

/**
 * Tombstone information
 */
export interface Tombstone {
  /** Withdrawal timestamp */
  withdrawnAt: string;
  /** Reason for withdrawal */
  reason?: string;
  /** Contact information */
  contact?: string;
  /** Alternative location */
  alternativeLocation?: string;
}

/**
 * Digital signature
 */
export interface DigitalSignature {
  /** Signature algorithm */
  algorithm: 'eddsa' | 'ecdsa' | 'rsa-pss';
  /** Public key */
  publicKey: string;
  /** Signature value */
  signature: string;
  /** Signing timestamp */
  timestamp: string;
}

/**
 * Telemetry data
 */
export interface TelemetryData {
  /** Total resolutions */
  resolutions?: number;
  /** Last resolved timestamp */
  lastResolved?: string;
  /** Popularity score */
  popularityScore?: number;
}

/**
 * Registration request
 */
export interface RegistrationRequest {
  /** Target URI */
  targetUri: string;
  /** Media type */
  mediaType?: string;
  /** Language */
  language?: string;
  /** Additional metadata */
  metadata?: Record<string, any>;
}

/**
 * Registration result
 */
export interface RegistrationResult {
  /** Generated LinkID */
  id: string;
  /** Full LinkID URI */
  uri: string;
  /** Creation timestamp */
  created: string;
}

/**
 * Update request
 */
export interface UpdateRequest {
  /** Updated records */
  records?: Partial<ResolutionRecord>[];
  /** Updated metadata */
  metadata?: Record<string, any>;
  /** Updated policy */
  policy?: Partial<ResolutionPolicy>;
}

/**
 * Withdrawal request
 */
export interface WithdrawalRequest {
  /** Reason for withdrawal */
  reason?: string;
  /** Contact information */
  contact?: string;
  /** Alternative location */
  alternativeLocation?: string;
  /** Additional tombstone data */
  tombstone?: Record<string, any>;
}

/**
 * Resolver discovery result
 */
export interface ResolverInfo {
  /** Resolver URL */
  url: string;
  /** Resolver capabilities */
  capabilities: string[];
  /** Rate limits */
  rateLimits?: {
    perMinute: number;
    perHour: number;
  };
}

/**
 * Error response structure
 */
export interface ErrorResponse {
  /** Error message */
  error: string;
  /** Error code */
  code?: string;
  /** Additional details */
  details?: any;
}