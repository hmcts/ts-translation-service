package uk.gov.hmcts.reform.translate.customvalue;

import java.util.Arrays;

public enum CustomValueKey {
    CONTAINS_DICTIONARY_FROM_CONTEXT("containsDictionaryFromContext"),
    CONTAINS_DICTIONARY_TRANSLATIONS("containsDictionaryTranslations"),
    PHRASES_FROM_CONTEXT_TRANSLATIONS("phrasesFromContextTranslations"),
    CONTAINS_TRANSLATIONS_FROM_CONTEXT_AS_TRANSLATE_RESPONSE("containsTranslationsFromContextAsTranslateResponse"),
    UNIQUE_TEST_PHRASE("UniqueTestPhrase"),
    UNIQUE_TRANSLATION_WITH_ENGLISH_AND_WELSH("UniqueTranslationWithEnglishAndWelsh"),
    UNIQUE_TRANSLATION_WITH_ONLY_ENGLISH("UniqueTranslationWithOnlyEnglish"),
    CONTAINS_TEST_TRANSLATIONS("containsTestTranslations"),
    DEFAULT_KEY("DefaultKey");

    private final String value;

    CustomValueKey(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static CustomValueKey getEnum(final String value) {
        return Arrays.stream(values())
            .filter(key -> value.startsWith(key.getValue()))
            .findFirst()
            .orElse(DEFAULT_KEY);
    }
}
