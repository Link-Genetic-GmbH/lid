package org.linkgenetic.resolver.model;

import java.time.Instant;
import java.util.Map;

public class ResolutionRecord {

    private String uri;
    private String status;
    private String mediaType;
    private String language;
    private Double quality;
    private Instant validFrom;
    private Instant validUntil;
    private Instant lastModified;
    private Checksum checksum;
    private Long size;
    private Map<String, Object> metadata;

    public ResolutionRecord() {}

    public ResolutionRecord(String uri, String status, String mediaType) {
        this.uri = uri;
        this.status = status;
        this.mediaType = mediaType;
        this.quality = 1.0;
        this.language = "en";
        this.validFrom = Instant.now();
        this.lastModified = Instant.now();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Double getQuality() {
        return quality;
    }

    public void setQuality(Double quality) {
        this.quality = quality;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public Checksum getChecksum() {
        return checksum;
    }

    public void setChecksum(Checksum checksum) {
        this.checksum = checksum;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Checksum {
        private String algorithm;
        private String value;

        public Checksum() {}

        public Checksum(String algorithm, String value) {
            this.algorithm = algorithm;
            this.value = value;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}