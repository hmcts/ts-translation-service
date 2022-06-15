package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

import java.util.stream.Stream;

public class TranslationServiceTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private final CustomValueEvaluator dictionaryTranslationsEvaluator = new DictionaryTranslationsEvaluator();
    private final CustomValueEvaluator getTranslationsEvaluator = new GetTranslationsEvaluator();
    private final CustomValueEvaluator uniqueStringEvaluator = new UniqueStringEvaluator();
    private final CustomValueEvaluator uniqueTranslationEvaluator = new UniqueTranslationEvaluator();
    private final CustomValueEvaluator uniqueLoadTranslationEvaluator = new UniqueLoadTranslationEvaluator();
    private final CustomValueEvaluator getDictionaryTranslationsEvaluator = new GetUniqueTranslationsEvaluator();
    private final CustomValueEvaluator getExpectedTranslationResponseEvaluator
        = new GetExpectedTranslationResponseEvaluator();

    @Override
    public synchronized Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return Stream.of(dictionaryTranslationsEvaluator, getTranslationsEvaluator, uniqueStringEvaluator,
                         uniqueTranslationEvaluator, getDictionaryTranslationsEvaluator, uniqueLoadTranslationEvaluator,
                         getExpectedTranslationResponseEvaluator)
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
