package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.reform.translate.customvalue.ContainsDictionaryFromContextEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.ContainsDictionaryTranslationsEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.ContainsTestTranslationsEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.ContainsTranslationsFromContextAsTranslateResponseEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.CustomValueEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.CustomValueKey;
import uk.gov.hmcts.reform.translate.customvalue.PhrasesFromContextTranslationsEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.UniqueTestPhraseEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.UniqueTranslationWithEnglishAndWelshEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.UniqueTranslationWithOnlyEnglishEvaluator;

import java.util.List;

public class TranslationServiceTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private final List<CustomValueEvaluator> customValueEvaluators = List.of(
            new ContainsDictionaryTranslationsEvaluator(),
            new ContainsDictionaryFromContextEvaluator(),
            new PhrasesFromContextTranslationsEvaluator(),
            new ContainsTranslationsFromContextAsTranslateResponseEvaluator(),
            new UniqueTestPhraseEvaluator(),
            new ContainsTestTranslationsEvaluator(),
            new UniqueTranslationWithEnglishAndWelshEvaluator(),
            new UniqueTranslationWithOnlyEnglishEvaluator()
    );

    @Override
    public synchronized Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        if (key.toString().startsWith("no_dynamic_injection_")) {
            return key.toString().replace("no_dynamic_injection_","");
        } else if (key.toString().startsWith("approximately ")) {
            try {
                String actualSizeFromHeaderStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.headers.Content-Length");
                String expectedSizeStr = key.toString().replace("approximately ", "");

                int actualSize =  Integer.parseInt(actualSizeFromHeaderStr);
                int expectedSize = Integer.parseInt(expectedSizeStr);

                if (Math.abs(actualSize - expectedSize) < (actualSize * 10 / 100)) {
                    return actualSizeFromHeaderStr;
                }
                return expectedSize;
            } catch (Exception e) {
                throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
            }
        } else if (key.toString().startsWith("contains ")) {
            try {
                String actualValueStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                     "testData.actualResponse.body.__plainTextValue__");
                String expectedValueStr = key.toString().replace("contains ", "");

                if (actualValueStr.contains(expectedValueStr)) {
                    return actualValueStr;
                }
                return "expectedValueStr " + expectedValueStr + " not present in response ";
            } catch (Exception e) {
                throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
            }
        } else {
            return customValueEvaluators.stream()
                .filter(candidate -> candidate.matches(CustomValueKey.getEnum(key.toString())))
                .findFirst()
                .map(evaluator -> evaluator.calculate(scenarioContext, key))
                .orElseGet(() -> super.calculateCustomValue(scenarioContext, key));
        }
    }

    @Override
    public BeftaTestDataLoader getDataLoader() {
        return new TranslationServiceTestDataLoader();
    }
}
