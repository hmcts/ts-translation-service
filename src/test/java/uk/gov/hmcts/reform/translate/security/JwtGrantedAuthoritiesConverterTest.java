package uk.gov.hmcts.reform.translate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class JwtGrantedAuthoritiesConverterTest {

    private static final String ACCESS_TOKEN = "access_token";
    @Mock
    private IdamRepository idamRepository;

    @InjectMocks
    private JwtGrantedAuthoritiesConverter converter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Gets empty authorities")
    void shouldReturnEmptyAuthorities() {
        Jwt jwt = mock(Jwt.class);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("No Claims should return empty authorities")
    void shouldReturnEmptyAuthoritiesWhenClaimNotAvailable() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(false);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("Should return empty authorities when token value is not matching with expected")
    void shouldReturnEmptyAuthoritiesWhenClaimValueNotEquals() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn("Test");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("Should return empty authorities when token value is not matching with expected")
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyAuthoritiesWhenIdamReturnsNoUsers() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List roles = new ArrayList();
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("Should return empty authorities when token value is not matching with expected")
    @SuppressWarnings("unchecked")
    void shouldReturnAuthoritiesWhenIdamReturnsUserRoles() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List roles = new ArrayList();
        roles.add("citizen");
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
    }
}
