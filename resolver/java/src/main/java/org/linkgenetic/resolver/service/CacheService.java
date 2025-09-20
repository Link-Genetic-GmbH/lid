package org.linkgenetic.resolver.service;

import org.linkgenetic.resolver.model.ResolutionResult;
import org.linkgenetic.resolver.repository.CacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final CacheRepository cacheRepository;

    public CacheService(CacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    public void put(String key, ResolutionResult result, Integer ttlSeconds) {
        try {
            Duration ttl = Duration.ofSeconds(ttlSeconds != null ? ttlSeconds : 3600);
            cacheRepository.put(key, result, ttl);
            logger.debug("Cached result for key: {} with TTL: {} seconds", key, ttl.getSeconds());
        } catch (Exception e) {
            logger.warn("Failed to cache result for key: {}", key, e);
        }
    }

    public Optional<ResolutionResult> get(String key) {
        try {
            Optional<ResolutionResult> result = cacheRepository.get(key);
            if (result.isPresent()) {
                logger.debug("Cache hit for key: {}", key);
                incrementHitCounter();
            } else {
                logger.debug("Cache miss for key: {}", key);
                incrementMissCounter();
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to retrieve from cache for key: {}", key, e);
            incrementMissCounter();
            return Optional.empty();
        }
    }

    public void evict(String key) {
        try {
            cacheRepository.evict(key);
            logger.debug("Evicted cache entry for key: {}", key);
        } catch (Exception e) {
            logger.warn("Failed to evict cache entry for key: {}", key, e);
        }
    }

    public void evictPattern(String pattern) {
        try {
            cacheRepository.evictPattern(pattern);
            logger.debug("Evicted cache entries matching pattern: {}", pattern);
        } catch (Exception e) {
            logger.warn("Failed to evict cache entries for pattern: {}", pattern, e);
        }
    }

    public void evictAll() {
        try {
            cacheRepository.evictPattern("*");
            logger.info("Evicted all cache entries");
        } catch (Exception e) {
            logger.warn("Failed to evict all cache entries", e);
        }
    }

    public boolean exists(String key) {
        try {
            return cacheRepository.exists(key);
        } catch (Exception e) {
            logger.warn("Failed to check cache existence for key: {}", key, e);
            return false;
        }
    }

    public CacheStats getStats() {
        try {
            Long hits = cacheRepository.getCounter("hits");
            Long misses = cacheRepository.getCounter("misses");
            return new CacheStats(hits, misses);
        } catch (Exception e) {
            logger.warn("Failed to retrieve cache stats", e);
            return new CacheStats(0L, 0L);
        }
    }

    private void incrementHitCounter() {
        try {
            cacheRepository.incrementCounter("hits");
        } catch (Exception e) {
            logger.debug("Failed to increment hit counter", e);
        }
    }

    private void incrementMissCounter() {
        try {
            cacheRepository.incrementCounter("misses");
        } catch (Exception e) {
            logger.debug("Failed to increment miss counter", e);
        }
    }

    public static class CacheStats {
        private final Long hits;
        private final Long misses;

        public CacheStats(Long hits, Long misses) {
            this.hits = hits != null ? hits : 0L;
            this.misses = misses != null ? misses : 0L;
        }

        public Long getHits() {
            return hits;
        }

        public Long getMisses() {
            return misses;
        }

        public Long getTotal() {
            return hits + misses;
        }

        public Double getHitRate() {
            Long total = getTotal();
            return total > 0 ? (double) hits / total : 0.0;
        }
    }
}