package uk.gov.hmcts.reform.translate.security.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.translate.controllers.ControllerConstants;
import uk.gov.hmcts.reform.translate.security.HttpServletRequestWithoutAuthenticationHeader;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TranslateCyEndpointFilterTest {
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private TranslateCyEndpointFilter underTest;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain filterChain = mock(FilterChain.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class,
        names = {"POST"},
        mode = EnumSource.Mode.EXCLUDE)
    void testShouldPerformAuthenticationWhenNotPostTranslateCyEndpoint(final HttpMethod param) throws Exception {
        doReturn(param.name()).when(request).getMethod();
        doReturn(ControllerConstants.TRANSLATIONS_URL).when(request).getServletPath();
        doNothing().when(filterChain).doFilter(request, response);

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityContext);
    }

    @Test
    void testShouldPerformAuthenticationWhenNotPostTranslateCyEndpoint() throws Exception {
        doReturn("POST").when(request).getMethod();
        doReturn(ControllerConstants.DICTIONARY_URL).when(request).getServletPath();
        doNothing().when(filterChain).doFilter(request, response);

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityContext);
    }

    @Test
    void testShouldSkipAuthenticationWhenPostTranslateCyEndpoint() throws Exception {
        doReturn("POST").when(request).getMethod();
        doReturn(ControllerConstants.TRANSLATIONS_URL).when(request).getServletPath();
        doNothing().when(filterChain).doFilter(any(HttpServletRequestWithoutAuthenticationHeader.class), eq(response));

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(HttpServletRequestWithoutAuthenticationHeader.class), eq(response));
        verify(securityContext).setAuthentication(any(AbstractAuthenticationToken.class));
    }
}
