package uk.gov.hmcts.reform.translate.helper;

import lombok.val;
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
                assertFalse(DictionaryUtils.hasTranslationPhrase(entry));
            });
    }

    private Dictionary getDictionaryRequestWithoutABody(int from, int to) {
        final Map<String, Translation> expectedMap = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMap.put("english_" + i, new Translation("")));
        return new Dictionary(expectedMap);
    }
}
