package uk.gov.hmcts.reform.translate;

import java.util.Arrays;

public enum CustomValueKey {
    UNIQUE_STRING("UniqueString"),
    UNIQUE_TRANSLATION("UniqueTranslation"),
    UNIQUE_LOAD_TRANSLATION("UniqueLoadTranslation"),
    CONTAINS_DICTIONARY_TRANSLATIONS("containsDictionaryTranslations"),
    GET_UNIQUE_TRANSLATIONS("getUniqueTranslations"),
    GET_EXPECTED_TRANSLATION_RESPONSE("getExpectedTranslationResponse"),
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
