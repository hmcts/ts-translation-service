package uk.gov.hmcts.reform.translate;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

public class UniqueStringEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_STRING.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        final int COUNT = 10;
        return "TEST-" + RandomStringUtils.randomAlphabetic(COUNT);
    }
}
