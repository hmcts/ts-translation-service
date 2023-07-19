package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.Map;

public class ContainsDictionaryFromContextEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.CONTAINS_DICTIONARY_FROM_CONTEXT.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {

            String contextPath =  EvaluatorUtils.extractParameter(key, CustomValueKey.CONTAINS_DICTIONARY_FROM_CONTEXT);

            // default path to read from ParentContext if not set
            if (StringUtils.isEmpty(contextPath)) {
                contextPath = "parentContext.testData.request.body.translations";
            }

            @SuppressWarnings("unchecked")
            final Map<String, Translation> expectedTranslations =
                (Map<String, Translation>) ReflectionUtils.deepGetFieldInObject(scenarioContext, contextPath);

            return EvaluatorUtils.calculateDictionaryFromActualResponseAndExpectedTranslations(
                scenarioContext,
                expectedTranslations
            );

        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }
}
