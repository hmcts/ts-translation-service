package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

import java.util.Map;

public class UniqueLoadTranslationEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_LOAD_TRANSLATION.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return Map.of(StringGenerator.generate(), "");
    }
}
