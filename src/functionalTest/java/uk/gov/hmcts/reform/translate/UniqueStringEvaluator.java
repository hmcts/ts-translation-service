package uk.gov.hmcts.reform.translate;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

import java.util.Map;

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
