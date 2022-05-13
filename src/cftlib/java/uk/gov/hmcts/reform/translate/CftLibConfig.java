package uk.gov.hmcts.reform.translate;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

@Component
public class CftLibConfig implements CFTLibConfigurer {

    @Override
    public void configure(CFTLib lib) {
        createCcdRoles(lib);
        createIdamUsers(lib);
    }

    private void createCcdRoles(CFTLib lib) {
        lib.createRoles(
            "manage-translations",
            "citizen"
        );
    }

    private void createIdamUsers(CFTLib lib) {
        lib.createIdamUser("ccd.ac.staff8@gmail.com", "manage-translations");
        lib.createIdamUser("ccd.ac.other1@gmail.com", "citizen");
    }
}
