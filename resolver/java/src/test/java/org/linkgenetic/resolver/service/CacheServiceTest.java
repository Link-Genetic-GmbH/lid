package org.linkgenetic.resolver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkgenetic.resolver.model.LinkIdRecord;
import org.linkgenetic.resolver.model.ResolutionResult;
import org.linkgenetic.resolver.repository.CacheRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheRepository cacheRepository;

    @InjectMocks
    private CacheService cacheService;

    @Test
    void put_passesDuration() {
        ResolutionResult value = new ResolutionResult.MetadataResult(new LinkIdRecord());
        cacheService.put("k", value, 42);
        verify(cacheRepository).put(eq("k"), eq(value), eq(Duration.ofSeconds(42)));
    }

    @Test
    void get_hitIncrementsHitsAndReturnsValue() {
        ResolutionResult value = new ResolutionResult.MetadataResult(new LinkIdRecord());
        when(cacheRepository.get("k")).thenReturn(Optional.of(value));

        Optional<ResolutionResult> out = cacheService.get("k");
        assertThat(out).containsSame(value);
        verify(cacheRepository).incrementCounter("hits");
    }

    @Test
    void get_missIncrementsMisses() {
        when(cacheRepository.get("k")).thenReturn(Optional.empty());
        Optional<ResolutionResult> out = cacheService.get("k");
        assertThat(out).isEmpty();
        verify(cacheRepository).incrementCounter("misses");
    }

    @Test
    void get_exceptionReturnsEmptyAndIncrementsMisses() {
        when(cacheRepository.get("k")).thenThrow(new RuntimeException("x"));
        Optional<ResolutionResult> out = cacheService.get("k");
        assertThat(out).isEmpty();
        verify(cacheRepository).incrementCounter("misses");
    }

    @Test
    void evict_evictPattern_exists_delegates() {
        cacheService.evict("k");
        verify(cacheRepository).evict("k");

        cacheService.evictPattern("p*");
        verify(cacheRepository).evictPattern("p*");

        when(cacheRepository.exists("k")).thenReturn(true);
        assertThat(cacheService.exists("k")).isTrue();
    }

    @Test
    void getStats_returnsCountersOrDefaultsOnError() {
        when(cacheRepository.getCounter("hits")).thenReturn(5L);
        when(cacheRepository.getCounter("misses")).thenReturn(5L);
        CacheService.CacheStats stats = cacheService.getStats();
        assertThat(stats.getTotal()).isEqualTo(10L);
        assertThat(stats.getHitRate()).isEqualTo(0.5);

        when(cacheRepository.getCounter(anyString())).thenThrow(new RuntimeException());
        CacheService.CacheStats stats2 = cacheService.getStats();
        assertThat(stats2.getTotal()).isEqualTo(0L);
    }
}
