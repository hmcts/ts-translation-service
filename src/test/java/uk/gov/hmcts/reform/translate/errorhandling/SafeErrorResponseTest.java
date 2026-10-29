package uk.gov.hmcts.reform.translate.errorhandling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))))
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void preservesOnlyAllowlistedBadRequestMessages() throws Exception {
        for (String message : new String[] {BadRequestError.BAD_SCHEMA, BadRequestError.WELSH_NOT_ALLOWED}) {
            controller.failure = new BadRequestException(message);
            mvc.perform(get("/error-probe/failure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
        }
        controller.failure = new BadRequestException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Bad Request"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
        controller.failure = new RuntimeException(BadRequestError.WELSH_NOT_ALLOWED);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(not(containsString(BadRequestError.WELSH_NOT_ALLOWED))));
    }

    @Test
    void hidesTypeConversionDiagnostics() throws Exception {
        mvc.perform(get("/error-probe/id/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.path").doesNotExist())
            .andExpect(content().string(not(containsString("not-a-uuid"))))
            .andExpect(content().string(not(containsString("java.util.UUID"))))
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void hidesMalformedJsonDiagnostics() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(content().string(not(containsString("JsonParseException"))))
            .andExpect(content().string(not(containsString("invalid"))))
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void retainsUnsupportedMediaAndMethodStatuses() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.TEXT_PLAIN).content("invalid"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(header().string("Accept", containsString("application/json")))
            .andExpect(jsonPath("$.exception").doesNotExist());
        mvc.perform(get("/error-probe"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void validRequestsStillSucceed() throws Exception {
        mvc.perform(post("/error-probe").contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"ok\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.value").value("ok"));
    }

    @Test
    void logsSafeCodesAndRetainsOriginalExceptionsAtInfo() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RestExceptionHandler.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        controller.failure = new RuntimeException(DIAGNOSTIC);
        try {
            logger.setLevel(Level.INFO);
            mvc.perform(get("/error-probe/failure")).andExpect(status().isInternalServerError());
            mvc.perform(get("/error-probe/id/not-a-uuid")).andExpect(status().isBadRequest());
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "Request failed: code=INTERNAL_ERROR status=500",
                "Request failed: code=INVALID_REQUEST status=400");
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getThrowableProxy()).isNotNull();
            });
            appender.list.clear();
            logger.setLevel(Level.DEBUG);
            mvc.perform(get("/error-probe/failure")).andExpect(status().isInternalServerError());
            assertThat(appender.list).hasSize(1);
            ILoggingEvent diagnostic = appender.list.get(0);
            assertThat(diagnostic.getLevel()).isEqualTo(Level.ERROR);
            assertThat(diagnostic.getFormattedMessage()).isEqualTo(
                "Request failed: code=INTERNAL_ERROR status=500");
            assertThat(diagnostic.getThrowableProxy().getMessage()).isEqualTo(DIAGNOSTIC);
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void returnsSafeAuthenticationCategoriesFromAdvice() throws Exception {
        controller.failure = new org.springframework.security.authentication.BadCredentialsException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
        controller.failure = new org.springframework.security.access.AccessDeniedException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
    }

    @Test
    void responseSerializationFailuresRetainTheExistingServerErrorStatus() throws Exception {
        controller.failure = new org.springframework.http.converter.HttpMessageNotWritableException(DIAGNOSTIC);
        mvc.perform(get("/error-probe/failure"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(content().string(not(containsString(DIAGNOSTIC))));
    }

    @Test
    void methodDiagnosticsRemainInLogsButNotResponses() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RestExceptionHandler.class);
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            mvc.perform(request(HttpMethod.valueOf("SYNTHETIC_PRIVATE_METHOD"), "/error-probe"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(content().string(not(containsString("SYNTHETIC_PRIVATE_METHOD"))));
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("Request failed: code=METHOD_NOT_ALLOWED status=405");
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .allSatisfy(event -> {
                    assertThat(event.getFormattedMessage()).doesNotContain("SYNTHETIC_PRIVATE_METHOD");
                    assertThat(event.getThrowableProxy()).isNotNull();
                });
        } finally {
            logger.setLevel(previousLevel);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
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
