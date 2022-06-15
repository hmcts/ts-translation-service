package uk.gov.hmcts.reform.translate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Slf4j
@Order(2) // Checking only for Idam User Token
@Configuration
public class UserSecurityConfiguration extends WebSecurityConfigurerAdapter {

//    private JwtAuthenticationConverter jwtAuthenticationConverter;
//
//    @Autowired
//    public UserSecurityConfiguration(final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter) {
//        super();
//        this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
//        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
//    }

  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers(WhiteList.AUTH_ALLOWED_LIST);
  }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
//            .sessionManagement().sessionCreationPolicy(STATELESS).and()
//            .csrf().disable()
//            .formLogin().disable()
//            .logout().disable()
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, "/translation/cy").permitAll()
            .anyRequest().authenticated();
//            .and()
//            .oauth2ResourceServer()
//            .jwt()
//            .jwtAuthenticationConverter(jwtAuthenticationConverter)
//            .and()
//            .and()
//            .oauth2Client();


//        try {
//            http
//              .headers().cacheControl().disable();
//            http.sessionManagement().sessionCreationPolicy(STATELESS).and()
//                .formLogin().disable()
//                .logout().disable()
//                .authorizeRequests()
//                .antMatchers(HttpMethod.POST, "/translation/cy").permitAll()
//                .and()
//                .oauth2ResourceServer()
//                .jwt()
//                .and()
//                .and()
//                .oauth2Client();
//        } catch (Exception e) {
//            log.info("Error in UserSecurityConfiguration: ", e);
//        }

    }

}
