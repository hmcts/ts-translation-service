package uk.gov.hmcts.reform.translate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SecurityUtils {
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    public static final String BEARER = "Bearer ";

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamRepository idamRepository;

    @Autowired
    public SecurityUtils(final AuthTokenGenerator authTokenGenerator, IdamRepository idamRepository) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamRepository = idamRepository;
    }

    public String getS2SToken() {
        return authTokenGenerator.generate();
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.add("user-id", getUserId());
        headers.add("user-roles", getUserRolesHeader());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            headers.add(HttpHeaders.AUTHORIZATION, getUserBearerToken());
        }
        return headers;
    }

    public UserInfo getUserInfo() {
        UserInfo userInfo = idamRepository.getUserInfo(getUserToken());
        if (userInfo != null) {
            log.info("SecurityUtils retrieved user info from idamRepository. User Id={}. Roles={}.",
                     userInfo.getUid(),
                     userInfo.getRoles());
        }
        return userInfo;
    }

    public String getUserId() {
        return getUserInfo().getUid();
    }

    public String getUserToken() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getTokenValue();
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

    public boolean hasManageTranslationsRole(List<String> roles) {
        String manageTranslationsRole = "manage-translations";
        return roles.contains(manageTranslationsRole);
    }
}

