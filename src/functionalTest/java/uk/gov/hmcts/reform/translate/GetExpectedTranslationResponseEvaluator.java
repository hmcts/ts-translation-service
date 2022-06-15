package uk.gov.hmcts.reform.translate;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class GetExpectedTranslationResponseEvaluator implements CustomValueEvaluator {
    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.GET_EXPECTED_TRANSLATION_RESPONSE.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {

            @SuppressWarnings("unchecked") final Map<String, String> translations =
                (Map<String, String>) ReflectionUtils.deepGetFieldInObject(
                    scenarioContext,
                    "parentContext.testData.request.body.translations"
                );
            //This is required to fill in the blank Welsh phrase in
            //order to match the translation endpoint response
            return translations.entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                        System.out.println("VALUE" + e.getValue());
                        if (StringUtils.isBlank(e.getValue())) {
                            System.out.println("HERE" + e.getValue());
                            System.out.println(e.getKey());
                            return e.getKey();
                        }
                        return e.getValue();
                    }
                ));
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }
}
