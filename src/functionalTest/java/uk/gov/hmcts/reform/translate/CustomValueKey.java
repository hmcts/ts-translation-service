package uk.gov.hmcts.reform.translate;

import java.util.Arrays;

public enum CustomValueKey {
    UNIQUE_STRING("UniqueString"),
    UNIQUE_TRANSLATION("UniqueTranslation"),
    CONTAINS_DICTIONARY_TRANSLATIONS("containsDictionaryTranslations"),
    S_003_1("S-003.1"),
    S_004_1("S-004.1"),
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
