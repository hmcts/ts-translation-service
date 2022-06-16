package uk.gov.hmcts.reform.translate.helper;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.translate.model.Dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DictionaryMapperTest")
class DictionaryUtilsTest {


    @Test
    void testForIsTranslationBodyEmpty() {
        val dictionaryRequest = new Dictionary();
        val result = DictionaryUtils.isTranslationBodyEmpty(dictionaryRequest);
        assertTrue(result);
    }

    @Test
    void testForHasAnyTranslations() {
        val dictionaryRequest = getDictionaryRequestWithoutABody(1, 2);
        val result = DictionaryUtils.hasAnyTranslations(dictionaryRequest);
        assertFalse(result);
    }

    @Test
    void testForHasTranslationPhrase() {
        val dictionaryRequest = getDictionaryRequestWithoutABody(1, 2);

        dictionaryRequest.getTranslations()
            .entrySet()
            .forEach(entry -> {
                DictionaryUtils.hasTranslationPhrase(entry);
            });
    }

    private Dictionary getDictionaryRequestWithoutABody(int from, int to) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, null));
        return new Dictionary(expectedMapKeysAndValues);
    }
}
