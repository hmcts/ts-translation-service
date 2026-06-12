package uk.gov.hmcts.reform.translate.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.OidcIssuerConfiguration;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.security.filter.TranslateCyEndpointFilter;

import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final String issuerUri;
    private final String expectedIssuer;
    private final String allowedIssuers;
    private final ServiceAuthFilter serviceAuthFilter;
    private final PutDictionaryEndpointFilter putDictionaryEndpointFilter;
    private final TranslateCyEndpointFilter translateCyEndpointFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String[] AUTH_ALLOWED_LIST = {
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/webjars/**",
        "/v3/api-docs/**",
        "/health",
        "/health/liveness",
        "/health/readiness",
        "/info",
        "/favicon.ico",
        "/"
    };

    @Autowired
    public SecurityConfiguration(@Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
                                 final String issuerUri,
                                 @Value("${oidc.issuer}")
                                 final String expectedIssuer,
                                 @Value("${oidc.allowed-issuers:}")
                                 final String allowedIssuers,
                                 final ServiceAuthFilter serviceAuthFilter,
                                 final PutDictionaryEndpointFilter putDictionaryEndpointFilter,
                                 final TranslateCyEndpointFilter translateCyEndpointFilter,
                                 final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter) {
        super();
        this.issuerUri = issuerUri;
        this.expectedIssuer = expectedIssuer;
        this.allowedIssuers = allowedIssuers;
        this.serviceAuthFilter = serviceAuthFilter;
        this.putDictionaryEndpointFilter = putDictionaryEndpointFilter;
        this.translateCyEndpointFilter = translateCyEndpointFilter;
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(AUTH_ALLOWED_LIST);
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .addFilterBefore(translateCyEndpointFilter, ServiceAuthFilter.class)
            .addFilterAfter(putDictionaryEndpointFilter, ServiceAuthFilter.class)
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .csrf(csrf -> csrf.disable())
            .formLogin(fl -> fl.disable())
            .logout(l -> l.disable())
            .authorizeHttpRequests(ar -> ar.anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(withDefaults())
            ;
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);
        // See docs/security/jwt-issuer-validation.md for issuer-uri discovery and oidc.issuer enforcement.
        jwtDecoder.setJwtValidator(jwtValidator(expectedIssuer, allowedIssuers));
        return jwtDecoder;
    }

    static OAuth2TokenValidator<Jwt> jwtValidator(String expectedIssuer, String allowedIssuersOverride) {
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        Set<String> acceptedIssuers = OidcIssuerConfiguration.allowedIssuers(expectedIssuer, allowedIssuersOverride);
        OAuth2TokenValidator<Jwt> withIssuer = new JwtClaimValidator<>(
            "iss",
            issuer -> issuer != null && acceptedIssuers.contains(issuer.toString())
        );
        return new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
    }

}
