package uk.gov.hmcts.reform.translate.helper;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.translate.model.Dictionary;

import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings({"PMD", "checkstyle:hideutilityclassconstructor"})
public class DictionaryUtils {

    private static final Predicate<String> isTranslationNotNull = translation -> !StringUtils.isEmpty(translation);

    public static boolean isTranslationBodyEmpty(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations() == null || dictionaryRequest.getTranslations().isEmpty();
    }

    public static boolean hasAnyTranslations(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations().values().stream().anyMatch(isTranslationNotNull);
    }

    public static boolean hasAnyTranslation(final Map.Entry<String, String> currentPhrase) {
        return isTranslationNotNull.test(currentPhrase.getValue());
    }
}
