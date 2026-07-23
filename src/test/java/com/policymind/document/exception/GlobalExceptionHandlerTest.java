package com.policymind.document.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {

        @GetMapping("/throw/document-processing")
        public String throwDocumentProcessing() {
            throw new DocumentProcessingException("Document is invalid.");
        }

        @GetMapping("/throw/illegal-argument")
        public String throwIllegalArgument() {
            throw new IllegalArgumentException("Bad input.");
        }

        @GetMapping("/throw/illegal-state")
        public String throwIllegalState() {
            throw new IllegalStateException("Provider temporarily unavailable.");
        }

        @GetMapping("/throw/data-integrity")
        public String throwDataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key");
        }

        @GetMapping("/throw/unexpected")
        public String throwUnexpected() {
            throw new RuntimeException("boom");
        }
    }

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void documentProcessingException_mapsToBadRequest() throws Exception {
        mvc().perform(get("/throw/document-processing"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.error").value("Document is invalid."));
    }

    @Test
    public void illegalArgumentException_mapsToBadRequest() throws Exception {
        mvc().perform(get("/throw/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad input."));
    }

    @Test
    public void illegalStateException_mapsToServiceUnavailable() throws Exception {
        mvc().perform(get("/throw/illegal-state"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Provider temporarily unavailable."));
    }

    @Test
    public void dataIntegrityViolation_mapsToConflictWithoutLeakingDetails() throws Exception {
        mvc().perform(get("/throw/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("The request conflicts with existing data."));
    }

    @Test
    public void unexpectedException_mapsToInternalServerErrorWithoutLeakingDetails() throws Exception {
        mvc().perform(get("/throw/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again."));
    }
}
