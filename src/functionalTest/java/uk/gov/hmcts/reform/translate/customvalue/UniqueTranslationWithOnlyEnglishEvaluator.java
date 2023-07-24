package uk.gov.hmcts.reform.translate.customvalue;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.Map;

public class UniqueTranslationWithOnlyEnglishEvaluator implements CustomValueEvaluator {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CustomValueKey.UNIQUE_TRANSLATION_WITH_ONLY_ENGLISH.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        final String marker = EvaluatorUtils.extractParameter(key, CustomValueKey.UNIQUE_TRANSLATION_WITH_ONLY_ENGLISH);
        return Map.of(EvaluatorUtils.generateTestPhrase(marker),new Translation(""));
    }
}
