package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.CollectionUtils;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.translate.service.DictionaryService.TEST_PHRASES_START_WITH;

public final class EvaluatorUtils {

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private EvaluatorUtils() {
    }

    public static Map<String, Object> calculateDictionaryFromActualResponseAndExpectedTranslations(
        BackEndFunctionalTestScenarioContext scenarioContext,
        Map<String, Object> expectedTranslations) {

        if (CollectionUtils.isEmpty(expectedTranslations)) {
            // throw a more useful error message if generated/found expected-translations list is empty
            throw new FunctionalTestException("The set of expected translations to search for cannot be empty.");
        }

        final Map<String, Object> actualTranslations = extractMapFromContext(
            scenarioContext,
            "testData.actualResponse.body.translations"
        );

        // if 'actual-response' contains all 'expected-translations':
        //    then return actual-response translations to ensure BEFTA assert passes
        //    otherwise return only expected-translations to cause befta assert failure
        boolean matches = createComparableTranslation(actualTranslations).entrySet()
            .containsAll(createComparableTranslation(expectedTranslations).entrySet());

        if (matches) {
            return actualTranslations;
        }
        BeftaUtils.defaultLog("Failed to match");
        return expectedTranslations;
    }

    public static Map<String, Object> extractMapFromContext(
        BackEndFunctionalTestScenarioContext scenarioContext,
        String fieldPath) {

        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> extractedMap
                = (Map<String, Object>) ReflectionUtils.deepGetFieldInObject(scenarioContext, fieldPath);

            return extractedMap;

        } catch (Exception e) {
            throw new FunctionalTestException(String.format("Problem reading map from context: %s : ", fieldPath), e);
        }
    }

    public static String extractParameter(Object key, CustomValueKey customValueKey) {
        final String keyAsString = key.toString();

        // if no parameters return empty
        if (customValueKey.getValue().equals(keyAsString)) {
            return "";
        }

        return keyAsString.replace(customValueKey + " ", "");
    }

    public static String[] extractParameters(Object key, CustomValueKey customValueKey) {
        return extractParameter(key, customValueKey).split(",");
    }

    public static String generateTestPhrase(String marker) {
        final int count = 10;
        return String.format("%s%s-%s", TEST_PHRASES_START_WITH, marker, 
            RandomStringUtils.secure().nextAlphabetic(count));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String,String> createComparableTranslation(Map<String,Object> input) {

        return input.entrySet().stream().map(e -> {
            String result = "";
            switch (e.getValue()) {
                case Translation tran -> result += tran.getTranslation();
                case String str -> result += str;
                case Map map -> result += map.getOrDefault("translation","");
                default -> { }
            }
            return Collections.singletonMap(e.getKey(), result);
        }).flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
