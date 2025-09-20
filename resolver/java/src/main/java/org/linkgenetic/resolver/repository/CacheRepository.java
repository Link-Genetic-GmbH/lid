package org.linkgenetic.resolver.repository;

import org.linkgenetic.resolver.model.ResolutionResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class CacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_PREFIX = "linkid:";

    public CacheRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(String key, ResolutionResult result, Duration ttl) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.opsForValue().set(cacheKey, result, ttl);
    }

    public Optional<ResolutionResult> get(String key) {
        String cacheKey = CACHE_PREFIX + key;
        Object result = redisTemplate.opsForValue().get(cacheKey);
        return Optional.ofNullable((ResolutionResult) result);
    }

    public void evict(String key) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
    }

    public void evictPattern(String pattern) {
        String cachePattern = CACHE_PREFIX + pattern;
        redisTemplate.delete(redisTemplate.keys(cachePattern));
    }

    public boolean exists(String key) {
        String cacheKey = CACHE_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }

    public void incrementCounter(String key) {
        String counterKey = CACHE_PREFIX + "counter:" + key;
        redisTemplate.opsForValue().increment(counterKey);
    }

    public Long getCounter(String key) {
        String counterKey = CACHE_PREFIX + "counter:" + key;
        Object value = redisTemplate.opsForValue().get(counterKey);
        return value != null ? (Long) value : 0L;
    }
}