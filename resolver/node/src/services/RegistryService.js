/**
 * RegistryService - Database operations for LinkID records
 */

const mongoose = require('mongoose');

// LinkID Record Schema
const linkIdRecordSchema = new mongoose.Schema({
  id: {
    type: String,
    required: true,
    unique: true,
    index: true,
    match: /^[A-Za-z0-9._~-]{32,64}$/
  },
  status: {
    type: String,
    required: true,
    enum: ['active', 'withdrawn', 'pending'],
    default: 'active'
  },
  created: {
    type: Date,
    required: true,
    default: Date.now
  },
  updated: {
    type: Date,
    required: true,
    default: Date.now
  },
  issuer: {
    type: String,
    required: true,
    index: true
  },
  records: [{
    uri: {
      type: String,
      required: true,
      validate: {
        validator: function(v) {
          try {
            new URL(v);
            return true;
          } catch {
            return false;
          }
        },
        message: 'Invalid URI format'
      }
    },
    status: {
      type: String,
      required: true,
      enum: ['active', 'inactive', 'deprecated'],
      default: 'active'
    },
    mediaType: {
      type: String,
      default: 'text/html'
    },
    language: {
      type: String,
      default: 'en'
    },
    quality: {
      type: Number,
      min: 0,
      max: 1,
      default: 1.0
    },
    validFrom: {
      type: Date,
      default: Date.now
    },
    validUntil: {
      type: Date,
      default: null
    },
    checksum: {
      algorithm: {
        type: String,
        enum: ['sha256', 'sha3-256', 'blake2b']
      },
      value: String
    },
    size: {
      type: Number,
      min: 0
    },
    lastModified: {
      type: Date,
      default: Date.now
    },
    metadata: {
      type: mongoose.Schema.Types.Mixed,
      default: {}
    }
  }],
  alternates: [{
    scheme: {
      type: String,
      required: true,
      enum: ['doi', 'ark', 'handle', 'isbn', 'issn']
    },
    identifier: {
      type: String,
      required: true
    }
  }],
  policy: {
    cacheTTL: {
      type: Number,
      default: 3600,
      min: 60,
      max: 86400
    },
    allowUpdates: {
      type: Boolean,
      default: true
    },
    fallbackResolvers: [String]
  },
  tombstone: {
    withdrawnAt: Date,
    reason: String,
    contact: String,
    alternativeLocation: String
  },
  signatures: [{
    algorithm: {
      type: String,
      enum: ['eddsa', 'ecdsa', 'rsa-pss']
    },
    publicKey: String,
    signature: String,
    timestamp: {
      type: Date,
      default: Date.now
    }
  }],
  telemetry: {
    resolutions: {
      type: Number,
      default: 0
    },
    lastResolved: Date,
    popularityScore: {
      type: Number,
      default: 0
    }
  }
}, {
  timestamps: true,
  collection: 'linkid_records'
});

// Indexes for performance
linkIdRecordSchema.index({ 'records.uri': 1 });
linkIdRecordSchema.index({ 'records.checksum.value': 1 });
linkIdRecordSchema.index({ status: 1, updated: -1 });
linkIdRecordSchema.index({ issuer: 1, created: -1 });

// Middleware to update the 'updated' field
linkIdRecordSchema.pre('save', function(next) {
  this.updated = new Date();
  next();
});

const LinkIdRecord = mongoose.model('LinkIdRecord', linkIdRecordSchema);

class RegistryService {
  constructor() {
    this.isConnected = false;
    this.connect();
  }

  async connect() {
    try {
      const mongoUrl = process.env.MONGODB_URL || 'mongodb://localhost:27017/linkid';
      await mongoose.connect(mongoUrl, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        maxPoolSize: 10,
        serverSelectionTimeoutMS: 5000,
        socketTimeoutMS: 45000
      });

      this.isConnected = true;
      console.log('Connected to MongoDB registry');
    } catch (error) {
      console.error('MongoDB connection error:', error.message);
      this.isConnected = false;
    }
  }

  /**
   * Get LinkID record by ID
   * @param {string} id - LinkID identifier
   * @returns {Promise<Object|null>} LinkID record or null
   */
  async get(id) {
    try {
      const record = await LinkIdRecord.findOne({ id }).lean();
      if (record) {
        // Increment resolution counter
        await LinkIdRecord.updateOne(
          { id },
          {
            $inc: { 'telemetry.resolutions': 1 },
            $set: { 'telemetry.lastResolved': new Date() }
          }
        );
      }
      return record;
    } catch (error) {
      console.error('Registry get error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Create new LinkID record
   * @param {Object} record - LinkID record data
   * @returns {Promise<Object>} Created record
   */
  async create(record) {
    try {
      const linkIdRecord = new LinkIdRecord(record);
      const saved = await linkIdRecord.save();
      return saved.toObject();
    } catch (error) {
      if (error.code === 11000) { // Duplicate key error
        throw new Error('LinkID already exists');
      }
      console.error('Registry create error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Update existing LinkID record
   * @param {string} id - LinkID identifier
   * @param {Object} updates - Update data
   * @returns {Promise<Object>} Updated record
   */
  async update(id, updates) {
    try {
      const updated = await LinkIdRecord.findOneAndUpdate(
        { id },
        {
          ...updates,
          updated: new Date()
        },
        {
          new: true,
          runValidators: true
        }
      ).lean();

      if (!updated) {
        throw new Error('LinkID not found');
      }

      return updated;
    } catch (error) {
      console.error('Registry update error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Delete LinkID record
   * @param {string} id - LinkID identifier
   * @returns {Promise<boolean>} True if deleted
   */
  async delete(id) {
    try {
      const result = await LinkIdRecord.deleteOne({ id });
      return result.deletedCount > 0;
    } catch (error) {
      console.error('Registry delete error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Search LinkID records by various criteria
   * @param {Object} criteria - Search criteria
   * @param {Object} options - Search options (limit, offset, sort)
   * @returns {Promise<Array>} Array of matching records
   */
  async search(criteria, options = {}) {
    try {
      const {
        limit = 50,
        offset = 0,
        sort = { updated: -1 }
      } = options;

      const query = this.buildSearchQuery(criteria);

      const records = await LinkIdRecord
        .find(query)
        .sort(sort)
        .skip(offset)
        .limit(limit)
        .lean();

      return records;
    } catch (error) {
      console.error('Registry search error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Get records by issuer
   * @param {string} issuer - Issuer identifier
   * @param {Object} options - Query options
   * @returns {Promise<Array>} Array of records
   */
  async getByIssuer(issuer, options = {}) {
    try {
      const {
        limit = 100,
        offset = 0,
        status = 'active'
      } = options;

      const records = await LinkIdRecord
        .find({ issuer, status })
        .sort({ updated: -1 })
        .skip(offset)
        .limit(limit)
        .lean();

      return records;
    } catch (error) {
      console.error('Registry getByIssuer error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Find records by content hash
   * @param {string} algorithm - Hash algorithm
   * @param {string} value - Hash value
   * @returns {Promise<Array>} Array of matching records
   */
  async findByChecksum(algorithm, value) {
    try {
      const records = await LinkIdRecord
        .find({
          'records.checksum.algorithm': algorithm,
          'records.checksum.value': value,
          status: 'active'
        })
        .lean();

      return records;
    } catch (error) {
      console.error('Registry findByChecksum error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Get registry statistics
   * @returns {Promise<Object>} Registry statistics
   */
  async getStats() {
    try {
      const stats = await LinkIdRecord.aggregate([
        {
          $group: {
            _id: '$status',
            count: { $sum: 1 }
          }
        }
      ]);

      const totalResolutions = await LinkIdRecord.aggregate([
        {
          $group: {
            _id: null,
            total: { $sum: '$telemetry.resolutions' }
          }
        }
      ]);

      return {
        records: stats.reduce((acc, stat) => {
          acc[stat._id] = stat.count;
          return acc;
        }, {}),
        totalResolutions: totalResolutions[0]?.total || 0,
        lastUpdated: new Date().toISOString()
      };
    } catch (error) {
      console.error('Registry getStats error:', error.message);
      throw new Error('Registry operation failed');
    }
  }

  /**
   * Build MongoDB query from search criteria
   * @param {Object} criteria - Search criteria
   * @returns {Object} MongoDB query object
   */
  buildSearchQuery(criteria) {
    const query = {};

    if (criteria.status) {
      query.status = criteria.status;
    }

    if (criteria.issuer) {
      query.issuer = criteria.issuer;
    }

    if (criteria.mediaType) {
      query['records.mediaType'] = { $regex: criteria.mediaType, $options: 'i' };
    }

    if (criteria.uri) {
      query['records.uri'] = { $regex: criteria.uri, $options: 'i' };
    }

    if (criteria.createdAfter) {
      query.created = { $gte: new Date(criteria.createdAfter) };
    }

    if (criteria.createdBefore) {
      query.created = { ...query.created, $lte: new Date(criteria.createdBefore) };
    }

    return query;
  }

  /**
   * Cleanup and close database connection
   */
  async cleanup() {
    if (this.isConnected) {
      await mongoose.connection.close();
      this.isConnected = false;
    }
  }
}

module.exports = RegistryService;