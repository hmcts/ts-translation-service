package uk.gov.hmcts.reform.translate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@PropertySource("classpath:application.yaml")
public class ApplicationParams {


    @Value("#{'${ts.endpoints.put-dictionary.s2s-authorised.bypass-role-authorise-check-for-services}'.split(',')}")
    private List<String> putDictionaryS2sServicesBypassRoleAuthCheck;

    public List<String> getPutDictionaryS2sServicesBypassRoleAuthCheck() {
        return putDictionaryS2sServicesBypassRoleAuthCheck;
    }
}
