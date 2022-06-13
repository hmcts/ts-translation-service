package uk.gov.hmcts.reform.translate;

import org.apache.commons.lang3.RandomStringUtils;

public final class StringGenerator {
    private StringGenerator(){

    }

    public static String generate(){
        final int count = 10;
        return "TEST-" + RandomStringUtils.randomAlphabetic(count);
    }
}
