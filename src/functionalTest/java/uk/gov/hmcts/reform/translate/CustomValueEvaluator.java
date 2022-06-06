package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

public interface CustomValueEvaluator {
    Boolean matches(final CustomValueKey key);

    Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key);
}
