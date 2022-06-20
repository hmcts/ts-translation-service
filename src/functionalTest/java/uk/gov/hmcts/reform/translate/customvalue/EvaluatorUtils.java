package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang3.RandomStringUtils;

public final class EvaluatorUtils {

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private EvaluatorUtils() {
    }

    public static String generate() {
        final int count = 10;
        return "TEST-" + RandomStringUtils.randomAlphabetic(count);
    }
}
