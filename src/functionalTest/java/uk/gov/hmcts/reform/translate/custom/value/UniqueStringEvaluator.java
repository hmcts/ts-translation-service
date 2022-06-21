package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.StringGenerator;

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
