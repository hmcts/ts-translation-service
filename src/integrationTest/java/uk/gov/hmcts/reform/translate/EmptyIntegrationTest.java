package uk.gov.hmcts.reform.translate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyIntegrationTest {

    @Tag("smoke")
    @Test
    void shouldRetrieveWhenExists() {
        assertTrue(true, "Exists is true");
    }

}
