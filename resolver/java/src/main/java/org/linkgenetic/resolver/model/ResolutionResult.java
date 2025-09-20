package org.linkgenetic.resolver.model;

import java.util.Map;

public abstract class ResolutionResult {

    public enum Type {
        REDIRECT, METADATA
    }

    public abstract Type getType();

    public static class RedirectResult extends ResolutionResult {
        private final String uri;
        private final Double quality;
        private final Map<String, String> headers;

        public RedirectResult(String uri, Double quality, Map<String, String> headers) {
            this.uri = uri;
            this.quality = quality;
            this.headers = headers;
        }

        @Override
        public Type getType() {
            return Type.REDIRECT;
        }

        public String getUri() {
            return uri;
        }

        public Double getQuality() {
            return quality;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    public static class MetadataResult extends ResolutionResult {
        private final LinkIdRecord record;

        public MetadataResult(LinkIdRecord record) {
            this.record = record;
        }

        @Override
        public Type getType() {
            return Type.METADATA;
        }

        public LinkIdRecord getRecord() {
            return record;
        }
    }
}