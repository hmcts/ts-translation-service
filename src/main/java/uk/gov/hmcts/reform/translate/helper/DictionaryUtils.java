package uk.gov.hmcts.reform.translate.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.translate.model.Dictionary;

import java.util.Map;
import java.util.function.Predicate;


public final class DictionaryUtils {

    private static final Predicate<String> isTranslationNotNull = translation -> !StringUtils.isEmpty(translation);

    public static boolean isTranslationBodyEmpty(final Dictionary dictionaryRequest) {
        return CollectionUtils.isEmpty(dictionaryRequest.getTranslations());
    }

    public static boolean hasAnyTranslations(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations().values().stream().anyMatch(isTranslationNotNull);
    }

    public static boolean hasTranslationPhrase(final Map.Entry<String, String> currentPhrase) {
        return isTranslationNotNull.test(currentPhrase.getValue());
    }

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private DictionaryUtils() {
    }
}
