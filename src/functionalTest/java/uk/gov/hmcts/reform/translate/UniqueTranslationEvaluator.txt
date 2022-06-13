package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

import java.util.Map;

public class UniqueTranslationEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TRANSLATION.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String englishPhrase = StringGenerator.generate();
        final String welshPhrase = englishPhrase + "-WELSH";
        return Map.of(englishPhrase, welshPhrase);
    }
}
