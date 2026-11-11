package uk.gov.hmcts.reform.translate.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.security.idam.IdamRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtGrantedAuthoritiesConverterTest {

    private static final String ACCESS_TOKEN = "access_token";
    @Mock
    private IdamRepository idamRepository;

    @InjectMocks
    private JwtGrantedAuthoritiesConverter converter;

    private final Jwt jwt = mock(Jwt.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Gets empty authorities")
    void shouldReturnEmptyAuthorities() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("No Claims should return empty authorities")
    void shouldReturnEmptyAuthoritiesWhenClaimNotAvailable() {
        when(jwt.hasClaim(anyString())).thenReturn(false);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("Should return empty authorities when token value is not matching with expected")
    void shouldReturnEmptyAuthoritiesWhenClaimValueNotEquals() {
        when(jwt.hasClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn("Test");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("Should return empty authorities when token value is not matching with expected")
    void shouldReturnEmptyAuthoritiesWhenIdamReturnsNoUsers() {
        when(jwt.hasClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List<String> roles = Collections.emptyList();
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(0, authorities.size());
    }

    @Test
    @DisplayName("Should return empty authorities when token value is not matching with expected")
    void shouldReturnAuthoritiesWhenIdamReturnsUserRoles() {
        when(jwt.hasClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List<String> roles = List.of("citizen");
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
    }

    @Test
    @DisplayName("Should rethrow any exceptions as AuthenticationServiceException")
    void shouldReThrowExceptionsAsAuthenticationServiceException() {
        when(jwt.hasClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        when(idamRepository.getUserInfo(anyString())).thenThrow(new IllegalStateException("Something went wrong"));
        AuthenticationServiceException authenticationServiceException = assertThrows(
            AuthenticationServiceException.class,
            () -> converter.convert(jwt)
        );

        assertEquals("IDAM error", authenticationServiceException.getMessage());
        assertEquals("Something went wrong", authenticationServiceException.getCause().getMessage());
    }

    @Test
    void idamFailuresAreNotLoggedBeforeTheBoundary() {
        when(jwt.hasClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        when(idamRepository.getUserInfo(anyString())).thenThrow(new IllegalStateException("Synthetic IDAM detail"));
        Logger logger = (Logger) LoggerFactory.getLogger(JwtGrantedAuthoritiesConverter.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            assertThrows(AuthenticationServiceException.class, () -> converter.convert(jwt));
            assertThat(appender.list).isEmpty();
            appender.list.clear();
            logger.setLevel(Level.DEBUG);
            assertThrows(AuthenticationServiceException.class, () -> converter.convert(jwt));
            assertThat(appender.list).isEmpty();
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
