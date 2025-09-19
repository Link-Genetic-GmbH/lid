/**
 * CacheService - Multi-tier caching for LinkID resolution
 */

const NodeCache = require('node-cache');
const redis = require('redis');

class CacheService {
  constructor() {
    // L1 Cache: In-memory (for hot data)
    this.memoryCache = new NodeCache({
      stdTTL: 600, // 10 minutes default
      checkperiod: 120, // Check for expired keys every 2 minutes
      useClones: false
    });

    // L2 Cache: Redis (for distributed caching)
    this.redisClient = null;
    this.initRedis();
  }

  async initRedis() {
    if (process.env.REDIS_URL) {
      try {
        this.redisClient = redis.createClient({
          url: process.env.REDIS_URL,
          retry_strategy: (options) => {
            if (options.error && options.error.code === 'ECONNREFUSED') {
              return new Error('Redis server connection refused');
            }
            if (options.total_retry_time > 1000 * 60 * 60) {
              return new Error('Retry time exhausted');
            }
            if (options.attempt > 10) {
              return undefined;
            }
            return Math.min(options.attempt * 100, 3000);
          }
        });

        await this.redisClient.connect();
        console.log('Connected to Redis cache');
      } catch (error) {
        console.warn('Redis connection failed, using memory cache only:', error.message);
        this.redisClient = null;
      }
    }
  }

  /**
   * Get value from cache (checks memory first, then Redis)
   * @param {string} key - Cache key
   * @returns {Promise<any>} Cached value or null
   */
  async get(key) {
    // Check L1 cache first
    const memoryValue = this.memoryCache.get(key);
    if (memoryValue !== undefined) {
      return memoryValue;
    }

    // Check L2 cache (Redis)
    if (this.redisClient) {
      try {
        const redisValue = await this.redisClient.get(key);
        if (redisValue) {
          const parsed = JSON.parse(redisValue);
          // Promote to L1 cache
          this.memoryCache.set(key, parsed, 300); // 5 minutes in memory
          return parsed;
        }
      } catch (error) {
        console.warn('Redis get error:', error.message);
      }
    }

    return null;
  }

  /**
   * Set value in cache (stores in both memory and Redis)
   * @param {string} key - Cache key
   * @param {any} value - Value to cache
   * @param {number} ttl - Time to live in seconds
   */
  async set(key, value, ttl = 3600) {
    // Store in L1 cache
    const memoryTTL = Math.min(ttl, 600); // Max 10 minutes in memory
    this.memoryCache.set(key, value, memoryTTL);

    // Store in L2 cache (Redis)
    if (this.redisClient) {
      try {
        await this.redisClient.setEx(key, ttl, JSON.stringify(value));
      } catch (error) {
        console.warn('Redis set error:', error.message);
      }
    }
  }

  /**
   * Delete specific key from cache
   * @param {string} key - Cache key to delete
   */
  async delete(key) {
    // Delete from L1 cache
    this.memoryCache.del(key);

    // Delete from L2 cache (Redis)
    if (this.redisClient) {
      try {
        await this.redisClient.del(key);
      } catch (error) {
        console.warn('Redis delete error:', error.message);
      }
    }
  }

  /**
   * Invalidate all keys matching a pattern
   * @param {string} pattern - Pattern to match (supports Redis patterns)
   */
  async invalidatePattern(pattern) {
    // For memory cache, we need to check all keys
    const memoryKeys = this.memoryCache.keys();
    for (const key of memoryKeys) {
      if (this.matchesPattern(key, pattern)) {
        this.memoryCache.del(key);
      }
    }

    // For Redis, use SCAN with pattern
    if (this.redisClient) {
      try {
        const keys = await this.redisClient.keys(pattern);
        if (keys.length > 0) {
          await this.redisClient.del(keys);
        }
      } catch (error) {
        console.warn('Redis pattern invalidation error:', error.message);
      }
    }
  }

  /**
   * Clear all cached data
   */
  async clear() {
    this.memoryCache.flushAll();

    if (this.redisClient) {
      try {
        await this.redisClient.flushDb();
      } catch (error) {
        console.warn('Redis clear error:', error.message);
      }
    }
  }

  /**
   * Get cache statistics
   * @returns {Object} Cache statistics
   */
  getStats() {
    const memoryStats = this.memoryCache.getStats();
    return {
      memory: {
        keys: memoryStats.keys,
        hits: memoryStats.hits,
        misses: memoryStats.misses,
        hitRate: memoryStats.hits / (memoryStats.hits + memoryStats.misses) || 0
      },
      redis: {
        connected: !!this.redisClient?.isOpen
      }
    };
  }

  /**
   * Simple pattern matching for cache key invalidation
   * @param {string} key - Key to test
   * @param {string} pattern - Pattern (supports * wildcard)
   * @returns {boolean} True if key matches pattern
   */
  matchesPattern(key, pattern) {
    const regexPattern = pattern
      .replace(/\*/g, '.*')
      .replace(/\?/g, '.');
    const regex = new RegExp(`^${regexPattern}$`);
    return regex.test(key);
  }

  /**
   * Cleanup resources
   */
  async cleanup() {
    if (this.redisClient) {
      await this.redisClient.quit();
    }
    this.memoryCache.close();
  }
}

module.exports = CacheService;