package uk.gov.hmcts.reform.translate.security;

import com.auth0.jwt.JWT;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.ApplicationParams;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class SecurityUtils {

    public static final String AUTHORIZATION = HttpHeaders.AUTHORIZATION;
    public static final String SERVICE_AUTHORIZATION = ServiceAuthFilter.AUTHORISATION;
    public static final String MANAGE_TRANSLATIONS_ROLE = "manage-translations";
    public static final String LOAD_TRANSLATIONS_ROLE = "load-translations";
    public static final String BEARER = "Bearer ";

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamRepository idamRepository;
    private final ApplicationParams applicationParams;

    @Autowired
    public SecurityUtils(final AuthTokenGenerator authTokenGenerator,
                         final IdamRepository idamRepository,
                         final ApplicationParams applicationParams) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamRepository = idamRepository;
        this.applicationParams = applicationParams;
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.add("user-id", getUserId());
        headers.add("user-roles", getUserRolesHeader());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            headers.add(AUTHORIZATION, getUserBearerToken());
        }
        return headers;
    }

    public UserInfo getUserInfo() {
        return Optional.ofNullable(getUserToken())
            .map(userToken -> {
                final UserInfo userInfo = idamRepository.getUserInfo(userToken);
                if (userInfo != null) {
                    log.info(
                        "SecurityUtils retrieved user info from idamRepository. User Id={}. Roles={}.",
                        userInfo.getUid(),
                        userInfo.getRoles()
                    );
                }
                return userInfo;
            })
            .orElse(null);
    }

    public String getUserId() {
        return getUserInfo().getUid();
    }

    public String getUserToken() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
            .map(principal -> {
                Jwt jwt = (Jwt) principal;
                return jwt.getTokenValue();
            })
            .orElse(null);
    }

    public String getUserBearerToken() {
        return BEARER + getUserToken();
    }

    public String getUserRolesHeader() {
        Collection<? extends GrantedAuthority> authorities =
            SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    }

    public boolean hasRole(String roleToMatch) {
        UserInfo userInfo = getUserInfo();
        return userInfo != null && userInfo.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase(roleToMatch));
    }

    public boolean hasAnyOfTheseRoles(List<String> roleToMatch) {
        val userInfo = getUserInfo();
        return userInfo != null
            && userInfo.getRoles().stream().anyMatch(roleToMatch.stream().collect(toSet())::contains);
    }

    public String getServiceNameFromS2SToken(String serviceAuthenticationToken) {
        // NB: this grabs the service name straight from the token under the assumption
        // that the S2S token has already been verified elsewhere
        return JWT.decode(removeBearerFromToken(serviceAuthenticationToken)).getSubject();
    }

    private String removeBearerFromToken(String token) {
        return token.startsWith(BEARER) ? token.substring(BEARER.length()) : token;
    }

    public boolean isBypassAuthCheck(String clientServiceName) {
        return applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck().contains(clientServiceName);
    }
}

