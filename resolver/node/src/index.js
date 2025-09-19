/**
 * LinkID Resolver - Node.js Implementation
 *
 * A high-performance HTTP resolver for LinkID persistent identifiers.
 * Implements the LinkID resolution specification with caching, authentication,
 * and federated resolver discovery.
 */

const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const { body, param, query, validationResult } = require('express-validator');
const winston = require('winston');
require('dotenv').config();

const ResolverService = require('./services/ResolverService');
const CacheService = require('./services/CacheService');
const RegistryService = require('./services/RegistryService');
const AuthService = require('./services/AuthService');

const app = express();
const PORT = process.env.PORT || 3000;

// Configure logging
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  transports: [
    new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
    new winston.transports.File({ filename: 'logs/combined.log' }),
    new winston.transports.Console({
      format: winston.format.simple()
    })
  ]
});

// Security middleware
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "https:"],
    },
  },
  hsts: {
    maxAge: 31536000,
    includeSubDomains: true,
    preload: true
  }
}));

app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 1000, // limit each IP to 1000 requests per windowMs
  message: {
    error: 'Too many requests',
    code: 'RATE_LIMIT_EXCEEDED'
  },
  standardHeaders: true,
  legacyHeaders: false
});

app.use(limiter);
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Request logging middleware
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    logger.info({
      method: req.method,
      url: req.url,
      status: res.statusCode,
      duration: `${duration}ms`,
      userAgent: req.get('User-Agent'),
      ip: req.ip
    });
  });
  next();
});

// Initialize services
const cacheService = new CacheService();
const registryService = new RegistryService();
const authService = new AuthService();
const resolverService = new ResolverService(registryService, cacheService, logger);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    version: process.env.npm_package_version || '1.0.0',
    uptime: process.uptime()
  });
});

// Well-known endpoint for resolver discovery
app.get('/.well-known/linkid-resolver', (req, res) => {
  res.status(200).json({
    resolver: {
      version: '1.0',
      endpoints: {
        resolve: `${req.protocol}://${req.get('host')}/resolve/{id}`,
        register: `${req.protocol}://${req.get('host')}/register`,
        update: `${req.protocol}://${req.get('host')}/resolve/{id}`,
        withdraw: `${req.protocol}://${req.get('host')}/resolve/{id}`
      },
      capabilities: [
        'content-negotiation',
        'caching',
        'authentication',
        'rate-limiting',
        'federation'
      ],
      supportedFormats: ['application/linkid+json', 'application/json'],
      rateLimits: {
        perMinute: 1000,
        perHour: 10000
      }
    }
  });
});

// Validation middleware
const validateLinkID = param('id')
  .isLength({ min: 32, max: 64 })
  .matches(/^[A-Za-z0-9._~-]+$/)
  .withMessage('Invalid LinkID format');

const validateQuery = [
  query('format').optional().isIn(['pdf', 'html', 'json', 'xml']),
  query('lang').optional().isLength({ min: 2, max: 5 }),
  query('version').optional().isNumeric(),
  query('at').optional().isISO8601()
];

// Main resolution endpoint
app.get('/resolve/:id',
  validateLinkID,
  validateQuery,
  async (req, res) => {
    try {
      // Check validation errors
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          error: 'Invalid request parameters',
          details: errors.array()
        });
      }

      const { id } = req.params;
      const requestParams = {
        format: req.query.format,
        language: req.query.lang,
        version: req.query.version,
        timestamp: req.query.at,
        acceptHeader: req.get('Accept') || '*/*',
        acceptLanguage: req.get('Accept-Language'),
        preferRedirect: !req.query.metadata
      };

      logger.info(`Resolving LinkID: ${id}`, { requestParams });

      const result = await resolverService.resolve(id, requestParams);

      if (result.type === 'redirect') {
        res.set({
          'Location': result.uri,
          'Cache-Control': `max-age=${result.cacheTTL || 3600}`,
          'Link': `<https://w3id.org/linkid/${id}>; rel="canonical"`,
          'X-LinkID-Resolver': req.get('host'),
          'X-LinkID-Quality': result.quality?.toString() || '1.0'
        });
        return res.status(result.permanent ? 301 : 302).send();
      }

      if (result.type === 'metadata') {
        res.set({
          'Content-Type': 'application/linkid+json',
          'Cache-Control': `max-age=${result.cacheTTL || 1800}`,
          'ETag': result.etag,
          'Vary': 'Accept, Accept-Language'
        });
        return res.status(200).json(result.data);
      }

      // Should not reach here
      res.status(500).json({ error: 'Internal resolver error' });

    } catch (error) {
      logger.error('Resolution error', {
        linkId: req.params.id,
        error: error.message,
        stack: error.stack
      });

      if (error.code === 'LINKID_NOT_FOUND') {
        return res.status(404).json({
          error: 'LinkID not found',
          linkId: req.params.id
        });
      }

      if (error.code === 'LINKID_WITHDRAWN') {
        return res.status(410).json({
          error: 'LinkID withdrawn',
          linkId: req.params.id,
          tombstone: error.tombstone
        });
      }

      res.status(500).json({
        error: 'Internal server error',
        requestId: req.id
      });
    }
  }
);

// Registration endpoint (requires authentication)
app.post('/register',
  authService.authenticate,
  [
    body('targetUri').isURL().withMessage('Valid target URI required'),
    body('mediaType').optional().isString(),
    body('metadata').optional().isObject()
  ],
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          error: 'Invalid request data',
          details: errors.array()
        });
      }

      const { targetUri, mediaType, metadata } = req.body;
      const issuer = req.user.sub; // From JWT token

      const result = await resolverService.register({
        targetUri,
        mediaType,
        metadata,
        issuer
      });

      logger.info(`Registered new LinkID: ${result.id}`, { issuer });

      res.status(201).json({
        id: result.id,
        uri: `https://w3id.org/linkid/${result.id}`,
        created: result.created
      });

    } catch (error) {
      logger.error('Registration error', {
        error: error.message,
        user: req.user?.sub
      });
      res.status(500).json({ error: 'Registration failed' });
    }
  }
);

// Update endpoint (requires authentication)
app.put('/resolve/:id',
  validateLinkID,
  authService.authenticate,
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          error: 'Invalid request parameters',
          details: errors.array()
        });
      }

      const { id } = req.params;
      const updates = req.body;
      const userId = req.user.sub;

      await resolverService.update(id, updates, userId);

      logger.info(`Updated LinkID: ${id}`, { userId });

      res.status(200).json({
        id,
        updated: new Date().toISOString()
      });

    } catch (error) {
      logger.error('Update error', {
        linkId: req.params.id,
        error: error.message,
        user: req.user?.sub
      });

      if (error.code === 'LINKID_NOT_FOUND') {
        return res.status(404).json({ error: 'LinkID not found' });
      }

      if (error.code === 'UNAUTHORIZED') {
        return res.status(403).json({ error: 'Not authorized to update this LinkID' });
      }

      res.status(500).json({ error: 'Update failed' });
    }
  }
);

// Withdrawal endpoint (requires authentication)
app.delete('/resolve/:id',
  validateLinkID,
  authService.authenticate,
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({
          error: 'Invalid request parameters',
          details: errors.array()
        });
      }

      const { id } = req.params;
      const { reason, tombstone } = req.body;
      const userId = req.user.sub;

      await resolverService.withdraw(id, { reason, tombstone }, userId);

      logger.info(`Withdrew LinkID: ${id}`, { userId, reason });

      res.status(200).json({
        id,
        withdrawn: new Date().toISOString(),
        reason
      });

    } catch (error) {
      logger.error('Withdrawal error', {
        linkId: req.params.id,
        error: error.message,
        user: req.user?.sub
      });

      if (error.code === 'LINKID_NOT_FOUND') {
        return res.status(404).json({ error: 'LinkID not found' });
      }

      if (error.code === 'UNAUTHORIZED') {
        return res.status(403).json({ error: 'Not authorized to withdraw this LinkID' });
      }

      res.status(500).json({ error: 'Withdrawal failed' });
    }
  }
);

// Error handling middleware
app.use((error, req, res, next) => {
  logger.error('Unhandled error', {
    error: error.message,
    stack: error.stack,
    url: req.url,
    method: req.method
  });

  res.status(500).json({
    error: 'Internal server error',
    timestamp: new Date().toISOString()
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Endpoint not found',
    path: req.path
  });
});

// Start server
const server = app.listen(PORT, () => {
  logger.info(`LinkID Resolver listening on port ${PORT}`);
  logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM received, shutting down gracefully');
  server.close(() => {
    logger.info('Process terminated');
    process.exit(0);
  });
});

module.exports = app;