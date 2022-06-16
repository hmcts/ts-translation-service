package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Map;

public class ContainsUniquePhrasesParentContext implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.CONTAINS_UNIQUE_PHRASES_PARENT_CONTEXT.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            @SuppressWarnings("unchecked")
            final Map<String, String> translations = (Map<String, String>) ReflectionUtils.deepGetFieldInObject(
                scenarioContext,
                "testData.actualResponse.body.translations"
            );

            @SuppressWarnings("unchecked") final Map<String, String> expectedTranslations =
                (Map<String, String>) ReflectionUtils.deepGetFieldInObject(
                    scenarioContext,
                    "parentContext.testData.request.body.translations"
                );

            return translations.entrySet().containsAll(expectedTranslations.entrySet()) ? translations : expectedTranslations;
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }
}
