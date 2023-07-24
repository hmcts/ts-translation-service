package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang3.RandomStringUtils;
import org.jooq.lambda.Unchecked;
import org.springframework.util.CollectionUtils;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.Map;

import static uk.gov.hmcts.reform.translate.service.DictionaryService.TEST_PHRASES_START_WITH;

public final class EvaluatorUtils {

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private EvaluatorUtils() {
    }

    @SuppressWarnings("unchecked")
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
        String expected = expectedTranslations.entrySet().stream().map(e -> {
            String result = e.getKey() + ":";
            if (e.getValue() instanceof Translation tran) {
                result += tran.getTranslation();
            } else if (e.getValue() instanceof String str) {
                result += str;
            } else if (e.getValue() instanceof Map map) {
                result += map.getOrDefault("translation","");
            }
            return result;

        }).reduce("", (partialString, element) -> partialString + element);

        String actual = actualTranslations.entrySet().stream()
            .map(e -> {
                String result = e.getKey() + ":";
                if (e.getValue() instanceof Translation tran) {
                    result += tran.getTranslation();
                } else if (e.getValue() instanceof String str) {
                    result += str;
                } else if (e.getValue() instanceof Map map) {
                    result += map.getOrDefault("translation","");
                }
                return result;
            }).reduce("", (partialString, element) -> partialString + element);

        BeftaUtils.defaultLog("Expected:" + expected);
        BeftaUtils.defaultLog("Actual:" + actual);

        // if 'actual-response' contains all 'expected-translations':
        //    then return actual-response translations to ensure BEFTA assert passes
        //    otherwise return only expected-translations to cause befta assert failure
        return actualTranslations.entrySet().containsAll(expectedTranslations.entrySet())
            ? actualTranslations : expectedTranslations;
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
        return String.format("%s%s-%s", TEST_PHRASES_START_WITH, marker, RandomStringUtils.randomAlphabetic(count));
    }

}
