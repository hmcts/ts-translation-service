package uk.gov.hmcts.reform.translate.security.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.translate.model.ControllerConstants;
import uk.gov.hmcts.reform.translate.security.HttpServletRequestWithoutAuthenticationHeader;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

@ExtendWith(MockitoExtension.class)
class PutDictionaryEndpointFilterTest {
    private static final String SERVICE_JWT = "eyJhbGciOiJIUzUxMiJ9";
    private static final String XUI_WEBAPP = "xui_webapp";
    private static final String CCD_DEFINITION = "ccd_definition";

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private PutDictionaryEndpointFilter underTest;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain filterChain = mock(FilterChain.class);
    private final SecurityContext securityContext = mock(SecurityContext.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "HEAD", "OPTIONS"})
    void testShouldPerformAuthenticationWhenNotPutEndpoint(final String param) throws Exception {
        doReturn(param).when(request).getMethod();
        doReturn(ControllerConstants.DICTIONARY_URL).when(request).getServletPath();
        doNothing().when(filterChain).doFilter(request, response);

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityUtils);
        verifyNoInteractions(securityContext);
    }

    @Test
    void testShouldPerformAuthenticationWhenNotPutDictionaryEndpoint() throws Exception {
        doReturn("PUT").when(request).getMethod();
        doReturn(ControllerConstants.TRANSLATIONS_URL).when(request).getServletPath();
        doNothing().when(filterChain).doFilter(request, response);

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityUtils);
        verifyNoInteractions(securityContext);
    }

    @Test
    void testShouldPerformAuthenticationWhenPutEndpointButNotPermittedService() throws Exception {
        doReturn("PUT").when(request).getMethod();
        doReturn(ControllerConstants.DICTIONARY_URL).when(request).getServletPath();
        doReturn(SERVICE_JWT).when(request).getHeader(SERVICE_AUTHORIZATION);
        doReturn(XUI_WEBAPP).when(securityUtils).getServiceNameFromS2SToken(anyString());
        doReturn(false).when(securityUtils).isBypassAuthCheck(anyString());
        doNothing().when(filterChain).doFilter(request, response);

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(securityUtils).getServiceNameFromS2SToken(anyString());
        verify(securityUtils).isBypassAuthCheck(XUI_WEBAPP);
        verifyNoInteractions(securityContext);
    }

    @Test
    void testShouldSkipAuthenticationWhenPutDictionaryEndpointAndPermittedService() throws Exception {
        doReturn("PUT").when(request).getMethod();
        doReturn(ControllerConstants.DICTIONARY_URL).when(request).getServletPath();
        doReturn(SERVICE_JWT).when(request).getHeader(SERVICE_AUTHORIZATION);
        doReturn(CCD_DEFINITION).when(securityUtils).getServiceNameFromS2SToken(anyString());
        doReturn(true).when(securityUtils).isBypassAuthCheck(anyString());
        doNothing().when(filterChain).doFilter(any(HttpServletRequestWithoutAuthenticationHeader.class), eq(response));

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(HttpServletRequestWithoutAuthenticationHeader.class), eq(response));
        verify(securityUtils).getServiceNameFromS2SToken(anyString());
        verify(securityUtils).isBypassAuthCheck(CCD_DEFINITION);
        verify(securityContext).setAuthentication(any(AbstractAuthenticationToken.class));
    }
}
