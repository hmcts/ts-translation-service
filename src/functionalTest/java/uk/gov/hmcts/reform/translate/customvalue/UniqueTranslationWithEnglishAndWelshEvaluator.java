package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.StringGenerator;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;

public class UniqueTranslationWithEnglishAndWelshEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TRANSLATION_WITH_ENGLISH_AND_WELSH.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        final String expectedValueStr = key.toString().replace("UniqueTranslationWithEnglishAndWelsh ", "");
        System.out.println("sdansdjaksjdkajs"+expectedValueStr);
        int uniquePhrasesAmount = Integer.parseInt(expectedValueStr);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i<uniquePhrasesAmount; i++) {
            String englishPhrase = StringGenerator.generate();
            final String welshPhrase = englishPhrase + "-WELSH";
            map.put(englishPhrase, welshPhrase);
        }
        return map;
    }
}
