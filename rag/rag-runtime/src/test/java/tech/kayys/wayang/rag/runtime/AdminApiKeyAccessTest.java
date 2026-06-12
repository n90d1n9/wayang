package tech.kayys.wayang.rag.runtime;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminApiKeyAccessTest {

    @Test
    void detectsConfiguredKeysAcrossPrimaryAndSecondarySlots() {
        assertFalse(AdminApiKeyAccess.hasConfiguredKeys(null, " "));
        assertTrue(AdminApiKeyAccess.hasConfiguredKeys("primary", null));
        assertTrue(AdminApiKeyAccess.hasConfiguredKeys("", "secondary"));
    }

    @Test
    void resolvesConfiguredKeySlots() {
        assertEquals(
                AdminApiKeyAccess.SLOT_PRIMARY,
                AdminApiKeyAccess.resolveSlot("primary", "primary", "secondary"));
        assertEquals(
                AdminApiKeyAccess.SLOT_SECONDARY,
                AdminApiKeyAccess.resolveSlot("secondary", "primary", "secondary"));
        assertNull(AdminApiKeyAccess.resolveSlot("wrong", "primary", "secondary"));
        assertNull(AdminApiKeyAccess.resolveSlot(" ", "primary", "secondary"));
    }

    @Test
    void normalizesResponseSlotValues() {
        assertEquals("primary", AdminApiKeyAccess.responseSlot("primary"));
        assertEquals("42", AdminApiKeyAccess.responseSlot(42));
        assertNull(AdminApiKeyAccess.responseSlot(null));
        assertNull(AdminApiKeyAccess.responseSlot(" "));
    }

    @Test
    void buildsConsistentChallengeResponses() {
        Response notConfigured = AdminApiKeyAccess.notConfiguredResponse();
        Response invalid = AdminApiKeyAccess.invalidKeyResponse();

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), notConfigured.getStatus());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), invalid.getStatus());
        assertEquals("ApiKey", invalid.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
    }
}
