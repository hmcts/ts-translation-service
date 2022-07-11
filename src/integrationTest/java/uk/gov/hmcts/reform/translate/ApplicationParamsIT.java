package uk.gov.hmcts.reform.translate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationParamsIT {

    @Nested
    @DisplayName("Empty value property")
    @SpringBootTest(
        classes = {ApplicationParams.class},
        properties = {"ts.endpoints.put-dictionary.s2s-authorised.bypass-role-authorise-check-for-services="}
    )
    class EmptyValueProperty {
        @Inject
        private ApplicationParams underTest;

        @Test
        void shouldResolvePropertyValues() {
            final List<String> result = underTest.getPutDictionaryS2sServicesBypassRoleAuthCheck();

            assertThat(result)
                .singleElement()
                .isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Single value property")
    @SpringBootTest(
        classes = {ApplicationParams.class},
        properties = {"ts.endpoints.put-dictionary.s2s-authorised.bypass-role-authorise-check-for-services=xui_webapp"}
    )
    class SingleValueProperty {
        @Inject
        private ApplicationParams underTest;

        @Test
        void shouldResolvePropertyValues() {
            final List<String> result = underTest.getPutDictionaryS2sServicesBypassRoleAuthCheck();

            assertThat(result)
                .isNotEmpty()
                .singleElement()
                .isEqualTo("xui_webapp");
        }
    }

    @Nested
    @DisplayName("Multi values property")
    @SpringBootTest(
        classes = {ApplicationParams.class},
        properties = {"ts.endpoints.put-dictionary.s2s-authorised.bypass-role-authorise-check-for-services="
                + "xui_webapp,ccd_admin,ccd_definition"}
    )
    class MultiValuesProperty {
        @Inject
        private ApplicationParams underTest;

        @Test
        void shouldResolvePropertyValues() {
            final List<String> result = underTest.getPutDictionaryS2sServicesBypassRoleAuthCheck();

            assertThat(result)
                .isNotEmpty()
                .hasSameElementsAs(List.of("xui_webapp", "ccd_admin", "ccd_definition"));
        }
    }
}
