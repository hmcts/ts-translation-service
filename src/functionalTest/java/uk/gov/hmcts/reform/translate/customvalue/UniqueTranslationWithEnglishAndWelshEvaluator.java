package uk.gov.hmcts.reform.translate.customvalue;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.HashMap;
import java.util.Map;

public class UniqueTranslationWithEnglishAndWelshEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TRANSLATION_WITH_ENGLISH_AND_WELSH.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {

        final String[] parameters
                = EvaluatorUtils.extractParameters(key, CustomValueKey.UNIQUE_TRANSLATION_WITH_ENGLISH_AND_WELSH);

        int paramCount = parameters.length;
        String scenarioMarker = paramCount > 0 ? parameters[0] : "";
        int uniquePhrasesAmount = paramCount > 1 ? Integer.parseInt(parameters[1]) : 1;

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < uniquePhrasesAmount; i++) {
            String englishPhrase = EvaluatorUtils.generateTestPhrase(String.format("%s-%s", scenarioMarker, i + 1));
            final String welsh = englishPhrase + "-WELSH";
            map.put(englishPhrase, new Translation(welsh).toString());
        }
        return map;
    }
}
