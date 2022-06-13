package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

public class DictionaryTranslationsEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.CONTAINS_DICTIONARY_TRANSLATIONS.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            @SuppressWarnings("unchecked")
            final Map<String, String> translations = (Map<String, String>) ReflectionUtils.deepGetFieldInObject(
                scenarioContext,
                "testData.actualResponse.body.translations"
            );

            final String expectedValueStr = key.toString().replace("containsDictionaryTranslations ", "");

            final Map<String, String> expectedValuesMap = Arrays.stream(expectedValueStr.split(","))
                .map(entry -> {
                    final String[] keyValuePair = entry.split(":");
                    return singletonMap(strip(keyValuePair[0]), strip(keyValuePair[1]));
                })
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

            return translations.entrySet().containsAll(expectedValuesMap.entrySet()) ? translations : expectedValueStr;
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }

    private String strip(final String value) {
        return value.trim().replaceAll("\"", "");
    }
}
