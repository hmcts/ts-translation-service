package uk.gov.hmcts.reform.translate.controllers;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.translate.BaseTest;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationalEndpointsIT extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void stubServiceAuthHealth() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/s2s/health"))
                             .willReturn(WireMock.okJson("{\"status\":\"UP\"}")));
    }

    @Test
    @DisplayName("Health endpoint reports the application is up")
    void shouldReportApplicationHealth() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.groups").isArray());
    }

    @Test
    @DisplayName("Liveness endpoint reports the application is alive")
    void shouldReportApplicationLiveness() throws Exception {
        mockMvc.perform(get("/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Readiness endpoint reports the application is ready")
    void shouldReportApplicationReadiness() throws Exception {
        mockMvc.perform(get("/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Info endpoint returns actuator information")
    void shouldReturnApplicationInfo() throws Exception {
        mockMvc.perform(get("/info"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"));
    }

    @Test
    @DisplayName("Swagger UI endpoint serves the documentation page")
    void shouldServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    }

    @Test
    @DisplayName("OpenAPI endpoint serves the live API specification")
    void shouldServeOpenApiSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.openapi", startsWith("3.")))
            .andExpect(jsonPath("$.info.title").value("Welsh Language Translation Service"))
            .andExpect(jsonPath("$.paths", hasKey("/dictionary")))
            .andExpect(jsonPath("$.paths", hasKey("/translation/cy")));
    }
}
