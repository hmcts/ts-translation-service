package uk.gov.hmcts.reform.translate.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import lombok.val;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.ApplicationParams;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@DisplayName("SecurityUtils")
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    private static final String SERVICE_JWT = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ4dWlfd2ViYXBwIiwiZXhwIjoxNjUzOTM2NDgwfQ."
        + "JzkZ5WDfIj3lnNglBNqDMdA3nnwOuCdKwHl4wbLMi8uNGKFiKhyFcLVAiby2dX6dECZHDC0EgRjqQN2s9TXWow";
    private static final String USER_ID = "123";
    private static final String USER_JWT = "Bearer 8gf364fg367f67";
    private static final String CLIENT_ID = "xui_webapp";
    private static final Jwt JWT = Jwt.withTokenValue(USER_JWT)
        .claim("aClaim", "aClaim")
        .claim("aud", Lists.newArrayList("ccd_gateway"))
        .header("aHeader", "aHeader")
        .build();
    private static final Collection<? extends GrantedAuthority> AUTHORITY_COLLECTION = Stream.of("role1", "role2")
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toCollection(ArrayList::new));
    private static final UserInfo USER_INFO = UserInfo.builder()
        .uid(USER_ID)
        .sub("emailId@a.com")
        .roles(List.of("myRole", "my2ndRole", "manage-translations", "manage-expectations"))
        .build();

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private IdamRepository idamRepository;

    @Mock
    private AuthTokenGenerator serviceTokenGenerator;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private SecurityUtils underTest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("AuthorizationHeaders Tests")
    class AuthorizationHeaders {
        @BeforeEach
        void prepare() {
            doReturn(JWT).when(authentication).getPrincipal();
            doReturn(authentication).when(securityContext).getAuthentication();

            doReturn(SERVICE_JWT).when(serviceTokenGenerator).generate();
            doAnswer(invocationOnMock -> AUTHORITY_COLLECTION).when(authentication).getAuthorities();

            doReturn(USER_INFO).when(idamRepository).getUserInfo(USER_JWT);
        }

        @Test
        @DisplayName("authorizationHeaders")
        void authorizationHeaders() {
            final HttpHeaders headers = underTest.authorizationHeaders();

            assertAll(
                () -> assertHeader(headers, "ServiceAuthorization", SERVICE_JWT),
                () -> assertHeader(headers, "user-id", USER_ID),
                () -> assertHeader(headers, "user-roles", "role1,role2")
            );
        }

        private void assertHeader(HttpHeaders headers, String name, String value) {
            assertThat(headers.get(name))
                .isNotEmpty()
                .singleElement()
                .isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("Assert user token and test roles absent scenarios")
    class UserTokenAndAbsentRoles {
        @BeforeEach
        void prepare() {
            doReturn(JWT).when(authentication).getPrincipal();
            doReturn(authentication).when(securityContext).getAuthentication();
        }

        @Test
        @DisplayName("Check manage-translations role not present")
        void shouldReturnTrueWhenRoleNotPresent() {
            assertFalse(underTest.hasRole("unknown-role"));
        }

        @Test
        @DisplayName("Check any role not present")
        void shouldReturnTrueWhenHasAnyOfTheseNotRolesPresent() {
            assertFalse(underTest.hasAnyOfTheseRoles(List.of("NOTmyRole")));
        }
    }

    @Nested
    @DisplayName("Assert that roles are present")
    class RolesCheck {
        @BeforeEach
        void prepare() {
            doReturn(JWT).when(authentication).getPrincipal();
            doReturn(authentication).when(securityContext).getAuthentication();
            doReturn(USER_INFO).when(idamRepository).getUserInfo(USER_JWT);
        }

        @Test
        @DisplayName("Check role present")
        void shouldReturnTrueWhenRolePresent() {
            assertTrue(underTest.hasRole("myRole"));
        }

        @Test
        @DisplayName("Check any role present")
        void shouldReturnTrueWhenHasAnyOfTheseRolesPresent() {
            assertTrue(underTest.hasAnyOfTheseRoles(List.of("myRole")));
        }
    }

    @Nested
    @DisplayName("User Info tests")
    class UserInfoTests {
        @BeforeEach
        void prepare() {
            doReturn(authentication).when(securityContext).getAuthentication();
        }

        @Test
        @DisplayName("Get user info when principal is not null")
        void shouldReturnUserInfo() {
            doReturn(JWT).when(authentication).getPrincipal();
            doReturn(USER_INFO).when(idamRepository).getUserInfo(USER_JWT);

            assertThat(underTest.getUserInfo())
                .isNotNull()
                .isEqualTo(USER_INFO);
        }

        @Test
        @DisplayName("Get user info when principal is null")
        void shouldReturnNullWhenAuthenticationPrincipalIsNull() {
            doReturn(null).when(authentication).getPrincipal();

            assertThat(underTest.getUserInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("User Token tests")
    class UserTokenTests {
        @BeforeEach
        void prepare() {
            doReturn(authentication).when(securityContext).getAuthentication();
        }

        @Test
        @DisplayName("Get user token when principal is not null")
        void shouldReturnUserToken() {
            doReturn(JWT).when(authentication).getPrincipal();

            assertThat(underTest.getUserToken())
                .isNotBlank()
                .isEqualTo(USER_JWT);
        }

        @Test
        @DisplayName("Get user token when principal is null")
        void shouldReturnNullWhenAuthenticationPrincipalIsNull() {
            doReturn(null).when(authentication).getPrincipal();

            assertThat(underTest.getUserToken()).isNull();
        }
    }

    @Nested
    @DisplayName("Auth skipper parameters")
    class AuthSkipperParameter {
        @ParameterizedTest
        @MethodSource("provideArguments")
        @DisplayName("Return true if param is in the list otherwise false")
        void shouldTest(final List<String> input, final boolean expected) {
            doReturn(input).when(applicationParams).getPutDictionaryS2sServicesBypassRoleAuthCheck();

            final Boolean result = underTest.isBypassAuthCheck("ccd_definition");

            assertThat(result)
                .isNotNull()
                .isEqualTo(expected);
        }

        private static Stream<Arguments> provideArguments() {
            return Stream.of(
                Arguments.of(List.of(), false),
                Arguments.of(List.of("xui_webapp"), false),
                Arguments.of(List.of("ccd_definition"), true),
                Arguments.of(List.of("ccd_definition", "xui_webapp"), true)
            );
        }
    }

    @Test
    @DisplayName("get client from getServiceNameFromS2SToken")
    void shouldReturnAClientForGetServiceNameFromS2SToken() {
        val clientID = underTest.getServiceNameFromS2SToken(SERVICE_JWT);
        assertEquals(CLIENT_ID, clientID);
    }

    @Test
    @DisplayName("Fail get client from getServiceNameFromS2SToken")
    void shouldFailReturnAClientForGetServiceNameFromS2SToken() {
        val jwtDecodeException =
            assertThrows(JWTDecodeException.class, () ->
                underTest.getServiceNameFromS2SToken("prudaj"));

        assertEquals("The token was expected to have 3 parts, but got 1.", jwtDecodeException.getMessage());
    }
}
