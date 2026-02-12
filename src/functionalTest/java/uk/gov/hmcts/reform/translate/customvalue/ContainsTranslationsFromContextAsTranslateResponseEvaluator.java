package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContainsTranslationsFromContextAsTranslateResponseEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.CONTAINS_TRANSLATIONS_FROM_CONTEXT_AS_TRANSLATE_RESPONSE.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            String contextPath = EvaluatorUtils.extractParameter(
                key, CustomValueKey.CONTAINS_TRANSLATIONS_FROM_CONTEXT_AS_TRANSLATE_RESPONSE
            );

            if (StringUtils.isEmpty(contextPath)) {
                contextPath = "parentContext.testData.request.body.translations";
            }

            @SuppressWarnings("unchecked")
            final Map<String, Object> requestTranslations =
                (Map<String, Object>) ReflectionUtils.deepGetFieldInObject(scenarioContext, contextPath);

            Map<String, Object> expectedTranslations = new LinkedHashMap<>();
            if (requestTranslations != null) {
                for (Map.Entry<String, Object> entry : requestTranslations.entrySet()) {
                    String englishPhrase = entry.getKey();
                    Object value = entry.getValue();

                    String expectedTranslation = null;
                    if (value instanceof Translation translation) {
                        expectedTranslation = translation.getTranslation();
                    } else if (value instanceof Map map) {
                        Object t = map.get("translation");
                        if (t != null) {
                            expectedTranslation = t.toString();
                        }
                    } else if (value instanceof String str) {
                        expectedTranslation = str;
                    }

                    if (StringUtils.isEmpty(expectedTranslation)) {
                        expectedTranslation = englishPhrase;
                    }

                    expectedTranslations.put(englishPhrase, new Translation(expectedTranslation));
                }
            }

            return EvaluatorUtils.calculateDictionaryFromActualResponseAndExpectedTranslations(
                scenarioContext,
                expectedTranslations
            );
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking translate response payload: ", e);
        }
    }
}
