package uk.gov.hmcts.reform.translate.errorhandling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.DICTIONARY_URL;

class HttpErrorTest {

    @ParameterizedTest
    @DisplayName("Should initialise HttpError with defaults")
    @MethodSource("provideHttpErrors")
    void shouldInitialiseHttpErrorWithDefaults(HttpError<String> httpError) {
        assertEquals("Should record exception name", RuntimeException.class.getName(), httpError.getException());
        assertNotNull("Should record a date & time", httpError.getTimestamp());
        assertEquals("Should record path", DICTIONARY_URL, httpError.getPath());
        assertEquals("Should record DEFAULT status", HttpError.DEFAULT_STATUS, httpError.getStatus());
        assertEquals("Should record DEFAULT error", HttpError.DEFAULT_ERROR, httpError.getError());
    }

    private static Stream<Arguments> provideHttpErrors() {
        RuntimeException ex = new RuntimeException();
        HttpServletRequest httpServletRequest = new MockHttpServletRequest("POST", DICTIONARY_URL);
        ServletWebRequest servletWebRequest = new ServletWebRequest(httpServletRequest);
        return Stream.of(
            Arguments.of(
                new HttpError<String>(ex, DICTIONARY_URL, null)
            ),
            Arguments.of(
                new HttpError<String>(ex, httpServletRequest, null)
            ),
            Arguments.of(
                new HttpError<String>(ex, servletWebRequest, null)
            ),
            Arguments.of(
                new HttpError<String>(ex, httpServletRequest)
            )
        );
    }

    @Test
    @DisplayName("Should initialise HttpError with defaults using HttpServletRequest")
    void shouldInitialiseHttpErrorWithDefaultsUsingHttpServletRequest() {
        // GIVEN
        RuntimeException ex = new RuntimeException();

        HttpServletRequest request = new MockHttpServletRequest("POST", DICTIONARY_URL);

        // WHEN
        HttpError<String> httpError = new HttpError<>(ex, request, null);

        // THEN
        assertEquals("Should record exception name", RuntimeException.class.getName(), httpError.getException());
        assertNotNull("Should record a date & time", httpError.getTimestamp());
        assertEquals("Should record path", DICTIONARY_URL, httpError.getPath());
        assertEquals("Should record DEFAULT status", HttpError.DEFAULT_STATUS, httpError.getStatus());
        assertEquals("Should record DEFAULT error", HttpError.DEFAULT_ERROR, httpError.getError());
    }


    @Test
    @DisplayName("Should initialise HttpError from supplied HttpStatus")
    void shouldInitialiseHttpErrorFromSuppliedStatus() {

        // GIVEN
        RuntimeException ex = new RuntimeException();
        HttpStatus status = HttpStatus.I_AM_A_TEAPOT;

        // WHEN
        HttpError<String> httpError = new HttpError<>(ex, DICTIONARY_URL, status);

        // THEN
        assertEquals("Should record supplied status", status.value(), httpError.getStatus());
        assertEquals("Should record supplied status as error", status.getReasonPhrase(), httpError.getError());
    }

    @Test
    @DisplayName("Should initialise HttpError from defaults if exception's ResponseStatus blank")
    void shouldInitialiseHttpErrorFromDefaultStatusIfExceptionsResponseStatusBlank() {

        // GIVEN
        TestResponseStatusThatsBlankException ex = new TestResponseStatusThatsBlankException("test");

        // WHEN
        HttpError<String> httpError =
            new HttpError<>(ex, DICTIONARY_URL, null);

        // THEN
        assertEquals("Should record DEFAULT status", HttpError.DEFAULT_STATUS, httpError.getStatus());
        assertEquals("Should record DEFAULT error", HttpError.DEFAULT_ERROR, httpError.getError());
    }

    @Test
    @DisplayName("Should initialise HttpError from defaults and exception's ResponseStatus Reason")
    void shouldInitialiseHttpErrorFromDefaultStatusAndExceptionsResponseStatusReason() {

        // GIVEN
        TestResponseStatusWithReasonException ex = new TestResponseStatusWithReasonException("test");

        // WHEN
        HttpError<String> httpError =
            new HttpError<>(ex, DICTIONARY_URL, null);

        // THEN
        assertEquals("Should record DEFAULT status",
                     HttpError.DEFAULT_STATUS, httpError.getStatus());
        assertEquals("Should record exception's ResponseStatus.Reason",
                     TestResponseStatusWithReasonException.REASON, httpError.getError());
    }

    @Test
    @DisplayName("Should initialise HttpError from exception's ResponseStatus Code")
    void shouldInitialiseHttpErrorFromExceptionsResponseStatusCode() {

        // GIVEN
        TestResponseStatusWithCodeException ex = new TestResponseStatusWithCodeException("test");

        // WHEN
        HttpError<String> httpError =
            new HttpError<>(ex, DICTIONARY_URL, null);

        // THEN
        assertEquals("Should record exception's ResponseStatus Code",
                     TestResponseStatusWithCodeException.CODE.value(), httpError.getStatus());
        assertEquals("Should record exception's ResponseStatus Code as error",
                     TestResponseStatusWithCodeException.CODE.getReasonPhrase(), httpError.getError());
    }

    @Test
    @DisplayName("Should initialise HttpError from exception's ResponseStatus Value")
    void shouldInitialiseHttpErrorFromExceptionsResponseStatusValue() {

        // GIVEN
        TestResponseStatusWithValueException ex = new TestResponseStatusWithValueException("test");

        // WHEN
        HttpError<String> httpError =
            new HttpError<>(ex, DICTIONARY_URL, null);

        // THEN
        assertEquals("Should record exception's ResponseStatus Code",
                     TestResponseStatusWithValueException.VALUE.value(), httpError.getStatus());
        assertEquals("Should record exception's ResponseStatus Code as error",
                     TestResponseStatusWithValueException.VALUE.getReasonPhrase(), httpError.getError());
    }

    @ResponseStatus()
    private static class TestResponseStatusThatsBlankException extends ApiException {
        public TestResponseStatusThatsBlankException(String message) {
            super(message);
        }
    }

    @ResponseStatus(reason = TestResponseStatusWithReasonException.REASON)
    private static class TestResponseStatusWithReasonException extends ApiException {
        public static final String REASON = "Test Reason";

        public TestResponseStatusWithReasonException(String message) {
            super(message);
        }
    }

    @ResponseStatus(code = HttpStatus.I_AM_A_TEAPOT)
    private static class TestResponseStatusWithCodeException extends ApiException {
        public static final HttpStatus CODE = HttpStatus.I_AM_A_TEAPOT;

        public TestResponseStatusWithCodeException(String message) {
            super(message);
        }
    }

    @ResponseStatus(value = HttpStatus.I_AM_A_TEAPOT)
    private static class TestResponseStatusWithValueException extends ApiException {
        public static final HttpStatus VALUE = HttpStatus.I_AM_A_TEAPOT;

        public TestResponseStatusWithValueException(String message) {
            super(message);
        }
    }
}
