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

    @Override
    public synchronized Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {

//      System.out.println("CONTEXTID ----   "+scenarioContext.getContextId());
//        if (scenarioContext.getContextId().equals("S-004.1")) {
//            System.out.println("CUSTOMVALUE PARENT===>     "+scenarioContext.getParentContext());
//            System.out.println("CUSTOMVALUE SIBLING===>     "+scenarioContext.getSiblingContexts());
//            System.out.println("CUSTOMVALUE CHILD===>     "+scenarioContext.getChildContexts());
//        }


        return Stream.of(dictionaryTranslationsEvaluator, getTranslationsEvaluator, uniqueStringEvaluator

            )
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
