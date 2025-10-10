package org.linkgenetic.resolver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkgenetic.resolver.exception.LinkIdNotFoundException;
import org.linkgenetic.resolver.model.*;
import org.linkgenetic.resolver.repository.LinkIdRepository;
import org.linkgenetic.resolver.util.LinkIdGenerator;
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
class RegistryServiceTest {

    @Mock
    private LinkIdRepository repository;

    @Mock
    private LinkIdGenerator generator;

    @Mock
    private LinkIdValidator validator;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private RegistryService registryService;

    private RegistrationRequest request;

    @BeforeEach
    void setup() {
        request = new RegistrationRequest();
        request.setTargetUri("https://example.com");
        request.setMediaType("text/html");
        request.setLanguage("en");
        request.setIssuer("issuer.example");
        Map<String, Object> md = new HashMap<>();
        md.put("k", "v");
        request.setMetadata(md);
    }

    @Test
    void register_persistsRecordWithDefaults() {
        when(generator.generateUUID()).thenReturn("idididididididididididididididid");

        RegistrationResponse resp = registryService.register(request);

        assertThat(resp.getId()).isEqualTo("idididididididididididididididid");
        assertThat(resp.getStatus()).isEqualTo("active");
        assertThat(resp.getResolverUri()).contains("/" + resp.getId());

        ArgumentCaptor<LinkIdRecord> captor = ArgumentCaptor.forClass(LinkIdRecord.class);
        verify(repository).save(captor.capture());
        LinkIdRecord saved = captor.getValue();
        assertThat(saved.getPolicy()).isNotNull();
        assertThat(saved.getPolicy().getCacheTTL()).isEqualTo(3600);
        assertThat(saved.getRecords()).hasSize(1);
        assertThat(saved.getRecords().get(0).getUri()).isEqualTo("https://example.com");
        assertThat(saved.getRecords().get(0).getMediaType()).isEqualTo("text/html");
        assertThat(saved.getRecords().get(0).getLanguage()).isEqualTo("en");
        assertThat(saved.getIssuer()).isEqualTo("issuer.example");
        assertThat(saved.getMetadata()).isNull(); // metadata is set on record, not top-level
        assertThat(saved.getRecords().get(0).getMetadata()).containsEntry("k", "v");
    }

    @Test
    void update_checksIssuer_updatesAndEvictsCache() {
        when(validator.normalize("ID")).thenReturn("id");
        LinkIdRecord existing = new LinkIdRecord("id", "active", Instant.now(), "issuer.example");
        existing.setRecords(new ArrayList<>());
        when(repository.findByIdAndStatus("id", "active")).thenReturn(Optional.of(existing));

        List<ResolutionRecord> newRecords = List.of(new ResolutionRecord("https://new", "active", "text/html"));

        LinkIdRecord updated = registryService.update("ID", newRecords, "issuer.example");

        assertThat(updated.getRecords()).hasSize(1);
        verify(cacheService).evictPattern("id*");
        verify(repository).save(existing);
    }

    @Test
    void update_wrongIssuer_throwsSecurityException() {
        when(validator.normalize("ID")).thenReturn("id");
        LinkIdRecord existing = new LinkIdRecord("id", "active", Instant.now(), "issuer.example");
        when(repository.findByIdAndStatus("id", "active")).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class, () -> registryService.update("ID", List.of(), "other.issuer"));
    }

    @Test
    void withdraw_setsWithdrawnAndEvicts() {
        when(validator.normalize("ID")).thenReturn("id");
        LinkIdRecord existing = new LinkIdRecord("id", "active", Instant.now(), "issuer.example");
        when(repository.findByIdAndStatus("id", "active")).thenReturn(Optional.of(existing));

        registryService.withdraw("ID", "reason", "contact", "issuer.example");

        assertThat(existing.getStatus()).isEqualTo("withdrawn");
        assertThat(existing.getTombstone()).isNotNull();
        verify(cacheService).evictPattern("id*");
        verify(repository).save(existing);
    }

    @Test
    void get_validatesAndReturns() {
        when(validator.normalize("ID")).thenReturn("id");
        LinkIdRecord existing = new LinkIdRecord("id", "active", Instant.now(), "issuer");
        when(repository.findById("id")).thenReturn(Optional.of(existing));

        LinkIdRecord out = registryService.get("ID");
        assertThat(out).isSameAs(existing);
    }

    @Test
    void get_missing_throwsNotFound() {
        when(validator.normalize("ID")).thenReturn("id");
        when(repository.findById("id")).thenReturn(Optional.empty());
        assertThrows(LinkIdNotFoundException.class, () -> registryService.get("ID"));
    }

    @Test
    void exists_returnsFalseForInvalid_thenTrueForValid() {
        when(validator.isValid("bad")).thenReturn(false);
        assertThat(registryService.exists("bad")).isFalse();

        when(validator.isValid("GOOD")).thenReturn(true);
        when(validator.normalize("GOOD")).thenReturn("good");
        when(repository.existsById("good")).thenReturn(true);
        assertThat(registryService.exists("GOOD")).isTrue();
    }

    @Test
    void getStats_aggregatesCounts() {
        when(repository.countByStatus("active")).thenReturn(10L);
        when(repository.countByStatus("withdrawn")).thenReturn(2L);
        RegistryService.RegistryStats stats = registryService.getStats();
        assertThat(stats.getTotal()).isEqualTo(12L);
        assertThat(stats.getActive()).isEqualTo(10L);
        assertThat(stats.getWithdrawn()).isEqualTo(2L);
    }
}
