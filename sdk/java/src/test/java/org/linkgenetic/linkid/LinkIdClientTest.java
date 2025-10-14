package org.linkgenetic.linkid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LinkIdClientTest {

    private HttpServer server;
    private URI baseUri;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(null);
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void resolve_redirects_are_returned_with_quality_and_headers() throws Exception {
        String linkId = validLinkId();
        String target = "https://example.com/abc";
        server.createContext("/resolve/" + linkId, exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            exchange.getResponseHeaders().add("Location", target);
            exchange.getResponseHeaders().add("X-LinkID-Quality", "0.87");
            exchange.getResponseHeaders().add("X-LinkID-Resolver", "http://resolver.local");
            sendResponse(exchange, 302, "");
        });

        LinkIdClient client = defaultClient();
        LinkIdClient.ResolutionResult res = client.resolve(linkId);

        assertTrue(res instanceof LinkIdClient.RedirectResolution);
        LinkIdClient.RedirectResolution rr = (LinkIdClient.RedirectResolution) res;
        assertEquals(linkId, rr.linkId());
        assertEquals(target, rr.uri());
        assertEquals("http://resolver.local", rr.resolver());
        assertFalse(rr.cached());
        assertEquals(OptionalDouble.of(0.87).getAsDouble(), rr.quality().orElseThrow());
    }

    @Test
    void resolve_metadata_200_returns_payload_and_resolver_header() throws Exception {
        String linkId = validLinkId();
        ObjectNode body = mapper.createObjectNode().put("name", "ok");
        server.createContext("/resolve/" + linkId, exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            exchange.getResponseHeaders().add("X-LinkID-Resolver", "http://resolver.meta");
            exchange.getResponseHeaders().add("Cache-Control", "max-age=3600");
            sendJson(exchange, 200, body);
        });

        LinkIdClient client = defaultClient();
        LinkIdClient.ResolutionResult res = client.resolve(linkId, LinkIdClient.ResolveOptions.builder().metadata(true).build());

        assertTrue(res instanceof LinkIdClient.MetadataResolution);
        LinkIdClient.MetadataResolution mr = (LinkIdClient.MetadataResolution) res;
        assertEquals("ok", mr.metadata().get("name").asText());
        assertEquals("http://resolver.meta", mr.resolver());
        assertFalse(mr.cached());
    }

    @Test
    void resolve_uses_cache_and_sets_cached_flag() throws Exception {
        String linkId = validLinkId();
        AtomicInteger hits = new AtomicInteger();
        ObjectNode body = mapper.createObjectNode().put("v", 1);
        server.createContext("/resolve/" + linkId, exchange -> {
            hits.incrementAndGet();
            exchange.getResponseHeaders().add("Cache-Control", "max-age=3600");
            sendJson(exchange, 200, body);
        });

        LinkIdClient client = defaultClient();
        LinkIdClient.ResolutionResult res1 = client.resolve(linkId);
        LinkIdClient.ResolutionResult res2 = client.resolve(linkId);

        assertTrue(res2 instanceof LinkIdClient.MetadataResolution);
        assertEquals(1, hits.get(), "second call should be served from cache");
        assertFalse(((LinkIdClient.MetadataResolution) res1).cached());
        assertTrue(((LinkIdClient.MetadataResolution) res2).cached());
    }

    @Test
    void update_clears_cache_and_next_resolve_fetches_again() throws Exception {
        String linkId = validLinkId();
        AtomicInteger resolveHits = new AtomicInteger();
        ObjectNode body1 = mapper.createObjectNode().put("v", 1);
        ObjectNode body2 = mapper.createObjectNode().put("v", 2);

        server.createContext("/resolve/" + linkId, exchange -> {
            if (Objects.equals(exchange.getRequestMethod(), "PUT")) {
                assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
                sendResponse(exchange, 204, "");
                return;
            }
            resolveHits.incrementAndGet();
            exchange.getResponseHeaders().add("Cache-Control", "max-age=3600");
            if (resolveHits.get() == 1) {
                sendJson(exchange, 200, body1);
            } else {
                sendJson(exchange, 200, body2);
            }
        });

        LinkIdClient client = defaultClient();
        LinkIdClient.ResolutionResult r1 = client.resolve(linkId);
        assertEquals(1, resolveHits.get());

        ObjectNode updateReq = new ObjectMapper().createObjectNode().put("note", "x");
        client.update(linkId, updateReq);

        LinkIdClient.ResolutionResult r2 = client.resolve(linkId);
        assertEquals(2, resolveHits.get(), "Cache should be cleared by update");
        assertEquals(2, ((LinkIdClient.MetadataResolution) r2).metadata().get("v").asInt());
    }

    @Test
    void withdraw_clears_cache_and_next_resolve_fetches_again() throws Exception {
        String linkId = validLinkId();
        AtomicInteger resolveHits = new AtomicInteger();
        ObjectNode body1 = mapper.createObjectNode().put("v", 1);
        ObjectNode body2 = mapper.createObjectNode().put("v", 2);

        server.createContext("/resolve/" + linkId, exchange -> {
            if (Objects.equals(exchange.getRequestMethod(), "DELETE")) {
                assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
                sendResponse(exchange, 204, "");
                return;
            }
            resolveHits.incrementAndGet();
            exchange.getResponseHeaders().add("Cache-Control", "max-age=3600");
            if (resolveHits.get() == 1) {
                sendJson(exchange, 200, body1);
            } else {
                sendJson(exchange, 200, body2);
            }
        });

        LinkIdClient client = defaultClient();
        client.resolve(linkId);
        client.withdraw(linkId, mapper.createObjectNode().put("reason", "x"));
        LinkIdClient.ResolutionResult r2 = client.resolve(linkId);
        assertEquals(2, resolveHits.get());
        assertEquals(2, ((LinkIdClient.MetadataResolution) r2).metadata().get("v").asInt());
    }

    @Test
    void register_posts_body_and_returns_json() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        server.createContext("/register", exchange -> {
            hits.incrementAndGet();
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
            String payload = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode parsed = mapper.readTree(payload);
            assertEquals("https://example.com/x", parsed.get("targetUri").asText());
            sendJson(exchange, 201, mapper.createObjectNode().put("ok", true));
        });

        LinkIdClient client = clientWithApiKey("k123");
        LinkIdClient.RegistrationRequest req = LinkIdClient.RegistrationRequest.builder()
            .targetUri("https://example.com/x")
            .mediaType("text/html")
            .language("en")
            .metadata(new ObjectMapper().createObjectNode().put("k", "v"))
            .build();

        JsonNode resp = client.register(req);
        assertTrue(resp.get("ok").asBoolean());
        assertEquals(1, hits.get());
    }

    @Test
    void headers_include_accept_user_agent_and_authorization() throws Exception {
        String linkId = validLinkId();
        server.createContext("/resolve/" + linkId, exchange -> {
            assertEquals("LinkID-Java-Sample/1.0", exchange.getRequestHeaders().getFirst("User-Agent"));
            assertEquals("application/linkid+json, application/json, */*", exchange.getRequestHeaders().getFirst("Accept"));
            assertEquals("ApiKey test123", exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange, 200, mapper.createObjectNode().put("ok", true));
        });

        LinkIdClient client = LinkIdClient.builder()
            .resolverUri(baseUri)
            .apiKey("test123")
            .timeout(Duration.ofSeconds(2))
            .retries(1)
            .build();

        client.resolve(linkId);
    }

    @Test
    void error_mapping_to_specific_exceptions() throws Exception {
        String linkId = validLinkId();
        server.createContext("/resolve/" + linkId, exchange -> {
            String header = Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Code")).orElse("500");
            int code = Integer.parseInt(header);
            ObjectNode body = mapper.createObjectNode();
            if (code == 410) {
                body.put("message", "withdrawn");
                body.set("tombstone", mapper.createObjectNode().put("why", "gone"));
            } else if (code == 404) {
                body.put("error", "not-found");
            } else {
                body.put("message", "err");
            }
            sendJson(exchange, code, body);
        });

        LinkIdClient client = defaultClient();

        assertThrows(LinkIdClient.NotFoundException.class, () -> client.resolve(linkId, withCode(404)));
        LinkIdClient.WithdrawnException w = assertThrows(LinkIdClient.WithdrawnException.class, () -> client.resolve(linkId, withCode(410)));
        assertEquals("gone", w.tombstone().get("why").asText());
        assertThrows(LinkIdClient.ValidationException.class, () -> client.resolve(linkId, withCode(400)));
        assertThrows(LinkIdClient.ValidationException.class, () -> client.resolve(linkId, withCode(422)));
        LinkIdClient.LinkIdException e401 = assertThrows(LinkIdClient.LinkIdException.class, () -> client.resolve(linkId, withCode(401)));
        assertEquals("UNAUTHORIZED", e401.code());
        LinkIdClient.LinkIdException e403 = assertThrows(LinkIdClient.LinkIdException.class, () -> client.resolve(linkId, withCode(403)));
        assertEquals("FORBIDDEN", e403.code());
        LinkIdClient.LinkIdException e429 = assertThrows(LinkIdClient.LinkIdException.class, () -> client.resolve(linkId, withCode(429)));
        assertEquals("RATE_LIMITED", e429.code());
    }

    @Test
    void validation_errors_for_linkid_and_registration_target() {
        LinkIdClient client = defaultClient();
        assertThrows(LinkIdClient.ValidationException.class, () -> client.resolve(""));
        assertThrows(LinkIdClient.ValidationException.class, () -> client.resolve("short"));
        assertThrows(LinkIdClient.ValidationException.class, () -> client.resolve(" ".repeat(33)));

        assertThrows(LinkIdClient.ValidationException.class, () -> LinkIdClient.RegistrationRequest.builder()
            .targetUri("ftp://invalid")
            .build());
    }

    @Test
    void resolve_options_are_encoded_in_query() throws Exception {
        String linkId = validLinkId();
        AtomicInteger hits = new AtomicInteger();
        server.createContext("/resolve/" + linkId, exchange -> {
            hits.incrementAndGet();
            String query = Optional.ofNullable(exchange.getRequestURI().getRawQuery()).orElse("");
            assertTrue(query.contains("format=json"));
            assertTrue(query.contains("lang=en"));
            assertTrue(query.contains("version=2"));
            assertTrue(query.contains("at=2024-01-01T00%3A00%3A00Z"));
            assertTrue(query.contains("metadata=true"));
            sendJson(exchange, 200, mapper.createObjectNode().put("ok", true));
        });

        LinkIdClient client = defaultClient();
        LinkIdClient.ResolveOptions options = LinkIdClient.ResolveOptions.builder()
            .format("json")
            .language("en")
            .version(2)
            .timestamp("2024-01-01T00:00:00Z")
            .metadata(true)
            .build();
        client.resolve(linkId, options);
        assertEquals(1, hits.get());
    }

    private LinkIdClient.ResolveOptions withCode(int code) {
        return LinkIdClient.ResolveOptions.builder().header("X-Code", Integer.toString(code)).build();
    }

    private LinkIdClient defaultClient() {
        return LinkIdClient.builder()
            .resolverUri(baseUri)
            .timeout(Duration.ofSeconds(2))
            .retries(1)
            .build();
    }

    private LinkIdClient clientWithApiKey(String key) {
        return LinkIdClient.builder()
            .resolverUri(baseUri)
            .apiKey(key)
            .timeout(Duration.ofSeconds(2))
            .retries(1)
            .build();
    }

    private static void sendJson(HttpExchange exchange, int status, JsonNode node) throws IOException {
        byte[] bytes = node.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String validLinkId() {
        return "A".repeat(32);
    }
}


