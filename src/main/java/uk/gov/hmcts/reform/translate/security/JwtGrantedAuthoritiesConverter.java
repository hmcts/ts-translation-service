package uk.gov.hmcts.reform.translate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;

@Component
@Slf4j
public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    public static final String TOKEN_NAME = "tokenName";

    private final IdamRepository idamRepository;

    @Autowired
    public JwtGrantedAuthoritiesConverter(IdamRepository idamRepository) {
        this.idamRepository = idamRepository;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (jwt.containsClaim(TOKEN_NAME) && jwt.getClaim(TOKEN_NAME).equals(ACCESS_TOKEN)) {
            UserInfo userInfo;

            try {
                userInfo = idamRepository.getUserInfo(jwt.getTokenValue());
                log.info("JwtGrantedAuthoritiesConverter retrieved user info from idamRepository."
                             + " User Id={}. Roles={}.",
                         userInfo.getUid(), userInfo.getRoles());

            } catch (Exception ex) {
                // NB: catch, log and then throw a recognised spring authentication error as exception during
                // HttpSecurity filters may fall outside of the remit of the RestExceptionHandler
                log.error("IDAM error", ex);
                throw new AuthenticationServiceException("IDAM error", ex);
            }

            return extractAuthorityFromClaims(userInfo.getRoles());
        }
        return Collections.emptyList();
    }

    private List<GrantedAuthority> extractAuthorityFromClaims(List<String> roles) {
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
}
