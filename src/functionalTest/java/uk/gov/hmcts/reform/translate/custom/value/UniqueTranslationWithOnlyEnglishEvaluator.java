package uk.gov.hmcts.reform.translate.custom.value;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.StringGenerator;

import java.util.Map;

public class UniqueTranslationWithOnlyEnglishEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TRANSLATION_WITH_ONLY_ENGLISH.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return Map.of(StringGenerator.generate(), "");
    }
}
