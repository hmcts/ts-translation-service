package uk.gov.hmcts.reform.translate.customvalue;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

public class ContainsDictionaryTranslationsEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.CONTAINS_DICTIONARY_TRANSLATIONS.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            final String[] expectedValues
                = EvaluatorUtils.extractParameters(key, CustomValueKey.CONTAINS_DICTIONARY_TRANSLATIONS);

            final Map<String, Translation> expectedTranslations = Arrays.stream(expectedValues)
                .map(entry -> {
                    final String[] keyValuePair = entry.split(":");
                    return singletonMap(strip(keyValuePair[0]), new Translation(strip(keyValuePair[1])));
                })
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

    private String strip(final String value) {
        return value.trim().replaceAll("\"", "");
    }
}
