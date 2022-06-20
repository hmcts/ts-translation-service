package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.custom.value.CustomValueEvaluator;
import uk.gov.hmcts.reform.translate.custom.value.CustomValueKey;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

public class GetTranslationsEvaluator implements CustomValueEvaluator {
    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.S_003_1.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            @SuppressWarnings("unchecked")
            final List<String> phrases = (List<String>) ReflectionUtils.deepGetFieldInObject(
                scenarioContext,
                "testData.request.body.phrases"
            );

            return phrases.stream()
                .map(phrase -> singletonMap(phrase, phrase))
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }
}