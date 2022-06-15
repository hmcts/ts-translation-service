package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Map;

public class GetUniqueTranslationsEvaluator implements CustomValueEvaluator {
    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.GET_UNIQUE_TRANSLATIONS.equals(key);
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
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }
}
