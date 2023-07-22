package uk.gov.hmcts.reform.translate.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.Map;
import java.util.function.Predicate;


public final class DictionaryUtils {

    private static final Predicate<Translation> isTranslationNotNull = translation -> translation != null && (
            StringUtils.isNotEmpty(translation.getTranslation())
            || StringUtils.isNotEmpty(translation.getYes())
            || StringUtils.isNotEmpty(translation.getNo())
        );

    public static boolean isTranslationBodyEmpty(final Dictionary dictionaryRequest) {
        return CollectionUtils.isEmpty(dictionaryRequest.getTranslations());
    }

    public static boolean hasAnyTranslations(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations().values().stream().anyMatch(isTranslationNotNull);
    }

    public static boolean hasTranslationPhrase(final Map.Entry<String, Translation> currentPhrase) {
        return isTranslationNotNull.test(currentPhrase.getValue());
    }

    public static boolean shouldSetYesOrNo(final Map.Entry<String, Translation> currentPhrase,
                                           final DictionaryEntity dictionaryEntity) {
        return currentPhrase.getValue() != null && (currentPhrase.getValue().isYesOrNo() || dictionaryEntity.isYesOrNo());
    }

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private DictionaryUtils() {
    }
}
