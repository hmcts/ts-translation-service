package uk.gov.hmcts.reform.translate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Slf4j
@Order(1) // Checking only for S2S Token
@Configuration
public class S2sSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private final ServiceAuthFilter serviceAuthFilter;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  @Autowired
  public S2sSecurityConfiguration(final ServiceAuthFilter serviceAuthFilter,
                                  final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter) {
    super();
    this.serviceAuthFilter = serviceAuthFilter;
    jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
  }

  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers(WhiteList.AUTH_ALLOWED_LIST);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
      .sessionManagement().sessionCreationPolicy(STATELESS).and()
      .csrf().disable()
      .formLogin().disable()
      .logout().disable()
      .authorizeRequests()
      .anyRequest()
      .authenticated()
      .and()
      .oauth2ResourceServer()
      .jwt()
      .jwtAuthenticationConverter(jwtAuthenticationConverter)
      .and()
      .and()
      .oauth2Client();
  }

}
