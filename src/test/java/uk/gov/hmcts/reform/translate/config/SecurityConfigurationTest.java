package uk.gov.hmcts.reform.translate.config;

import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.translate.security.SafeServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.security.filter.TranslateCyEndpointFilter;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;
import uk.gov.hmcts.reform.translate.controllers.DictionaryController;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

@WebMvcTest(
    controllers = {SecurityConfigurationTest.SecuredController.class, DictionaryController.class},
    properties = {
        "spring.security.oauth2.client.provider.oidc.issuer-uri=https://issuer.example/o",
        "oidc.issuer=https://issuer.example/o"
    }
)
@Import({
    SecurityConfiguration.class,
    DictionaryController.class,
    SecurityConfigurationTest.SecuredController.class,
    SecurityConfigurationTest.TestConfig.class
})
class SecurityConfigurationTest {

    private static final String CLIENT_REGISTRATION_REPOSITORY =
        "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository";
    private static final String USER_ID = "user-123";
    private static final String USER_TOKEN = "test-token";
    private static final String SERVICE_TOKEN = "service-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private IdamRepository idamRepository;

    @MockitoBean
    private DictionaryService dictionaryService;

    @MockitoBean
    private SecurityUtils securityUtils;

    @MockitoBean(name = "jwtDecoder")
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(USER_TOKEN)).thenReturn(
            Jwt.withTokenValue(USER_TOKEN)
                .header("alg", "none")
                .claim("sub", USER_ID)
                .build()
        );
    }

    @Test
    void invalidJwtDoesNotExposeDecoderDiagnostics() throws Exception {
        when(jwtDecoder.decode(USER_TOKEN)).thenThrow(
            new org.springframework.security.oauth2.jwt.BadJwtException("decoder at http://internal-service/jwks"));
        mockMvc.perform(get("/secured-test")
                .header("Authorization", "Bearer " + USER_TOKEN)
                .header("ServiceAuthorization", "service-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("WWW-Authenticate", "Bearer error=\"invalid_token\""));
    }

    @Test
    void missingJwtStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/secured-test").header("ServiceAuthorization", "service-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(""));
    }

    @Test
    void reportedEndpointsRetainTheirServiceAuthenticationRequirement() throws Exception {
        for (var request : List.of(get("/dictionary").servletPath("/dictionary"),
            put("/dictionary").servletPath("/dictionary"),
            post("/translation/cy").servletPath("/translation/cy"))) {
            mockMvc.perform(request.contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        }
        verifyNoInteractions(dictionaryService, jwtDecoder);
    }

    @Test
    void dictionaryStillRequiresUserAuthenticationAfterServiceValidation() throws Exception {
        mockMvc.perform(get("/dictionary").servletPath("/dictionary")
                .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN))
            .andExpect(status().isUnauthorized()).andExpect(content().string(""))
            .andExpect(header().string("WWW-Authenticate", "Bearer"));
        verifyNoInteractions(dictionaryService);
    }

    @Test
    void translationsRetainServiceOnlyAccessAndSafeValidation() throws Exception {
        mockMvc.perform(post("/translation/cy").servletPath("/translation/cy")
                .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN).header(AUTHORIZATION, "Bearer invalid-user-token")
                .contentType("application/json").content("{\"phrases\":[\"synthetic phrase\"]}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/translation/cy").servletPath("/translation/cy")
                .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN)
                .contentType("application/json").content("{SYNTHETIC_PRIVATE_INPUT"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.path").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("SYNTHETIC_PRIVATE_INPUT"))));
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void approvedDictionaryServicesStillBypassUserAuthentication() throws Exception {
        when(securityUtils.getServiceNameFromS2SToken(SERVICE_TOKEN)).thenReturn("test");
        when(securityUtils.isBypassAuthCheck("test")).thenReturn(true);
        mockMvc.perform(put("/dictionary").servletPath("/dictionary")
                .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN).header(AUTHORIZATION, "Bearer invalid-user-token")
                .contentType("application/json").content("{\"translations\":{}}"))
            .andExpect(status().isCreated());
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void shouldAuthenticateBearerJwtWithoutOauth2ClientRegistration() throws Exception {
        mockMvc.perform(get("/secured-test")
                            .header(AUTHORIZATION, "Bearer " + USER_TOKEN)
                            .header(SERVICE_AUTHORIZATION, SERVICE_TOKEN))
            .andExpect(status().isOk())
            .andExpect(content().string("JwtAuthenticationToken:" + USER_ID));

        assertThat(environment.containsProperty("spring.security.oauth2.client.registration.oidc.client-id"))
            .isFalse();
        assertThat(environment.containsProperty("spring.security.oauth2.client.registration.oidc.client-secret"))
            .isFalse();
        assertThat(ClassUtils.isPresent(CLIENT_REGISTRATION_REPOSITORY, applicationContext.getClassLoader()))
            .isFalse();
    }

    @Test
    void serviceFailuresAreSafeThroughTheCompleteSecurityChain() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeServiceAuthFilter.class);
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            mockMvc.perform(get("/secured-test")
                    .header("Authorization", "Bearer " + USER_TOKEN)
                    .header(ServiceAuthFilter.AUTHORISATION, "invalid-service-token"))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""));
            mockMvc.perform(get("/secured-test")
                    .header("Authorization", "Bearer " + USER_TOKEN)
                    .header(ServiceAuthFilter.AUTHORISATION, "denied-service-token"))
                .andExpect(status().isForbidden()).andExpect(content().string(""));
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).contains(
                "Service authentication failed: code=AUTHENTICATION_REQUIRED status=401",
                "Service access denied: code=ACCESS_DENIED status=403");
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .allSatisfy(event -> {
                    assertThat(event.getFormattedMessage()).doesNotContain("Synthetic", "private-service-id");
                    assertThat(event.getThrowableProxy()).isNull();
                });
        } finally {
            logger.setLevel(previousLevel);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void idamOutagesStay500AndDoNotEscapeTheSecurityChain() throws Exception {
        when(jwtDecoder.decode(USER_TOKEN)).thenReturn(
            Jwt.withTokenValue(USER_TOKEN).header("alg", "none").claim("sub", USER_ID)
                .claim("tokenName", "access_token").build());
        when(idamRepository.getUserInfo(USER_TOKEN))
            .thenThrow(new IllegalStateException("Synthetic private IDAM detail"));
        Logger logger = (Logger) LoggerFactory.getLogger(
            uk.gov.hmcts.reform.translate.security.SafeAuthenticationErrors.class);
        Logger converterLogger = (Logger) LoggerFactory.getLogger(JwtGrantedAuthoritiesConverter.class);
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        Level previousConverterLevel = converterLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            converterLogger.setLevel(Level.INFO);
            for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                logger.setLevel(level);
                appender.list.clear();
                mockMvc.perform(get("/secured-test")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .header(ServiceAuthFilter.AUTHORISATION, "service-token"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(""))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .doesNotExist("WWW-Authenticate"));
                assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .contains("Authentication service failed: code=INTERNAL_ERROR status=500");
                assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.ERROR).hasSize(1);
                assertThat(appender.list).filteredOn(event -> event.getThrowableProxy() != null)
                    .hasSize(level == Level.DEBUG ? 1 : 0);
                assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .allSatisfy(event -> {
                        assertThat(event.getFormattedMessage()).doesNotContain("Synthetic private IDAM detail");
                        assertThat(event.getThrowableProxy()).isNull();
                    });
                if (level == Level.DEBUG) {
                    assertThat(appender.list).filteredOn(event -> event.getLoggerName().equals(logger.getName())
                        && event.getLevel() == Level.DEBUG).anySatisfy(event ->
                            assertThat(event.getThrowableProxy().getCause().getMessage())
                                .isEqualTo("Synthetic private IDAM detail"));
                }
            }
        } finally {
            logger.setLevel(previousLevel);
            converterLogger.setLevel(previousConverterLevel);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    @RestController
    public static class SecuredController {

        @GetMapping("/secured-test")
        String secured(Authentication authentication) {
            return authentication.getClass().getSimpleName() + ":" + authentication.getName();
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ServiceAuthFilter serviceAuthFilter() {
            return new SafeServiceAuthFilter(new TestAuthTokenValidator(), List.of("test"));
        }

        @Bean
        PutDictionaryEndpointFilter putDictionaryEndpointFilter(SecurityUtils securityUtils) {
            return new PutDictionaryEndpointFilter(securityUtils);
        }

        @Bean
        TranslateCyEndpointFilter translateCyEndpointFilter() {
            return new TranslateCyEndpointFilter();
        }

        @Bean
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter(IdamRepository repository) {
            return new JwtGrantedAuthoritiesConverter(repository);
        }
    }

    private static final class TestAuthTokenValidator implements AuthTokenValidator {

        @Override
        public void validate(String token) {
            // Not used by ServiceAuthFilter for this request path.
        }

        @Override
        public void validate(String token, List<String> roles) {
            // Not used by ServiceAuthFilter for this request path.
        }

        @Override
        public String getServiceName(String token) {
            if (token.equals("Bearer invalid-service-token")) {
                throw new InvalidTokenException("Synthetic internal S2S diagnostic");
            }
            return token.equals("Bearer denied-service-token") ? "private-service-id" : "test";
        }
    }
}
