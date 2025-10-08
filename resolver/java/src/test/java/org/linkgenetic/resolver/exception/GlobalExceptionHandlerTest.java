package org.linkgenetic.resolver.exception;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/throw")
    static class ThrowingController {
        @GetMapping("/invalid")
        public ResponseEntity<Void> invalid() { throw new InvalidLinkIdFormatException("bad"); }
        @GetMapping("/notfound")
        public ResponseEntity<Void> notFound() { throw new LinkIdNotFoundException("missing"); }
        @GetMapping("/withdrawn")
        public ResponseEntity<Void> withdrawn() { throw new LinkIdWithdrawnException("gone", null); }
        @GetMapping("/generic")
        public ResponseEntity<Void> generic() { throw new RuntimeException("boom"); }
    }

    @Test
    void invalid_returns400() throws Exception {
        mockMvc.perform(get("/throw/invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid LinkID Format"))
            .andExpect(jsonPath("$.linkId").value("bad"));
    }

    @Test
    void notFound_returns404() throws Exception {
        mockMvc.perform(get("/throw/notfound"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("LinkID Not Found"))
            .andExpect(jsonPath("$.linkId").value("missing"));
    }

    @Test
    void withdrawn_returns410() throws Exception {
        mockMvc.perform(get("/throw/withdrawn"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error").value("LinkID Withdrawn"))
            .andExpect(jsonPath("$.linkId").value("gone"));
    }

    @Test
    void generic_returns500() throws Exception {
        mockMvc.perform(get("/throw/generic"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
