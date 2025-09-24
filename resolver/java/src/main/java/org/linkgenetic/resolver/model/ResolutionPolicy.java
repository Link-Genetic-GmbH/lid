package org.linkgenetic.resolver.model;

import java.util.List;

public class ResolutionPolicy {

    private String preferredFormat;
    private Integer cacheTTL;
    private List<String> fallbackResolvers;
    private Double qualityThreshold;
    private Integer maxAge;
    private Boolean allowExpiredRecords;

    public ResolutionPolicy() {
        this.cacheTTL = 3600;
        this.qualityThreshold = 0.0;
        this.allowExpiredRecords = false;
    }

    public String getPreferredFormat() {
        return preferredFormat;
    }

    public void setPreferredFormat(String preferredFormat) {
        this.preferredFormat = preferredFormat;
    }

    public Integer getCacheTTL() {
        return cacheTTL;
    }

    public void setCacheTTL(Integer cacheTTL) {
        this.cacheTTL = cacheTTL;
    }

    public List<String> getFallbackResolvers() {
        return fallbackResolvers;
    }

    public void setFallbackResolvers(List<String> fallbackResolvers) {
        this.fallbackResolvers = fallbackResolvers;
    }

    public Double getQualityThreshold() {
        return qualityThreshold;
    }

    public void setQualityThreshold(Double qualityThreshold) {
        this.qualityThreshold = qualityThreshold;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public Boolean getAllowExpiredRecords() {
        return allowExpiredRecords;
    }

    public void setAllowExpiredRecords(Boolean allowExpiredRecords) {
        this.allowExpiredRecords = allowExpiredRecords;
    }
}