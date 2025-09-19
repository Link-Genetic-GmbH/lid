/**
 * Error classes for LinkID Client SDK
 */

/**
 * Base error class for LinkID operations
 */
export class LinkIDError extends Error {
  public readonly code: string;
  public readonly timestamp: string;

  constructor(message: string, code: string = 'LINKID_ERROR') {
    super(message);
    this.name = 'LinkIDError';
    this.code = code;
    this.timestamp = new Date().toISOString();

    // Maintain proper stack trace for where our error was thrown (only available on V8)
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, LinkIDError);
    }
  }
}

/**
 * Error thrown when a LinkID is not found
 */
export class LinkIDNotFoundError extends LinkIDError {
  public readonly linkId: string;

  constructor(linkId: string, message: string = 'LinkID not found') {
    super(message, 'LINKID_NOT_FOUND');
    this.name = 'LinkIDNotFoundError';
    this.linkId = linkId;
  }
}

/**
 * Error thrown when a LinkID has been withdrawn
 */
export class LinkIDWithdrawnError extends LinkIDError {
  public readonly linkId: string;
  public readonly tombstone?: any;

  constructor(linkId: string, message: string = 'LinkID withdrawn', tombstone?: any) {
    super(message, 'LINKID_WITHDRAWN');
    this.name = 'LinkIDWithdrawnError';
    this.linkId = linkId;
    this.tombstone = tombstone;
  }
}

/**
 * Error thrown for validation failures
 */
export class ValidationError extends LinkIDError {
  constructor(message: string) {
    super(message, 'VALIDATION_ERROR');
    this.name = 'ValidationError';
  }
}

/**
 * Error thrown for network-related failures
 */
export class NetworkError extends LinkIDError {
  public readonly statusCode?: number;
  public readonly response?: Response;

  constructor(message: string, statusCode?: number, response?: Response) {
    super(message, 'NETWORK_ERROR');
    this.name = 'NetworkError';
    this.statusCode = statusCode;
    this.response = response;
  }
}

/**
 * Error thrown for authentication failures
 */
export class AuthenticationError extends LinkIDError {
  constructor(message: string = 'Authentication failed') {
    super(message, 'AUTHENTICATION_ERROR');
    this.name = 'AuthenticationError';
  }
}

/**
 * Error thrown for authorization failures
 */
export class AuthorizationError extends LinkIDError {
  constructor(message: string = 'Access denied') {
    super(message, 'AUTHORIZATION_ERROR');
    this.name = 'AuthorizationError';
  }
}

/**
 * Error thrown when rate limit is exceeded
 */
export class RateLimitError extends LinkIDError {
  public readonly retryAfter?: number;

  constructor(message: string = 'Rate limit exceeded', retryAfter?: number) {
    super(message, 'RATE_LIMIT_ERROR');
    this.name = 'RateLimitError';
    this.retryAfter = retryAfter;
  }
}

/**
 * Error thrown for timeout failures
 */
export class TimeoutError extends NetworkError {
  constructor(message: string = 'Request timeout') {
    super(message);
    this.name = 'TimeoutError';
    this.code = 'TIMEOUT_ERROR';
  }
}

/**
 * Error thrown for malformed responses
 */
export class ResponseError extends NetworkError {
  constructor(message: string = 'Invalid response format') {
    super(message);
    this.name = 'ResponseError';
    this.code = 'RESPONSE_ERROR';
  }
}

/**
 * Error thrown for cache-related failures
 */
export class CacheError extends LinkIDError {
  constructor(message: string) {
    super(message, 'CACHE_ERROR');
    this.name = 'CacheError';
  }
}

/**
 * Utility function to determine if an error is retryable
 */
export function isRetryableError(error: Error): boolean {
  if (error instanceof NetworkError) {
    // Retry on 5xx errors but not 4xx errors
    return !error.statusCode || error.statusCode >= 500;
  }

  if (error instanceof TimeoutError) {
    return true;
  }

  // Don't retry on validation, authentication, or LinkID-specific errors
  if (error instanceof ValidationError ||
      error instanceof AuthenticationError ||
      error instanceof AuthorizationError ||
      error instanceof LinkIDNotFoundError ||
      error instanceof LinkIDWithdrawnError) {
    return false;
  }

  return true;
}

/**
 * Utility function to extract error details for logging
 */
export function getErrorDetails(error: Error): Record<string, any> {
  const details: Record<string, any> = {
    name: error.name,
    message: error.message,
    stack: error.stack
  };

  if (error instanceof LinkIDError) {
    details.code = error.code;
    details.timestamp = error.timestamp;
  }

  if (error instanceof LinkIDNotFoundError || error instanceof LinkIDWithdrawnError) {
    details.linkId = error.linkId;
  }

  if (error instanceof LinkIDWithdrawnError) {
    details.tombstone = error.tombstone;
  }

  if (error instanceof NetworkError) {
    details.statusCode = error.statusCode;
  }

  if (error instanceof RateLimitError) {
    details.retryAfter = error.retryAfter;
  }

  return details;
}