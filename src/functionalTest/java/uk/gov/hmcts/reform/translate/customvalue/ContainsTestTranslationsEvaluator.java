package uk.gov.hmcts.reform.translate.customvalue;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

public class ContainsTestTranslationsEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.CONTAINS_TEST_TRANSLATIONS.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            @SuppressWarnings("unchecked")
            final List<String> phrases = (List<String>) ReflectionUtils.deepGetFieldInObject(
                scenarioContext,
                "testData.request.body.phrases"
            );

            Map<String,Object> expectedTranslations = phrases.stream()
                .map(phrase -> singletonMap(phrase, new Translation(phrase)))
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

            return EvaluatorUtils.calculateDictionaryFromActualResponseAndExpectedTranslations(
                    scenarioContext,
                    expectedTranslations
                );
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }
}
