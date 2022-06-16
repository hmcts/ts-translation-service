package uk.gov.hmcts.reform.translate.custom.value;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class GetVerifyTranslationResponseFromParentContextEvaluator implements CustomValueEvaluator {
    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.GET_VERIFY_TRANSLATION_RESPONSE_FROM_PARENT_CONTEXT.equals(key);
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
                        if (StringUtils.isBlank(e.getValue())) {
                            return e.getKey();
                        }
                        return e.getValue();
                    }
                ));
        } catch (Exception e) {
            throw new FunctionalTestException("Problem generating custom value for "
                                                  + "GetVerifyTranslationResponseFromParentContextEvaluator: ", e);
        }
    }
}
