package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.customvalue.ContainsDictionaryFromContextEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.ContainsDictionaryTranslationsEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.CustomValueEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.CustomValueKey;
import uk.gov.hmcts.reform.translate.customvalue.GetTranslationsEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.UniqueTestPhraseEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.UniqueTranslationWithEnglishAndWelshEvaluator;
import uk.gov.hmcts.reform.translate.customvalue.UniqueTranslationWithOnlyEnglishEvaluator;

import java.util.stream.Stream;

public class TranslationServiceTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private final CustomValueEvaluator
        containsDictionaryTranslationsEvaluator = new ContainsDictionaryTranslationsEvaluator();
    private final CustomValueEvaluator
        containsDictionaryFromContextEvaluator = new ContainsDictionaryFromContextEvaluator();

    private final CustomValueEvaluator uniqueTestPhraseEvaluator = new UniqueTestPhraseEvaluator();
    private final CustomValueEvaluator getTranslationsEvaluator = new GetTranslationsEvaluator();

    private final CustomValueEvaluator
        uniqueTranslationWithEnglishAndWelshEvaluator = new UniqueTranslationWithEnglishAndWelshEvaluator();
    private final CustomValueEvaluator
        uniqueTranslationWithOnlyEnglishEvaluator = new UniqueTranslationWithOnlyEnglishEvaluator();

    @Override
    public synchronized Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return Stream.of(containsDictionaryTranslationsEvaluator, containsDictionaryFromContextEvaluator,
                         uniqueTestPhraseEvaluator, getTranslationsEvaluator,
                         uniqueTranslationWithOnlyEnglishEvaluator, uniqueTranslationWithEnglishAndWelshEvaluator)
            .filter(candidate -> candidate.matches(CustomValueKey.getEnum(key.toString())))
            .findFirst()
            .map(evaluator -> evaluator.calculate(scenarioContext, key))
            .orElseGet(() -> super.calculateCustomValue(scenarioContext, key));
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
