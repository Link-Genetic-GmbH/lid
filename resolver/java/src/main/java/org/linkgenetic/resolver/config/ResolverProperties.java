package org.linkgenetic.resolver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "linkid.resolver")
public class ResolverProperties {

    private String baseUrl = "https://w3id.org/linkid";
    private Integer defaultCacheTtl = 3600;
    private Double defaultQuality = 1.0;
    private String defaultLanguage = "en";
    private String defaultMediaType = "text/html";
    private List<String> fallbackResolvers;
    private RateLimit rateLimit = new RateLimit();
    private Cache cache = new Cache();
    private Security security = new Security();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getDefaultCacheTtl() {
        return defaultCacheTtl;
    }

    public void setDefaultCacheTtl(Integer defaultCacheTtl) {
        this.defaultCacheTtl = defaultCacheTtl;
    }

    public Double getDefaultQuality() {
        return defaultQuality;
    }

    public void setDefaultQuality(Double defaultQuality) {
        this.defaultQuality = defaultQuality;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    public List<String> getFallbackResolvers() {
        return fallbackResolvers;
    }

    public void setFallbackResolvers(List<String> fallbackResolvers) {
        this.fallbackResolvers = fallbackResolvers;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class RateLimit {
        private Boolean enabled = true;
        private Integer requestsPerMinute = 100;
        private Integer requestsPerHour = 1000;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(Integer requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public Integer getRequestsPerHour() {
            return requestsPerHour;
        }

        public void setRequestsPerHour(Integer requestsPerHour) {
            this.requestsPerHour = requestsPerHour;
        }
    }

    public static class Cache {
        private Boolean enabled = true;
        private Integer defaultTtl = 3600;
        private Integer maxEntries = 100000;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Integer defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Integer getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(Integer maxEntries) {
            this.maxEntries = maxEntries;
        }
    }

    public static class Security {
        private Boolean requireAuth = false;
        private String jwtSecret = "change-me-in-production";
        private Integer jwtExpirationHours = 24;

        public Boolean getRequireAuth() {
            return requireAuth;
        }

        public void setRequireAuth(Boolean requireAuth) {
            this.requireAuth = requireAuth;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public Integer getJwtExpirationHours() {
            return jwtExpirationHours;
        }

        public void setJwtExpirationHours(Integer jwtExpirationHours) {
            this.jwtExpirationHours = jwtExpirationHours;
        }
    }
}