package uk.gov.hmcts.reform.translate.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DictionaryUtilsTest")
class DictionaryUtilsTest {


    @Test
    void testForIsTranslationBodyEmpty() {
        final var dictionaryRequest = new Dictionary();
        final var result = DictionaryUtils.isTranslationBodyEmpty(dictionaryRequest);
        assertTrue(result);
    }

    @Test
    void testForHasAnyTranslations() {
        final var dictionaryRequest = getDictionaryRequestWithoutABody(1, 2);
        final var result = DictionaryUtils.hasAnyTranslations(dictionaryRequest);
        assertFalse(result);
    }

    @Test
    void testForHasTranslationPhrase() {
        final var dictionaryRequest = getDictionaryRequestWithoutABody(1, 2);

        dictionaryRequest.getTranslations()
            .entrySet()
            .forEach(entry -> {
                assertFalse(DictionaryUtils.hasTranslationPhrase(entry));
            });
    }

    private Dictionary getDictionaryRequestWithoutABody(int from, int to) {
        final Map<String, Translation> expectedMap = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMap.put("english_" + i, new Translation("")));
        return new Dictionary(expectedMap);
    }
}
