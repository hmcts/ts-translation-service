package uk.gov.hmcts.reform.translate.errorhandling;

/**
 * Authentication and Authorisation error messages.
 */
public class AuthError {
    public static final String AUTHENTICATION_TOKEN_INVALID
        = "Authentication failure due to invalid / expired tokens (IDAM / S2S).";

    public static final String UNAUTHORISED_S2S_SERVICE = "Unauthorised S2S service.";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private AuthError() {
    }
}
