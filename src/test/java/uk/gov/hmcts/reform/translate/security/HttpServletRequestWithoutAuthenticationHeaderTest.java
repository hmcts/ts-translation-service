package uk.gov.hmcts.reform.translate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpServletRequestWithoutAuthenticationHeaderTest {
    private static final String APPLICATION_JSON = "application/json";
    private static final String BEARER_TOKEN = "Bearer IyeJhbGciOiJIUzUxMiJ9";
    private static final String TEST_HEADER = "Test-Header";
    private static final String TEST_HEADER_VALUE_ONE = "Test-Value-One";
    private static final String TEST_HEADER_VALUE_TWO = "Test-Value-Two";

    private HttpServletRequestWithoutAuthenticationHeader underTest;

    @Nested
    @DisplayName("Test GetHeader")
    class GetHeader {
        @BeforeEach
        void prepare() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);

            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);
        }

        @Test
        void testShouldGetHeaderWhenNotAuthorization() {
            final String header = underTest.getHeader(HttpHeaders.ACCEPT);

            assertThat(header)
                .isNotNull()
                .isEqualTo(APPLICATION_JSON);
        }

        @Test
        void testShouldReturnNullWhenAuthorization() {
            final String header = underTest.getHeader(HttpHeaders.AUTHORIZATION);

            assertThat(header)
                .isNull();
        }
    }

    @Nested
    @DisplayName("Test GetHeaderNames")
    class GetHeaderNames {
        @Test
        void testShouldReturnEmptyWhenOnlyAuthorizationPresent() {
            // GIVEN
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);

            // WHEN
            final Enumeration<String> headerNames = underTest.getHeaderNames();

            // THEN
            assertThat(headerNames)
                .isNotNull()
                .satisfies(enumeration -> assertThat(enumeration.asIterator())
                    .isNotNull()
                    .toIterable()
                    .isEmpty());
        }

        @Test
        void testShouldReturnHeaderNames() {
            // GIVEN
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            request.addHeader(TEST_HEADER, List.of(TEST_HEADER_VALUE_ONE, TEST_HEADER_VALUE_TWO));
            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);

            // WHEN
            final Enumeration<String> headerNames = underTest.getHeaderNames();

            // THEN
            assertThat(headerNames)
                .isNotNull()
                .satisfies(enumeration -> assertThat(enumeration.asIterator())
                    .isNotNull()
                    .toIterable()
                    .containsExactly(HttpHeaders.ACCEPT, TEST_HEADER));
        }

        @Test
        void testShouldFilterOutAuthorizationHeader() {
            // GIVEN
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
            request.addHeader(TEST_HEADER, List.of(TEST_HEADER_VALUE_ONE, TEST_HEADER_VALUE_TWO));
            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);

            // WHEN
            final Enumeration<String> headerNames = underTest.getHeaderNames();

            // THEN
            assertThat(headerNames)
                .isNotNull()
                .satisfies(enumeration -> assertThat(enumeration.asIterator())
                    .isNotNull()
                    .toIterable()
                    .doesNotContain(HttpHeaders.AUTHORIZATION)
                    .containsExactly(HttpHeaders.ACCEPT, TEST_HEADER));
        }
    }


    @Nested
    @DisplayName("Test GetHeaders")
    class GetHeaders {
        @Test
        void testShouldReturnEmptyWhenGetAuthorization() {
            // GIVEN
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);

            // WHEN
            final Enumeration<String> headers = underTest.getHeaders(HttpHeaders.AUTHORIZATION);

            // THEN
            assertThat(headers)
                .isNotNull()
                .satisfies(enumeration -> assertThat(enumeration.asIterator())
                    .isNotNull()
                    .toIterable()
                    .isEmpty());
        }

        @Test
        void testShouldReturnHeaders() {
            // GIVEN
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
            request.addHeader(TEST_HEADER, List.of(TEST_HEADER_VALUE_ONE, TEST_HEADER_VALUE_TWO));
            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);

            // WHEN
            final Enumeration<String> headerValues = underTest.getHeaders(TEST_HEADER);

            // THEN
            assertThat(headerValues)
                .isNotNull()
                .satisfies(enumeration -> assertThat(enumeration.asIterator())
                    .isNotNull()
                    .toIterable()
                    .containsExactly(TEST_HEADER_VALUE_ONE, TEST_HEADER_VALUE_TWO));
        }

        @Test
        void testShouldReturnEmptyWhenHeaderIsNotPresent() {
            // GIVEN
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
            request.addHeader(TEST_HEADER, List.of(TEST_HEADER_VALUE_ONE, TEST_HEADER_VALUE_TWO));
            underTest = new HttpServletRequestWithoutAuthenticationHeader(request);

            // WHEN
            final Enumeration<String> headerValues = underTest.getHeaders(HttpHeaders.ACCEPT);

            // THEN
            assertThat(headerValues)
                .isNotNull()
                .satisfies(enumeration -> assertThat(enumeration.asIterator())
                    .isNotNull()
                    .toIterable()
                    .isEmpty());
        }
    }

}
