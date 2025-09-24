package org.linkgenetic.linkid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sample LinkID client for Java demonstrating resolver interactions.
 */
public final class LinkIdClient {

    private final URI resolverUri;
    private final String apiKey;
    private final Duration timeout;
    private final int retries;
    private final boolean caching;
    private final Duration cacheTtl;
    private final Map<String, String> defaultHeaders;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CacheEntry> cache;

    private LinkIdClient(Builder builder) {
        this.resolverUri = builder.resolverUri;
        this.apiKey = builder.apiKey;
        this.timeout = builder.timeout;
        this.retries = builder.retries;
        this.caching = builder.caching;
        this.cacheTtl = builder.cacheTtl;
        this.defaultHeaders = Map.copyOf(builder.headers);
        this.httpClient = builder.httpClient != null
            ? builder.httpClient
            : HttpClient.newBuilder()
                .connectTimeout(builder.timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = builder.objectMapper != null ? builder.objectMapper : new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }

    public LinkIdClient() {
        this(new Builder());
    }

    public static Builder builder() {
        return new Builder();
    }

    public ResolutionResult resolve(String linkId) {
        return resolve(linkId, ResolveOptions.builder().build());
    }

    public ResolutionResult resolve(String linkId, ResolveOptions options) {
        Objects.requireNonNull(linkId, "linkId");
        validateLinkId(linkId);
        ResolveOptions effectiveOptions = options != null ? options : ResolveOptions.builder().build();

        String cacheKey = cacheKey(linkId, effectiveOptions);
        ResolutionResult cachedResult = getFromCache(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        URI requestUri = buildResolveUri(linkId, effectiveOptions);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(requestUri)
            .GET()
            .timeout(timeout);
        applyHeaders(requestBuilder, effectiveOptions.headers());

        HttpResponse<String> response = send(requestBuilder.build());

        int status = response.statusCode();
        ResolutionResult result;
        if (status == 200) {
            JsonNode payload = parseJson(response.body());
            result = new MetadataResolution(
                linkId,
                payload,
                resolverUsed(response),
                false
            );
        } else if (isRedirect(status)) {
            String location = response.headers().firstValue("Location")
                .or(() -> response.headers().firstValue("location"))
                .orElseThrow(() -> new LinkIdException("Redirect response missing Location header"));

            OptionalDouble quality = response.headers()
                .firstValue("X-LinkID-Quality")
                .map(LinkIdClient::parseQuality)
                .orElse(OptionalDouble.empty());

            result = new RedirectResolution(
                linkId,
                location,
                resolverUsed(response),
                false,
                quality
            );
        } else {
            handleError(response, linkId);
            throw new LinkIdException("Unhandled response status: " + status);
        }

        if (caching) {
            long ttl = determineCacheTtl(response.headers().firstValue("Cache-Control"));
            cache.put(cacheKey, new CacheEntry(result, Instant.now().plusSeconds(ttl)));
        }

        return result;
    }

    public JsonNode register(RegistrationRequest request) {
        Objects.requireNonNull(request, "request");
        validateRegistrationTarget(request.targetUri());

        URI requestUri = resolverUri.resolve("/register");
        ObjectNode body = request.toJson(objectMapper);

        HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)));
        applyHeaders(builder, Map.of());

        HttpResponse<String> response = send(builder.build());
        int status = response.statusCode();
        if (status != 200 && status != 201) {
            handleError(response, null);
        }
        return parseJson(response.body());
    }

    public void update(String linkId, ObjectNode request) {
        Objects.requireNonNull(request, "request");
        validateLinkId(linkId);

        URI requestUri = resolverUri.resolve("/resolve/" + linkId);

        HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(writeJson(request)));
        applyHeaders(builder, Map.of());

        HttpResponse<String> response = send(builder.build());
        if (!isSuccess(response.statusCode())) {
            handleError(response, linkId);
        }
        if (caching) {
            cache.clear();
        }
    }

    public void withdraw(String linkId, ObjectNode request) {
        Objects.requireNonNull(request, "request");
        validateLinkId(linkId);

        URI requestUri = resolverUri.resolve("/resolve/" + linkId);

        HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .method("DELETE", HttpRequest.BodyPublishers.ofString(writeJson(request)));
        applyHeaders(builder, Map.of());

        HttpResponse<String> response = send(builder.build());
        if (!isSuccess(response.statusCode())) {
            handleError(response, linkId);
        }
        if (caching) {
            cache.clear();
        }
    }

    public ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    // -----------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------
    private HttpResponse<String> send(HttpRequest request) {
        IOException lastIo = null;
        InterruptedException lastInterrupted = null;

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException ex) {
                lastIo = ex;
            } catch (InterruptedException ex) {
                lastInterrupted = ex;
                Thread.currentThread().interrupt();
                break;
            }

            if (attempt < retries) {
                try {
                    Thread.sleep((long) Math.pow(2, attempt - 1) * 1000L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    lastInterrupted = interruptedException;
                    break;
                }
            }
        }

        if (lastInterrupted != null) {
            throw new NetworkException("Request interrupted", lastInterrupted);
        }
        throw new NetworkException("Request failed after " + retries + " attempts", lastIo);
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> extra) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "LinkID-Java-Sample/1.0");
        headers.put("Accept", "application/linkid+json, application/json, */*");
        headers.putAll(defaultHeaders);
        if (extra != null) {
            headers.putAll(extra);
        }
        if (apiKey != null && !apiKey.isBlank()) {
            headers.put("Authorization", "ApiKey " + apiKey);
        }
        headers.forEach(builder::header);
    }

    private String cacheKey(String linkId, ResolveOptions options) {
        StringBuilder builder = new StringBuilder("linkid:").append(linkId);
        options.appendTo(builder);
        return builder.toString();
    }

    private ResolutionResult getFromCache(String key) {
        if (!caching) {
            return null;
        }
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(key);
            return null;
        }
        return copyWithCached(entry.result());
    }

    private ResolutionResult copyWithCached(ResolutionResult result) {
        if (result instanceof RedirectResolution redirect) {
            return new RedirectResolution(
                redirect.linkId(),
                redirect.uri(),
                redirect.resolver(),
                true,
                redirect.quality()
            );
        }
        if (result instanceof MetadataResolution metadata) {
            return new MetadataResolution(
                metadata.linkId(),
                metadata.metadata(),
                metadata.resolver(),
                true
            );
        }
        return result;
    }

    private URI buildResolveUri(String linkId, ResolveOptions options) {
        StringBuilder path = new StringBuilder("/resolve/").append(linkId);
        String query = options.toQueryString();
        if (!query.isBlank()) {
            path.append('?').append(query);
        }
        return resolverUri.resolve(path.toString());
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private void handleError(HttpResponse<String> response, String linkId) {
        JsonNode payload = parseJson(response.body());
        String message = Optional.ofNullable(payload.get("error"))
            .map(JsonNode::asText)
            .or(() -> Optional.ofNullable(payload.get("message")).map(JsonNode::asText))
            .orElse("HTTP " + response.statusCode());

        switch (response.statusCode()) {
            case 404 -> throw new NotFoundException(linkId != null ? linkId : "unknown", message);
            case 410 -> throw new WithdrawnException(
                linkId != null ? linkId : "unknown",
                message,
                payload.get("tombstone")
            );
            case 400, 422 -> throw new ValidationException(message);
            case 401 -> throw new LinkIdException(message, "UNAUTHORIZED");
            case 403 -> throw new LinkIdException(message, "FORBIDDEN");
            case 429 -> throw new LinkIdException(message, "RATE_LIMITED");
            default -> throw new LinkIdException(message, "HTTP_ERROR");
        }
    }

    private JsonNode parseJson(String body) {
        try {
            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("error", body != null ? body : "UNKNOWN");
            return fallback;
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new LinkIdException("Failed to serialize request body", ex);
        }
    }

    private static OptionalDouble parseQuality(String value) {
        try {
            return OptionalDouble.of(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return OptionalDouble.empty();
        }
    }

    private long determineCacheTtl(Optional<String> cacheControl) {
        if (cacheControl.isEmpty()) {
            return cacheTtl.toSeconds();
        }
        String header = cacheControl.get();
        int index = header.indexOf("max-age=");
        if (index >= 0) {
            int start = index + 8;
            int end = header.indexOf(',', start);
            String value = end >= 0 ? header.substring(start, end) : header.substring(start);
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return cacheTtl.toSeconds();
    }

    private String resolverUsed(HttpResponse<?> response) {
        return response.headers().firstValue("X-LinkID-Resolver").orElse(resolverUri.toString());
    }

    private static void validateLinkId(String linkId) {
        if (linkId == null || linkId.isBlank()) {
            throw new ValidationException("LinkID must be a non-empty string");
        }
        if (linkId.length() < 32 || linkId.length() > 64) {
            throw new ValidationException("LinkID must be between 32 and 64 characters");
        }
        if (!linkId.matches("[A-Za-z0-9._~-]+")) {
            throw new ValidationException("LinkID contains invalid characters");
        }
    }

    private static void validateRegistrationTarget(String targetUri) {
        if (targetUri == null || targetUri.isBlank()) {
            throw new ValidationException("'targetUri' is required");
        }
        if (!isLikelyUrl(targetUri)) {
            throw new ValidationException("'targetUri' must be an absolute HTTP(S) URL");
        }
    }

    private static boolean isLikelyUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    // -----------------------------------------------------------------
    // Nested types
    // -----------------------------------------------------------------

    private record CacheEntry(ResolutionResult result, Instant expiresAt) { }

    public sealed interface ResolutionResult permits RedirectResolution, MetadataResolution {
        String linkId();
        String resolver();
        boolean cached();
    }

    public record RedirectResolution(
        String linkId,
        String uri,
        String resolver,
        boolean cached,
        OptionalDouble quality
    ) implements ResolutionResult { }

    public record MetadataResolution(
        String linkId,
        JsonNode metadata,
        String resolver,
        boolean cached
    ) implements ResolutionResult { }

    public static final class ResolveOptions {
        private final String format;
        private final String language;
        private final Integer version;
        private final String timestamp;
        private final boolean metadata;
        private final Map<String, String> headers;

        private ResolveOptions(Builder builder) {
            this.format = builder.format;
            this.language = builder.language;
            this.version = builder.version;
            this.timestamp = builder.timestamp;
            this.metadata = builder.metadata;
            this.headers = Map.copyOf(builder.headers);
        }

        public static Builder builder() {
            return new Builder();
        }

        public Map<String, String> headers() {
            return headers;
        }

        private void appendTo(StringBuilder builder) {
            builder.append("|fmt=").append(format != null ? format : "");
            builder.append("|lang=").append(language != null ? language : "");
            builder.append("|ver=").append(version != null ? version : "");
            builder.append("|at=").append(timestamp != null ? timestamp : "");
            builder.append("|meta=").append(metadata ? "1" : "0");
        }

        private String toQueryString() {
            StringBuilder query = new StringBuilder();
            appendQuery(query, "format", format);
            appendQuery(query, "lang", language);
            if (version != null) {
                appendQuery(query, "version", version.toString());
            }
            appendQuery(query, "at", timestamp);
            if (metadata) {
                appendQuery(query, "metadata", "true");
            }
            if (query.length() > 0 && query.charAt(0) == '&') {
                return query.substring(1);
            }
            return query.toString();
        }

        private static void appendQuery(StringBuilder builder, String key, String value) {
            if (value == null || value.isBlank()) {
                return;
            }
            builder.append('&')
                .append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }

        public static final class Builder {
            private String format;
            private String language;
            private Integer version;
            private String timestamp;
            private boolean metadata;
            private final Map<String, String> headers = new HashMap<>();

            public Builder format(String value) {
                this.format = value;
                return this;
            }

            public Builder language(String value) {
                this.language = value;
                return this;
            }

            public Builder version(Integer value) {
                this.version = value;
                return this;
            }

            public Builder timestamp(String value) {
                this.timestamp = value;
                return this;
            }

            public Builder metadata(boolean value) {
                this.metadata = value;
                return this;
            }

            public Builder header(String name, String value) {
                this.headers.put(name, value);
                return this;
            }

            public ResolveOptions build() {
                return new ResolveOptions(this);
            }
        }
    }

    public static final class RegistrationRequest {
        private final String targetUri;
        private final String mediaType;
        private final String language;
        private final ObjectNode metadata;

        private RegistrationRequest(Builder builder) {
            this.targetUri = builder.targetUri;
            this.mediaType = builder.mediaType;
            this.language = builder.language;
            this.metadata = builder.metadata;
        }

        public String targetUri() {
            return targetUri;
        }

        ObjectNode toJson(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("targetUri", targetUri);
            if (mediaType != null && !mediaType.isBlank()) {
                node.put("mediaType", mediaType);
            }
            if (language != null && !language.isBlank()) {
                node.put("language", language);
            }
            if (metadata != null) {
                node.set("metadata", metadata);
            }
            return node;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String targetUri;
            private String mediaType;
            private String language;
            private ObjectNode metadata;

            public Builder targetUri(String value) {
                this.targetUri = value;
                return this;
            }

            public Builder mediaType(String value) {
                this.mediaType = value;
                return this;
            }

            public Builder language(String value) {
                this.language = value;
                return this;
            }

            public Builder metadata(ObjectNode value) {
                this.metadata = value;
                return this;
            }

            public RegistrationRequest build() {
                validateRegistrationTarget(targetUri);
                return new RegistrationRequest(this);
            }
        }
    }

    public static final class Builder {
        private URI resolverUri = URI.create("https://resolver.linkid.org");
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(10);
        private int retries = 3;
        private boolean caching = true;
        private Duration cacheTtl = Duration.ofHours(1);
        private final Map<String, String> headers = new HashMap<>();
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        public Builder resolverUri(URI uri) {
            this.resolverUri = Objects.requireNonNull(uri, "resolverUri");
            return this;
        }

        public Builder apiKey(String value) {
            this.apiKey = value;
            return this;
        }

        public Builder timeout(Duration value) {
            this.timeout = Objects.requireNonNull(value, "timeout");
            return this;
        }

        public Builder retries(int value) {
            this.retries = Math.max(1, value);
            return this;
        }

        public Builder caching(boolean value) {
            this.caching = value;
            return this;
        }

        public Builder cacheTtl(Duration value) {
            this.cacheTtl = Objects.requireNonNull(value, "cacheTtl");
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder httpClient(HttpClient client) {
            this.httpClient = client;
            return this;
        }

        public Builder objectMapper(ObjectMapper mapper) {
            this.objectMapper = mapper;
            return this;
        }

        public LinkIdClient build() {
            return new LinkIdClient(this);
        }
    }

    // -----------------------------------------------------------------
    // Exception hierarchy
    // -----------------------------------------------------------------

    public static class LinkIdException extends RuntimeException {
        private final String code;

        public LinkIdException(String message) {
            this(message, null, null);
        }

        public LinkIdException(String message, Throwable cause) {
            this(message, null, cause);
        }

        public LinkIdException(String message, String code) {
            this(message, code, null);
        }

        public LinkIdException(String message, String code, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    public static final class NetworkException extends LinkIdException {
        public NetworkException(String message, Throwable cause) {
            super(message, "NETWORK_ERROR", cause);
        }
    }

    public static final class ValidationException extends LinkIdException {
        public ValidationException(String message) {
            super(message, "VALIDATION_ERROR");
        }
    }

    public static final class NotFoundException extends LinkIdException {
        private final String linkId;

        public NotFoundException(String linkId, String message) {
            super(message, "NOT_FOUND");
            this.linkId = linkId;
        }

        public String linkId() {
            return linkId;
        }
    }

    public static final class WithdrawnException extends LinkIdException {
        private final String linkId;
        private final JsonNode tombstone;

        public WithdrawnException(String linkId, String message, JsonNode tombstone) {
            super(message, "WITHDRAWN");
            this.linkId = linkId;
            this.tombstone = tombstone;
        }

        public String linkId() {
            return linkId;
        }

        public JsonNode tombstone() {
            return tombstone;
        }
    }
}
