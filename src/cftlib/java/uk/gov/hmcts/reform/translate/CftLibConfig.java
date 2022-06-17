package uk.gov.hmcts.reform.translate;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import static uk.gov.hmcts.reform.translate.security.SecurityUtils.LOAD_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;

@Component
public class CftLibConfig implements CFTLibConfigurer {

    private static final String CCD_IMPORT_ROLE = "ccd-import";
    private static final String CASEWORKER_ROLE = "caseworker";
    private static final String CASEWORKER_JURISDICTION_ROLE = "caseworker-befta_jurisdiction_2";
    private static final String CASEWORKER_JURISDICTION_SOLICITOR_ROLE = "caseworker-befta_jurisdiction_2-solicitor_2";

    @Override
    public void configure(CFTLib lib) {
        createCcdRoles(lib);
        createIdamUsers(lib);
    }

    private void createCcdRoles(CFTLib lib) {
        lib.createRoles(
            CCD_IMPORT_ROLE,
            MANAGE_TRANSLATIONS_ROLE,
            LOAD_TRANSLATIONS_ROLE
        );
    }

    private void createIdamUsers(CFTLib lib) {
        lib.createIdamUser("ts.service.translation-manage@gmail.com",
                           CCD_IMPORT_ROLE, MANAGE_TRANSLATIONS_ROLE);
        lib.createIdamUser("ts.service.translation-load@gmail.com",
                           CCD_IMPORT_ROLE, LOAD_TRANSLATIONS_ROLE);
        lib.createIdamUser("ts.service.translation-manage-load@gmail.com",
                           CCD_IMPORT_ROLE, LOAD_TRANSLATIONS_ROLE, MANAGE_TRANSLATIONS_ROLE);
        lib.createIdamUser("befta.caseworker.2.solicitor.2@gmail.com",
                           CASEWORKER_ROLE, CASEWORKER_JURISDICTION_ROLE, CASEWORKER_JURISDICTION_SOLICITOR_ROLE);
    }
}
