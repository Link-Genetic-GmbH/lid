package org.linkgenetic.resolver.controller;

import org.junit.jupiter.api.Test;
import org.linkgenetic.resolver.config.SecurityConfig;
import org.linkgenetic.resolver.model.LinkIdRecord;
import org.linkgenetic.resolver.model.ResolutionPolicy;
import org.linkgenetic.resolver.model.ResolutionResult;
import org.linkgenetic.resolver.service.RegistryService;
import org.linkgenetic.resolver.service.ResolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResolverController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class ResolverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResolverService resolverService;

    @MockBean
    private RegistryService registryService;

    @Test
    void getResolve_redirectsWithHeaders() throws Exception {
        Map<String, String> headers = Map.of("X-LinkID-Resolver", "linkid-resolver-java");
        when(resolverService.resolve(eq("abc"), anyMap())).thenReturn(
            new ResolutionResult.RedirectResult("https://target.example", 1.0, headers)
        );

        mockMvc.perform(get("/resolve/{id}", "abc"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://target.example"))
            .andExpect(header().string("Link", containsString("rel=\"canonical\"")))
            .andExpect(header().string("X-LinkID-Resolver", "linkid-resolver-java"));
    }

    @Test
    void getResolve_metadataReturnsJson() throws Exception {
        LinkIdRecord rec = new LinkIdRecord("abc", "active", Instant.now(), "issuer");
        when(resolverService.resolve(eq("abc"), anyMap())).thenReturn(
            new ResolutionResult.MetadataResult(rec)
        );

        mockMvc.perform(get("/resolve/{id}", "abc").param("format", "metadata"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/linkid+json"))
            .andExpect(jsonPath("$.id").value("abc"))
            .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    @WithMockUser
    void postRegister_requiresAuthAndReturnsCreated() throws Exception {
        when(registryService.register(any())).thenReturn(
            new org.linkgenetic.resolver.model.RegistrationResponse("id", "active", Instant.now(), "https://w3id.org/linkid/id")
        );

        String body = "{\n" +
                "  \"targetUri\": \"https://example.com\",\n" +
                "  \"mediaType\": \"text/html\",\n" +
                "  \"language\": \"en\",\n" +
                "  \"issuer\": \"example.org\"\n" +
                "}";

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("id"))
            .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    @WithMockUser
    void putUpdate_passesIssuerFromToken() throws Exception {
        LinkIdRecord updated = new LinkIdRecord("abc", "active", Instant.now(), "example.org");
        when(registryService.update(eq("abc"), anyList(), eq("example.org"))).thenReturn(updated);

        String body = "{\n" +
                "  \"records\": [ { \"uri\": \"https://x\", \"status\": \"active\", \"mediaType\": \"text/html\" } ]\n" +
                "}";

        mockMvc.perform(put("/resolve/{id}", "abc")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("abc"));
    }

    @Test
    @WithMockUser
    void deleteWithdraw_returnsOk() throws Exception {
        String body = "{\n" +
                "  \"reason\": \"obsolete\",\n" +
                "  \"contact\": \"admin@example.org\"\n" +
                "}";

        mockMvc.perform(delete("/resolve/{id}", "abc")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("withdrawn"))
            .andExpect(jsonPath("$.id").value("abc"));
    }

    @Test
    void getLinkId_returnsRecord() throws Exception {
        LinkIdRecord rec = new LinkIdRecord("abc", "active", Instant.now(), "issuer");
        when(registryService.get("abc")).thenReturn(rec);

        mockMvc.perform(get("/linkid/{id}", "abc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("abc"));
    }

    @Test
    void getByIssuer_returnsList() throws Exception {
        when(registryService.getByIssuer("issuer")).thenReturn(List.of());
        mockMvc.perform(get("/issuer/{issuer}/linkids", "issuer"))
            .andExpect(status().isOk());
    }

    @Test
    void getStats_returnsOk() throws Exception {
        when(registryService.getStats()).thenReturn(new RegistryService.RegistryStats(1L, 1L, 0L));
        mockMvc.perform(get("/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void wellKnown_returnsDiscovery() throws Exception {
        mockMvc.perform(get("/.well-known/linkid-resolver"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(header().string("Cache-Control", containsString("max-age")))
            .andExpect(jsonPath("$.resolver").value("LinkID Resolver Java"));
    }
}
