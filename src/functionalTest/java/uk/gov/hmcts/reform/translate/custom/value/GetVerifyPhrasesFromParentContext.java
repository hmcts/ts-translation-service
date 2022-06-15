package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Map;

public class GetVerifyPhrasesFromParentContext implements CustomValueEvaluator {
    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.GET_VERIFY_PHRASES_FROM_PARENT_CONTEXT.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {

            @SuppressWarnings("unchecked") final Map<String, String> translations =
                (Map<String, String>) ReflectionUtils.deepGetFieldInObject(
                scenarioContext,
                "parentContext.testData.request.body.translations"
            );

            return translations.keySet();
        } catch (Exception e) {
            throw new FunctionalTestException("Problem generating custom value for <evaluator name>: ", e);        }
    }
}
