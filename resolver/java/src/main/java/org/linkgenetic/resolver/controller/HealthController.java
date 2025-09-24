package org.linkgenetic.resolver.controller;

import org.linkgenetic.resolver.service.CacheService;
import org.linkgenetic.resolver.service.RegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final RegistryService registryService;
    private final CacheService cacheService;

    public HealthController(RegistryService registryService, CacheService cacheService) {
        this.registryService = registryService;
        this.cacheService = cacheService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "LinkID Resolver");
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "LinkID Resolver");
        health.put("version", "1.0.0");

        try {
            RegistryService.RegistryStats registryStats = registryService.getStats();
            Map<String, Object> registry = new HashMap<>();
            registry.put("status", "UP");
            registry.put("totalRecords", registryStats.getTotal());
            registry.put("activeRecords", registryStats.getActive());
            registry.put("withdrawnRecords", registryStats.getWithdrawn());
            health.put("registry", registry);
        } catch (Exception e) {
            Map<String, Object> registry = new HashMap<>();
            registry.put("status", "DOWN");
            registry.put("error", e.getMessage());
            health.put("registry", registry);
        }

        try {
            CacheService.CacheStats cacheStats = cacheService.getStats();
            Map<String, Object> cache = new HashMap<>();
            cache.put("status", "UP");
            cache.put("hits", cacheStats.getHits());
            cache.put("misses", cacheStats.getMisses());
            cache.put("hitRate", cacheStats.getHitRate());
            health.put("cache", cache);
        } catch (Exception e) {
            Map<String, Object> cache = new HashMap<>();
            cache.put("status", "DOWN");
            cache.put("error", e.getMessage());
            health.put("cache", cache);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> readiness = new HashMap<>();

        boolean isReady = true;
        StringBuilder issues = new StringBuilder();

        try {
            registryService.getStats();
        } catch (Exception e) {
            isReady = false;
            issues.append("Registry not accessible; ");
        }

        try {
            cacheService.getStats();
        } catch (Exception e) {
            isReady = false;
            issues.append("Cache not accessible; ");
        }

        readiness.put("status", isReady ? "READY" : "NOT_READY");
        readiness.put("timestamp", Instant.now());

        if (!isReady) {
            readiness.put("issues", issues.toString());
        }

        return ResponseEntity.ok(readiness);
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", Instant.now());

        return ResponseEntity.ok(liveness);
    }
}