package uk.gov.hmcts.reform.translate.controllers;

public final class ControllerConstants {
    // DictionaryController
    public static final String DICTIONARY_URL = "/dictionary";
    public static final String TRANSLATIONS_URL = "/translation/cy";

    // TestingSupportController
    public static final String TESTING_SUPPORT_URL = "/testing-support";
    public static final String TEST_PHRASES_URL = "/dictionary/test-phrases";

    private ControllerConstants() {
        // Hide Utility Class Constructor :
        // Utility classes should not have a public or default constructor (squid:S1118)
    }
}
