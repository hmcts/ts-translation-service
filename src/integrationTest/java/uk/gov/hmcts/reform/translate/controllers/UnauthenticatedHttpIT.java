package uk.gov.hmcts.reform.translate.controllers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.translate.Application;
import uk.gov.hmcts.reform.translate.logging.SafeHealthDiagnosticsFilter;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.address=127.0.0.1", "management.endpoint.health.cache.time-to-live=0ms"})
@ActiveProfiles("itest")
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@Import(UnauthenticatedHttpIT.ProbeConfiguration.class)
class UnauthenticatedHttpIT {

    private static final String PRIVATE = "SYNTHETIC_PRIVATE_IDENTIFIER";
    private static final String DIAGNOSTICS = "?trace=true&message=true&errors=true&exception=true";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @LocalServerPort
    private int port;

    @Autowired
    private ProbeHealthIndicator health;

    @BeforeEach
    void captureLogs() {
        health.down = false;
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/s2s/health")
            .willReturn(okJson("{\"status\":\"UP\"}")));
        events.start();
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(events);
    }

    @AfterEach
    void stopCapturing() {
        health.down = false;
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(events);
        events.stop();
    }

    @Test
    void publicEndpointsRemainAvailableWithoutCredentials() throws Exception {
        for (String path : List.of("/", "/health", "/health/liveness", "/health/readiness", "/info",
            "/swagger-ui/index.html", "/v3/api-docs")) {
            assertThat(request("GET", path, "*/*").statusCode()).as(path).isEqualTo(200);
        }
    }

    @Test
    void protectedEndpointsRemainEmpty401WithoutCredentials() throws Exception {
        for (String accept : List.of("application/json", "text/html")) {
            for (String path : List.of("/dictionary", "/translation/cy")) {
                HttpResponse<String> response = request(path.endsWith("cy") ? "POST" : "PUT",
                    path + DIAGNOSTICS, accept);
                assertThat(response.statusCode()).isEqualTo(401);
                assertThat(response.body()).isEmpty();
                assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
            }
        }
        assertThat(events.list).filteredOn(event -> event.getLoggerName().endsWith("SafeServiceAuthFilter"))
            .hasSize(4).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getThrowableProxy()).isNotNull();
            });
    }

    @Test
    void publicMvcFailuresRetainExistingJsonAndHtmlBehaviour() throws Exception {
        for (String accept : List.of("application/json", "text/html")) {
            HttpResponse<String> method = request("POST", "/" + DIAGNOSTICS, accept);
            // HTML currently falls through to the protected error dispatch; changing this needs approval.
            assertThat(method.statusCode()).as(accept).isEqualTo(accept.equals("text/html") ? 401 : 405);
            if (accept.equals("application/json")) {
                assertThat(method.headers().firstValue("Allow")).hasValue("GET");
                assertThat(method.body()).contains("METHOD_NOT_ALLOWED");
            }
            assertSafeBody(method.body());
            HttpResponse<String> missing = request("GET", "/swagger-ui/" + PRIVATE + DIAGNOSTICS, accept);
            assertThat(missing.statusCode()).as(accept).isEqualTo(accept.equals("text/html") ? 401 : 404);
            assertSafeBody(missing.body());
        }
        assertThat(events.list).filteredOn(event -> event.getLevel() == Level.ERROR
            && event.getLoggerName().endsWith("RestExceptionHandler")).hasSize(4)
            .allSatisfy(event -> assertThat(event.getThrowableProxy()).isNotNull());
    }

    @Test
    void healthFailuresHidePublicDetailsAndRetainOneServerCauseAtNormalLevels() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeHealthDiagnosticsFilter.class);
        Level previous = logger.getLevel();
        health.down = true;
        try {
            for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                logger.setLevel(level);
                events.list.clear();
                HttpResponse<String> response = request("GET", "/health" + DIAGNOSTICS, "application/json");
                assertThat(response.statusCode()).isEqualTo(503);
                assertThat(response.body()).contains("DOWN").doesNotContain("details", "components");
                assertSafeBody(response.body());
                assertThat(events.list).filteredOn(event -> event.getLoggerName().equals(logger.getName()))
                    .singleElement().satisfies(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getThrowableProxy().getMessage()).isEqualTo(PRIVATE);
                    });
            }
        } finally {
            logger.setLevel(previous);
        }
    }

    @Test
    void malformedHttpRequestsDoNotExposeContainerDiagnostics() throws Exception {
        for (String accept : List.of("application/json", "text/html")) {
            for (String target : List.of("/%ZZ-" + PRIVATE, "/ HTTP/invalid-" + PRIVATE)) {
                try (Socket socket = new Socket("127.0.0.1", port)) {
                    socket.setSoTimeout(10000);
                    String raw = "GET " + target + " HTTP/1.1\r\nHost: localhost\r\nAccept: " + accept
                        + "\r\nConnection: close\r\n\r\n";
                    socket.getOutputStream().write(raw.getBytes(StandardCharsets.US_ASCII));
                    String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    assertThat(response).startsWith("HTTP/1.1 400");
                    assertSafeBody(response);
                }
            }
        }
        assertThat(events.list).filteredOn(event -> event.getLoggerName()
            .endsWith("SafeDependencyDiagnosticsFilter") && event.getLevel() == Level.INFO)
            .anySatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("org.apache.coyote.http11.Http11Processor");
                assertThat(event.getThrowableProxy().getMessage()).contains(PRIVATE);
            });
    }

    private HttpResponse<String> request(String method, String path, String accept) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10)).header("Accept", accept).header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertSafeBody(String body) {
        assertThat(body).doesNotContain(PRIVATE, "java.lang", "org.apache", "Apache Tomcat", "Exception",
            "Whitelabel", "\"path\"", "\"trace\"", "\"exception\"", "/opt/app");
    }

    @TestConfiguration
    static class ProbeConfiguration {
        @Bean
        ProbeHealthIndicator anonymousProbeHealthIndicator() {
            return new ProbeHealthIndicator();
        }
    }

    static class ProbeHealthIndicator extends AbstractHealthIndicator {
        private volatile boolean down;

        @Override
        protected void doHealthCheck(Health.Builder builder) {
            if (down) {
                throw new IllegalStateException(PRIVATE);
            }
            builder.up();
        }
    }
}
