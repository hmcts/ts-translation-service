package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.StringGenerator;

import java.util.Map;

public class UniqueTranslationWithEnglishAndWelshEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TRANSLATION_WITH_ENGLISH_AND_WELSH.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String englishPhrase = StringGenerator.generate();
        final String welshPhrase = englishPhrase + "-WELSH";
        return Map.of(englishPhrase, welshPhrase);
    }
}
