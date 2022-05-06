package uk.gov.hmcts.reform.translate;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("itest")
@SuppressWarnings("HideUtilityClassConstructor")
public class BaseTest {

    protected static final String GET_TRANSLATION_TABLES_SCRIPT =
        "classpath:sql/get-Dictionary_And_TranslationUploads.sql";
    protected static final String DELETE_TRANSLATION_TABLES_SCRIPT =
        "classpath:sql/delete-Dictionary_And_TranslationUploads.sql";
}
