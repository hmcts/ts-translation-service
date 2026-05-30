package uk.gov.hmcts.reform.translate.config;

import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.security.filter.TranslateCyEndpointFilter;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

@WebMvcTest(
    controllers = SecurityConfigurationTest.SecuredController.class,
    properties = {
        "spring.security.oauth2.client.provider.oidc.issuer-uri=https://issuer.example/o",
        "oidc.issuer=https://issuer.example/o"
    }
)
@Import({
    SecurityConfiguration.class,
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
            return new ServiceAuthFilter(new TestAuthTokenValidator(), List.of("test"));
        }

        @Bean
        PutDictionaryEndpointFilter putDictionaryEndpointFilter() {
            return new PutDictionaryEndpointFilter(mock(SecurityUtils.class));
        }

        @Bean
        TranslateCyEndpointFilter translateCyEndpointFilter() {
            return new TranslateCyEndpointFilter();
        }

        @Bean
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
            return new JwtGrantedAuthoritiesConverter(mock(IdamRepository.class));
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
            return "test";
        }
    }
}
