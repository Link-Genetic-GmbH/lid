/**
 * LinkID Client SDK
 *
 * A comprehensive TypeScript/JavaScript client library for interacting with
 * LinkID persistent identifier resolvers. Supports resolution, registration,
 * caching, and advanced features like semantic search and federation.
 *
 * @example
 * ```typescript
 * import { LinkIDClient } from '@linkid/client';
 *
 * const client = new LinkIDClient({
 *   resolverUrl: 'https://resolver.linkid.org',
 *   apiKey: 'your-api-key'
 * });
 *
 * // Resolve a LinkID
 * const result = await client.resolve('b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14');
 * console.log(result.uri); // https://example.org/document.pdf
 *
 * // Register a new LinkID
 * const linkId = await client.register({
 *   targetUri: 'https://example.org/document.pdf',
 *   mediaType: 'application/pdf'
 * });
 * ```
 *
 * @author Link Genetic GmbH
 * @version 1.0.0
 * @license MIT
 */

// Import polyfill for fetch in Node.js environments
import 'cross-fetch/polyfill';

export * from './types';
export * from './client';
export * from './cache';
export * from './discovery';
export * from './errors';

// Default export
export { LinkIDClient as default } from './client';