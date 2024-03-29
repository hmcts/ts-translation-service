package uk.gov.hmcts.reform.translate;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.TestAutomationAdapter;
import uk.gov.hmcts.befta.TestAutomationConfig;
import uk.gov.hmcts.befta.auth.UserTokenProviderConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.JsonUtils;
import uk.gov.hmcts.reform.translate.controllers.ControllerConstants;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TranslationServiceTestDataLoader extends DefaultBeftaTestDataLoader {

    @Override
    public void doLoadTestData() {

    }

    @Override
    public boolean isTestDataLoadedForCurrentRound() {
        return false;
    }

    @Override
    public void loadDataIfNotLoadedVeryRecently() {
        RestAssured.useRelaxedHTTPSValidation();
        try {
            clearTestPhrases();
        } catch (Exception e) {
            // Write details to log then swallow this error as this failure is not critical.  i.e. allow it to continue
            // so any errors related to the deployment under test will re-occur during smoke / functional test run
            // which has better fault reporting.
            BeftaUtils.defaultLog("Error clearing down test data.", e);
        }
    }

    private void clearTestPhrases() {

        Response response = asManageTranslationUser().when()
            .delete(ControllerConstants.TESTING_SUPPORT_URL + ControllerConstants.TEST_PHRASES_URL);

        if (response.getStatusCode() != 204) {
            String message = "Call to delete test phrases failed with response body: " + response.body().prettyPrint();
            message += "\nand http code: " + response.statusCode();
            throw new RuntimeException(message);
        }
        BeftaUtils.defaultLog("Deleted previously generated test phrases");
    }

    private RequestSpecification asManageTranslationUser() {

        UserData manageTranslationUser = getManageTranslationUser();

        try {
            TestAutomationAdapter adapter = BeftaMain.getAdapter();
            adapter.authenticate(manageTranslationUser, UserTokenProviderConfig.DEFAULT_INSTANCE.getClientId());
            String s2sToken = adapter.getNewS2SToken("xui_webapp");

            return RestAssured
                .given(
                    new RequestSpecBuilder().setBaseUri(TestAutomationConfig.INSTANCE.getTestUrl()).build()
                )
                .header(SecurityUtils.AUTHORIZATION, "Bearer " + manageTranslationUser.getAccessToken())
                .header(SecurityUtils.SERVICE_AUTHORIZATION, s2sToken);

        } catch (ExecutionException e) {
            String message = String.format("authenticating as %s failed ", manageTranslationUser.getUsername());
            throw new RuntimeException(message, e);
        }
    }

    private UserData getManageTranslationUser() {

        try {
            // reuse user from FTAs
            JsonNode userJson = JsonUtils.readObjectFromJsonFile(
                BeftaUtils.getFileFromResource("features/common/users/ManageTranslationsUser.td.json").getPath(),
                JsonNode.class
            );

            return new UserData(
                EnvironmentVariableUtils.resolvePossibleVariable(userJson.findValue("username").asText()),
                EnvironmentVariableUtils.resolvePossibleVariable(userJson.findValue("password").asText())
            );

        } catch (IOException e) {
            throw new RuntimeException("Error loading user data for ManageTranslationUser", e);
        }
    }

}
