package uk.gov.hmcts.reform.translate;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

import java.util.concurrent.ExecutionException;

public class TranslationServiceTestAutomationAdapter extends DefaultTestAutomationAdapter {


    @Override
    public String getNewS2SToken() {
        return null;
    }

    @Override
    public String getNewS2SToken(String clientId) {
        return null;
    }

    @Override
    public void authenticate(UserData user, String preferredTokenClientId) throws ExecutionException {

    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return null;
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
