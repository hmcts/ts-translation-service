package uk.gov.hmcts.reform.translate.custom.value;

import java.util.Arrays;

public enum CustomValueKey {
    UNIQUE_STRING("UniqueString"),
    UNIQUE_TRANSLATION_WITH_ENGLISH_AND_WELSH("UniqueTranslationWithEnglishAndWelsh"),
    UNIQUE_TRANSLATION_WITH_ONLY_ENGLISH("UniqueTranslationWithOnlyEnglish"),
    CONTAINS_DICTIONARY_TRANSLATIONS("containsDictionaryTranslations"),
    CONTAINS_UNIQUE_PHRASES_PARENT_CONTEXT("containsUniquePhrasesParentContext"),
    S_003_1("S-003.1"),
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
