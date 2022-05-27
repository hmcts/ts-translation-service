package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TranslationServiceTestAutomationAdapter extends DefaultTestAutomationAdapter {

    @Override
    public synchronized Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {

        if (key.toString().startsWith("containsDictionaryTranslations")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> translations = (Map<String, String>) ReflectionUtils.deepGetFieldInObject(
                    scenarioContext,
                    "testData.actualResponse.body.translations"
                );

                String expectedValueStr = key.toString().replace("containsDictionaryTranslations ", "");

                Map<String, String> expectedValuesMap = new HashMap<>();

                Arrays.stream(expectedValueStr.split(",")).toList().forEach(s -> {
                    String[] keyValuePair = s.split(":");
                    expectedValuesMap.put(keyValuePair[0].trim(),
                                          keyValuePair[1].trim());
                });


                boolean allMatch = expectedValuesMap.entrySet().stream().allMatch(entrySet ->
                    translations.containsKey(entrySet.getKey()) && translations.containsValue(entrySet.getValue())
                );

                if (allMatch) {
                    return translations;
                } else {
                    return expectedValueStr;
                }
            } catch (Exception e) {
                throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
            }
        }

        return super.calculateCustomValue(scenarioContext, key);
    }

    @Override
    public BeftaTestDataLoader getDataLoader() {
        return new DefaultBeftaTestDataLoader() {
            @Override
            public void doLoadTestData() {

            }

            @Override
            public boolean isTestDataLoadedForCurrentRound() {
                return false;
            }

            @Override
            public void loadDataIfNotLoadedVeryRecently() {

            }
        };
    }
}
