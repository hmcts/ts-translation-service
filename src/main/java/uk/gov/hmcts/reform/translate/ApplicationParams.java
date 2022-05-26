package uk.gov.hmcts.reform.translate;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class ApplicationParams {

    @Value("#{'${ccd.s2s-authorised.services.translations}'.split(',')}")
    private List<String> authorisedServicesForTranslation;


    public List<String> getAuthorisedServicesForTranslation() {
        return authorisedServicesForTranslation;
    }
}
