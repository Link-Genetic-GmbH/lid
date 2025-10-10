package org.linkgenetic.resolver.controller;

import org.junit.jupiter.api.Test;
import org.linkgenetic.resolver.config.SecurityConfig;
import org.linkgenetic.resolver.service.CacheService;
import org.linkgenetic.resolver.service.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistryService registryService;

    @MockBean
    private CacheService cacheService;

    @Test
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void detailedHealth_includesSections() throws Exception {
        when(registryService.getStats()).thenReturn(new RegistryService.RegistryStats(1L, 1L, 0L));
        when(cacheService.getStats()).thenReturn(new CacheService.CacheStats(1L, 1L));

        mockMvc.perform(get("/health/detailed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.registry.status").value("UP"))
            .andExpect(jsonPath("$.cache.status").value("UP"));
    }

    @Test
    void live_and_ready_endpointsReturnOk() throws Exception {
        when(registryService.getStats()).thenReturn(new RegistryService.RegistryStats(0L, 0L, 0L));
        when(cacheService.getStats()).thenReturn(new CacheService.CacheStats(0L, 0L));

        mockMvc.perform(get("/health/live")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ALIVE"));

        mockMvc.perform(get("/health/ready")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
    }
}
