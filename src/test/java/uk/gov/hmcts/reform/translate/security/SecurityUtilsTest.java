package uk.gov.hmcts.reform.translate.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import lombok.val;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    private static final String SERVICE_JWT = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ4dWlfd2ViYXBwIiwiZXhwIjoxNjUzOTM2NDgwfQ."
        + "JzkZ5WDfIj3lnNglBNqDMdA3nnwOuCdKwHl4wbLMi8uNGKFiKhyFcLVAiby2dX6dECZHDC0EgRjqQN2s9TXWow";
    private static final String USER_ID = "123";
    private static final String USER_JWT = "Bearer 8gf364fg367f67";
    private static final String CLIENT_ID = "xui_webapp";

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private IdamRepository idamRepository;

    @Mock
    private AuthTokenGenerator serviceTokenGenerator;

    @InjectMocks
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(serviceTokenGenerator.generate()).thenReturn(SERVICE_JWT);

        Jwt jwt =   Jwt.withTokenValue(USER_JWT)
            .claim("aClaim", "aClaim")
            .claim("aud", Lists.newArrayList("ccd_gateway"))
            .header("aHeader", "aHeader")
            .build();
        Collection<? extends GrantedAuthority> authorityCollection = Stream.of("role1", "role2")
            .map(a -> new SimpleGrantedAuthority(a))
            .collect(Collectors.toCollection(ArrayList::new));

        doReturn(jwt).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        when(authentication.getAuthorities()).thenAnswer(invocationOnMock -> authorityCollection);
        SecurityContextHolder.setContext(securityContext);


        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID)
            .sub("emailId@a.com")
            .roles(List.of("myRole", "my2ndRole", "manage-translations", "manage-expectations"))
            .build();
        doReturn(userInfo).when(idamRepository).getUserInfo(USER_JWT);
    }

    @Test
    @DisplayName("authorizationHeaders")
    void authorizationHeaders() {
        final HttpHeaders headers = securityUtils.authorizationHeaders();

        assertAll(
            () -> assertHeader(headers, "ServiceAuthorization", SERVICE_JWT),
            () -> assertHeader(headers, "user-id", USER_ID),
            () -> assertHeader(headers, "user-roles", "role1,role2")
        );
    }

    @Test
    @DisplayName("Get user token")
    void shouldReturnUserToken() {
        assertThat(securityUtils.getUserToken(), is(USER_JWT));
    }

    private void assertHeader(HttpHeaders headers, String name, String value) {
        assertThat(headers.get(name), hasSize(1));
        assertThat(headers.get(name).get(0), equalTo(value));
    }

    @Test
    @DisplayName("Check role present")
    void shouldReturnTrueWhenRolePresent() {
        assertTrue(securityUtils.hasRole("myRole"));
    }

    @Test
    @DisplayName("Check manage-translations role not present")
    void shouldReturnTrueWhenRoleNotPresent() {
        assertFalse(securityUtils.hasRole("unknown-role"));
    }

    @Test
    @DisplayName("Check any role present")
    void shouldReturnTrueWhenHasAnyOfTheseRolesPresent() {
        assertTrue(securityUtils.hasAnyOfTheseRoles(Arrays.asList("myRole")));
    }

    @Test
    @DisplayName("Check any role not present")
    void shouldReturnTrueWhenHasAnyOfTheseNotRolesPresent() {
        assertFalse(securityUtils.hasAnyOfTheseRoles(Arrays.asList("NOTmyRole")));
    }

    @Test
    @DisplayName("get client from getServiceNameFromS2SToken")
    void shouldReturnAClientForGetServiceNameFromS2SToken() {
        val clientID = securityUtils.getServiceNameFromS2SToken(SERVICE_JWT);
        assertEquals(CLIENT_ID,clientID);
    }


    @Test
    @DisplayName("Fail get client from getServiceNameFromS2SToken")
    void shouldFailReturnAClientForGetServiceNameFromS2SToken() {
        val jwtDecodeException =
            assertThrows(JWTDecodeException.class, () ->
                securityUtils.getServiceNameFromS2SToken("prudaj"));

        assertEquals("The token was expected to have 3 parts, but got 1.", jwtDecodeException.getMessage());
    }
}
