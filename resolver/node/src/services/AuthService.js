/**
 * AuthService - Authentication and authorization for LinkID resolver
 */

const jwt = require('jsonwebtoken');
const crypto = require('crypto');

class AuthService {
  constructor() {
    this.jwtSecret = process.env.JWT_SECRET || this.generateSecret();
    this.jwtExpiry = process.env.JWT_EXPIRY || '24h';

    // API key storage (in production, use a proper database)
    this.apiKeys = new Map();
    this.loadApiKeys();
  }

  /**
   * Generate a secure secret for JWT signing
   * @returns {string} Random secret
   */
  generateSecret() {
    return crypto.randomBytes(64).toString('hex');
  }

  /**
   * Load API keys from environment or database
   */
  loadApiKeys() {
    // In production, load from secure database
    const envKeys = process.env.API_KEYS;
    if (envKeys) {
      const keys = JSON.parse(envKeys);
      for (const [key, data] of Object.entries(keys)) {
        this.apiKeys.set(key, {
          userId: data.userId,
          scopes: data.scopes || ['read'],
          created: new Date(data.created),
          lastUsed: data.lastUsed ? new Date(data.lastUsed) : null,
          rateLimit: data.rateLimit || 1000
        });
      }
    }
  }

  /**
   * Middleware to authenticate requests
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   * @param {Function} next - Next middleware function
   */
  authenticate = async (req, res, next) => {
    try {
      const authHeader = req.get('Authorization');

      if (!authHeader) {
        return res.status(401).json({
          error: 'Authorization header required',
          code: 'MISSING_AUTH_HEADER'
        });
      }

      const [scheme, token] = authHeader.split(' ');

      if (scheme === 'Bearer') {
        // JWT token authentication
        const user = await this.verifyJWT(token);
        req.user = user;
        req.authMethod = 'jwt';
      } else if (scheme === 'ApiKey') {
        // API key authentication
        const user = await this.verifyApiKey(token);
        req.user = user;
        req.authMethod = 'apikey';
      } else {
        return res.status(401).json({
          error: 'Invalid authentication scheme',
          code: 'INVALID_AUTH_SCHEME'
        });
      }

      next();
    } catch (error) {
      if (error.name === 'JsonWebTokenError') {
        return res.status(401).json({
          error: 'Invalid token',
          code: 'INVALID_TOKEN'
        });
      }

      if (error.name === 'TokenExpiredError') {
        return res.status(401).json({
          error: 'Token expired',
          code: 'TOKEN_EXPIRED'
        });
      }

      if (error.code === 'INVALID_API_KEY') {
        return res.status(401).json({
          error: 'Invalid API key',
          code: 'INVALID_API_KEY'
        });
      }

      console.error('Authentication error:', error);
      res.status(500).json({
        error: 'Authentication failed',
        code: 'AUTH_ERROR'
      });
    }
  };

  /**
   * Verify JWT token
   * @param {string} token - JWT token
   * @returns {Promise<Object>} User information
   */
  async verifyJWT(token) {
    return new Promise((resolve, reject) => {
      jwt.verify(token, this.jwtSecret, (err, decoded) => {
        if (err) {
          reject(err);
        } else {
          resolve(decoded);
        }
      });
    });
  }

  /**
   * Verify API key
   * @param {string} apiKey - API key
   * @returns {Promise<Object>} User information
   */
  async verifyApiKey(apiKey) {
    const keyData = this.apiKeys.get(apiKey);

    if (!keyData) {
      const error = new Error('Invalid API key');
      error.code = 'INVALID_API_KEY';
      throw error;
    }

    // Update last used timestamp
    keyData.lastUsed = new Date();
    this.apiKeys.set(apiKey, keyData);

    return {
      sub: keyData.userId,
      scopes: keyData.scopes,
      authMethod: 'apikey',
      rateLimit: keyData.rateLimit
    };
  }

  /**
   * Generate JWT token for user
   * @param {Object} user - User information
   * @returns {string} JWT token
   */
  generateJWT(user) {
    const payload = {
      sub: user.id,
      email: user.email,
      scopes: user.scopes || ['read'],
      iat: Math.floor(Date.now() / 1000)
    };

    return jwt.sign(payload, this.jwtSecret, {
      expiresIn: this.jwtExpiry,
      issuer: 'linkid-resolver',
      audience: 'linkid-api'
    });
  }

  /**
   * Generate new API key for user
   * @param {string} userId - User ID
   * @param {Array} scopes - Allowed scopes
   * @returns {string} API key
   */
  generateApiKey(userId, scopes = ['read']) {
    const apiKey = crypto.randomBytes(32).toString('hex');

    this.apiKeys.set(apiKey, {
      userId,
      scopes,
      created: new Date(),
      lastUsed: null,
      rateLimit: 1000
    });

    return apiKey;
  }

  /**
   * Revoke API key
   * @param {string} apiKey - API key to revoke
   * @returns {boolean} True if revoked
   */
  revokeApiKey(apiKey) {
    return this.apiKeys.delete(apiKey);
  }

  /**
   * Check if user has required scope
   * @param {Object} user - User object from auth
   * @param {string} requiredScope - Required scope
   * @returns {boolean} True if authorized
   */
  hasScope(user, requiredScope) {
    if (!user.scopes) return false;
    return user.scopes.includes(requiredScope) || user.scopes.includes('admin');
  }

  /**
   * Middleware to check specific scope
   * @param {string} requiredScope - Required scope
   * @returns {Function} Middleware function
   */
  requireScope(requiredScope) {
    return (req, res, next) => {
      if (!req.user) {
        return res.status(401).json({
          error: 'Authentication required',
          code: 'AUTH_REQUIRED'
        });
      }

      if (!this.hasScope(req.user, requiredScope)) {
        return res.status(403).json({
          error: 'Insufficient permissions',
          code: 'INSUFFICIENT_SCOPE',
          required: requiredScope
        });
      }

      next();
    };
  }

  /**
   * Get user's API keys
   * @param {string} userId - User ID
   * @returns {Array} Array of API key information
   */
  getUserApiKeys(userId) {
    const userKeys = [];

    for (const [key, data] of this.apiKeys.entries()) {
      if (data.userId === userId) {
        userKeys.push({
          key: key.substring(0, 8) + '...' + key.substring(key.length - 8),
          scopes: data.scopes,
          created: data.created,
          lastUsed: data.lastUsed
        });
      }
    }

    return userKeys;
  }

  /**
   * Validate password strength
   * @param {string} password - Password to validate
   * @returns {Object} Validation result
   */
  validatePassword(password) {
    const minLength = 8;
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumbers = /\d/.test(password);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password);

    const isValid = password.length >= minLength &&
                   hasUpperCase &&
                   hasLowerCase &&
                   hasNumbers &&
                   hasSpecialChar;

    return {
      isValid,
      errors: [
        ...(password.length < minLength ? ['Password must be at least 8 characters'] : []),
        ...(!hasUpperCase ? ['Password must contain uppercase letters'] : []),
        ...(!hasLowerCase ? ['Password must contain lowercase letters'] : []),
        ...(!hasNumbers ? ['Password must contain numbers'] : []),
        ...(!hasSpecialChar ? ['Password must contain special characters'] : [])
      ]
    };
  }

  /**
   * Hash password using bcrypt-like algorithm
   * @param {string} password - Plain text password
   * @returns {string} Hashed password
   */
  hashPassword(password) {
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512');
    return `${salt}:${hash.toString('hex')}`;
  }

  /**
   * Verify password against hash
   * @param {string} password - Plain text password
   * @param {string} hashedPassword - Hashed password
   * @returns {boolean} True if password matches
   */
  verifyPassword(password, hashedPassword) {
    const [salt, originalHash] = hashedPassword.split(':');
    const hash = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512');
    return originalHash === hash.toString('hex');
  }
}

module.exports = AuthService;