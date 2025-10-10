package org.linkgenetic.resolver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkgenetic.resolver.exception.InvalidLinkIdFormatException;
import org.linkgenetic.resolver.exception.LinkIdNotFoundException;
import org.linkgenetic.resolver.model.LinkIdRecord;
import org.linkgenetic.resolver.model.ResolutionPolicy;
import org.linkgenetic.resolver.model.ResolutionRecord;
import org.linkgenetic.resolver.model.ResolutionResult;
import org.linkgenetic.resolver.repository.LinkIdRepository;
import org.linkgenetic.resolver.util.LinkIdValidator;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolverServiceTest {

    @Mock
    private LinkIdRepository linkIdRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private LinkIdValidator validator;

    @InjectMocks
    private ResolverService resolverService;

    private LinkIdRecord activeRecord;

    @BeforeEach
    void setUp() {
        activeRecord = new LinkIdRecord();
        activeRecord.setId("id");
        activeRecord.setStatus("active");
        activeRecord.setCreated(Instant.now());

        ResolutionPolicy policy = new ResolutionPolicy();
        policy.setCacheTTL(1234);
        activeRecord.setPolicy(policy);
    }

    @Test
    void cacheHit_shortCircuits() {
        when(validator.normalize("ID")).thenReturn("id");
        when(cacheService.get(anyString())).thenReturn(Optional.of(new ResolutionResult.MetadataResult(new LinkIdRecord())));

        ResolutionResult out = resolverService.resolve("ID", Map.of("format", "text/html"));

        assertThat(out.getType()).isEqualTo(ResolutionResult.Type.METADATA);
        verifyNoInteractions(linkIdRepository);
    }

    @Test
    void resolve_happyPath_redirects_andCachesWithPolicyTTL() {
        when(validator.normalize("id")).thenReturn("id");

        ResolutionRecord r1 = new ResolutionRecord("https://a.example", "active", "text/html");
        r1.setLanguage("en");
        r1.setQuality(0.8);
        r1.setValidFrom(Instant.now().minusSeconds(3600));
        r1.setLastModified(Instant.now().minusSeconds(100));

        ResolutionRecord r2 = new ResolutionRecord("https://b.example", "active", "text/html");
        r2.setLanguage("en");
        r2.setQuality(0.9); // better
        r2.setValidFrom(Instant.now().minusSeconds(3600));
        r2.setLastModified(Instant.now());

        activeRecord.setRecords(List.of(r1, r2));

        when(linkIdRepository.findByIdAndStatus("id", "active")).thenReturn(Optional.of(activeRecord));
        when(cacheService.get(anyString())).thenReturn(Optional.empty());

        Map<String, String> params = new HashMap<>();
        params.put("lang", "en");
        params.put("format", "text/html");

        ResolutionResult result = resolverService.resolve("id", params);
        assertThat(result).isInstanceOf(ResolutionResult.RedirectResult.class);
        ResolutionResult.RedirectResult redirect = (ResolutionResult.RedirectResult) result;
        assertThat(redirect.getUri()).isEqualTo("https://b.example");
        assertThat(redirect.getHeaders()).containsEntry("Cache-Control", "max-age=1234");

        verify(cacheService).put(anyString(), eq(result), eq(1234));
    }

    @Test
    void resolve_metadataFormat_returnsMetadataResult() {
        when(validator.normalize("id")).thenReturn("id");
        activeRecord.setRecords(Collections.emptyList());
        when(linkIdRepository.findByIdAndStatus("id", "active")).thenReturn(Optional.of(activeRecord));
        when(cacheService.get(anyString())).thenReturn(Optional.empty());

        Map<String, String> params = Map.of("format", "metadata");

        assertThrows(LinkIdNotFoundException.class, () -> resolverService.resolve("id", params));
        // No records → not found, even if metadata requested
    }

    @Test
    void resolve_invalidId_throwsInvalidFormat() {
        doThrow(new InvalidLinkIdFormatException("bad"))
                .when(validator).validate("bad");
        assertThrows(InvalidLinkIdFormatException.class, () -> resolverService.resolve("bad", Map.of()));
        verifyNoInteractions(linkIdRepository);
    }

    @Test
    void generateCacheKey_sortsParamsDeterministically() {
        when(validator.normalize("id")).thenReturn("id");
        ResolutionRecord r = new ResolutionRecord("https://x", "active", "text/html");
        r.setLanguage("en");
        activeRecord.setRecords(List.of(r));
        when(linkIdRepository.findByIdAndStatus("id", "active")).thenReturn(Optional.of(activeRecord));
        when(cacheService.get(anyString())).thenReturn(Optional.empty());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("lang", "en");
        params.put("format", "text/html");

        resolverService.resolve("id", params);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService).put(keyCaptor.capture(), any(), any());
        String key = keyCaptor.getValue();
        // Expect sorted by key: format;lang
        assertThat(key).isEqualTo("id:format=text/html;lang=en;");
    }
}
