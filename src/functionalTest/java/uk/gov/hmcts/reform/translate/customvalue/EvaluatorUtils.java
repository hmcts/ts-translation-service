package uk.gov.hmcts.reform.translate.customvalue;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.CollectionUtils;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Map;

import static uk.gov.hmcts.reform.translate.service.DictionaryService.TEST_PHRASES_START_WITH;

public final class EvaluatorUtils {

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private EvaluatorUtils() {
    }

    public static Map<String, String> calculateDictionaryFromActualResponseAndExpectedTranslations(
        BackEndFunctionalTestScenarioContext scenarioContext,
        Map<String, String> expectedTranslations) {

        if (CollectionUtils.isEmpty(expectedTranslations)) {
            // throw a more useful error message if generated/found expected-translations list is empty
            throw new FunctionalTestException("The set of expected translations to search for cannot be empty.");
        }

        final Map<String, String> actualTranslations = extractMapFromContext(
            scenarioContext,
            "testData.actualResponse.body.translations"
        );

        // if 'actual-response' contains all 'expected-translations':
        //    then return actual-response translations to ensure BEFTA assert passes
        //    otherwise return only expected-translations to cause befta assert failure
        return actualTranslations.entrySet().containsAll(expectedTranslations.entrySet())
            ? actualTranslations : expectedTranslations;
    }

    public static Map<String, String> extractMapFromContext(
        BackEndFunctionalTestScenarioContext scenarioContext,
        String fieldPath) {

        try {
            @SuppressWarnings("unchecked")
            final Map<String, String> extractedMap
                = (Map<String, String>) ReflectionUtils.deepGetFieldInObject(scenarioContext, fieldPath);

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
