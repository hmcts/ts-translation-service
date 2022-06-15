package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

public class UniqueStringEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_STRING.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return StringGenerator.generate();
    }
}
