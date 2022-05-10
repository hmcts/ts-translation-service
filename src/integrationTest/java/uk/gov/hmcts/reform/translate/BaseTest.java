package uk.gov.hmcts.reform.translate;

import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.translate.config.TestIdamConfiguration;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import javax.inject.Inject;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("itest")
@SuppressWarnings("HideUtilityClassConstructor")
public class BaseTest {

    @Inject
    protected SecurityUtils securityUtils;

    @Mock
    protected Authentication authentication;

    protected static final String GET_TRANSLATION_TABLES_SCRIPT =
        "classpath:sql/get-Dictionary_And_TranslationUploads.sql";

    protected static final String DELETE_TRANSLATION_TABLES_SCRIPT =
        "classpath:sql/delete-Dictionary_And_TranslationUploads.sql";
}
