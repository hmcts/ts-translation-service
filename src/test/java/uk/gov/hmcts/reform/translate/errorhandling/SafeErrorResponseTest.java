package uk.gov.hmcts.reform.translate.errorhandling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SafeErrorResponseTest {

    private static final String DIAGNOSTIC = "java.lang.SecretException at http://internal-service/private/file";
    private MockMvc mvc;
    private ProbeController controller;

    @BeforeEach
    void setUp() {
        controller = new ProbeController();
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler()).build();
    }

    @Test
    void hidesUnexpectedExceptionAndCause() throws Exception {
        controller.failure = new RuntimeException(DIAGNOSTIC, new IllegalStateException(DIAGNOSTIC));
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(not(containsString(DIAGNOSTIC))))
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void hidesTypeConversionDiagnostics() throws Exception {
        mvc.perform(get("/error-probe/id/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(not(containsString("java.util.UUID"))))
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void hidesMalformedJsonDiagnostics() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(not(containsString("JsonParseException"))))
            .andExpect(content().string(not(containsString("invalid"))))
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void retainsUnsupportedMediaAndMethodStatuses() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.TEXT_PLAIN).content("invalid"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.exception").doesNotExist());
        mvc.perform(get("/error-probe"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void validRequestsStillSucceed() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"ok\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.value").value("ok"));
    }

    @RestController
    static class ProbeController {
        private RuntimeException failure;

        @GetMapping("/error-probe/failure")
        public void fail() {
            throw failure;
        }

        @GetMapping("/error-probe/id/{id}")
        public UUID identifier(@PathVariable("id") UUID id) {
            return id;
        }

        @PostMapping(value = "/error-probe", consumes = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, String> echo(@RequestBody Map<String, String> value) {
            return value;
        }
    }
}
