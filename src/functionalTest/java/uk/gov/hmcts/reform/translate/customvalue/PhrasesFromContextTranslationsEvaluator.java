package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class PhrasesFromContextTranslationsEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.PHRASES_FROM_CONTEXT_TRANSLATIONS.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            String contextPath = EvaluatorUtils.extractParameter(
                key, CustomValueKey.PHRASES_FROM_CONTEXT_TRANSLATIONS
            );

            if (StringUtils.isEmpty(contextPath)) {
                contextPath = "parentContext.testData.request.body.translations";
            }

            @SuppressWarnings("unchecked")
            final Map<String, Object> translations =
                (Map<String, Object>) ReflectionUtils.deepGetFieldInObject(scenarioContext, contextPath);

            if (translations == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(translations.keySet());
        } catch (Exception e) {
            throw new FunctionalTestException("Problem building phrases list from context: ", e);
        }
    }
}
