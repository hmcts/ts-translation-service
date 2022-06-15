package uk.gov.hmcts.reform.translate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;

@EnableWebSecurity
public class SecurityConfiguration {

  @Value("#{'${idam.s2s-authorised.services}'.split(',')}")
  private List<String> s2sNamesWhiteList;

  @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
  private String issuerUri;

  @Value("${oidc.issuer}")
  private String issuerOverride;

  @Bean
  public Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor() {
    return any -> s2sNamesWhiteList;
  }

  @Bean
  JwtDecoder jwtDecoder() {
    NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);
    // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
    final OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
    final OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp);
    jwtDecoder.setJwtValidator(validator);

    return jwtDecoder;
  }

}
