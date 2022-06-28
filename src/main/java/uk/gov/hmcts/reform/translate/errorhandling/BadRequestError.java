package uk.gov.hmcts.reform.translate.errorhandling;

public class BadRequestError {

    public static final String BAD_SCHEMA = "Bad Request (001 bad schema)";

    public static final String WELSH_NOT_ALLOWED =
        "Bad Request (002 Welsh not allowed for this user)";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private BadRequestError() {
    }
}
