package uk.gov.hmcts.reform.translate.customvalue;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

public class UniqueTestPhraseEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TEST_PHRASE.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        final String marker =  EvaluatorUtils.extractParameter(key, CustomValueKey.UNIQUE_TEST_PHRASE);
        return EvaluatorUtils.generateTestPhrase(marker);
    }
}
