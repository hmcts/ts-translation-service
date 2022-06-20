package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.customvalue.*;

import java.util.stream.Stream;

public class TranslationServiceTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private final CustomValueEvaluator dictionaryTranslationsEvaluator = new ContainsDictionaryTranslationsEvaluator();
    private final CustomValueEvaluator containsDictionaryTranslationsFromContextEvaluator =
        new ContainsDictionaryTranslationsFromContextEvaluator();
    private final CustomValueEvaluator getTranslationsEvaluator = new GetTranslationsEvaluator();
    private final CustomValueEvaluator uniqueStringEvaluator = new UniqueStringEvaluator();
    private final CustomValueEvaluator
        uniqueTranslationWithEnglishAndWelshEvaluator = new UniqueTranslationWithEnglishAndWelshEvaluator();
    private final CustomValueEvaluator
        uniqueTranslationWithOnlyEnglishEvaluator = new UniqueTranslationWithOnlyEnglishEvaluator();

    @Override
    public synchronized Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return Stream.of(dictionaryTranslationsEvaluator, getTranslationsEvaluator, uniqueStringEvaluator,
                         uniqueTranslationWithOnlyEnglishEvaluator, uniqueTranslationWithEnglishAndWelshEvaluator,
                         containsDictionaryTranslationsFromContextEvaluator)
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
