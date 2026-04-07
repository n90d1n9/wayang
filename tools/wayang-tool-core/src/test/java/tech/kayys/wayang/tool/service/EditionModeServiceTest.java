package tech.kayys.wayang.tool.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditionModeServiceTest {

    @Test
    void defaultsToCommunityMode() {
        EditionModeService service = new EditionModeService();
        service.edition = "community";
        service.enterpriseEnabled = false;
        service.multitenancyEnabled = false;

        assertFalse(service.isEnterpriseMode());
        assertFalse(service.supportsMcpRegistryDatabase());
    }

    @Test
    void enablesEnterpriseModeViaEditionFlag() {
        EditionModeService service = new EditionModeService();
        service.edition = "enterprise";
        service.enterpriseEnabled = false;
        service.multitenancyEnabled = false;

        assertTrue(service.isEnterpriseMode());
        assertTrue(service.supportsMcpRegistryDatabase());
    }

    @Test
    void enablesEnterpriseModeViaBooleanFlags() {
        EditionModeService service = new EditionModeService();
        service.edition = "community";
        service.enterpriseEnabled = true;
        service.multitenancyEnabled = false;
        assertTrue(service.isEnterpriseMode());

        service.enterpriseEnabled = false;
        service.multitenancyEnabled = true;
        assertTrue(service.isEnterpriseMode());
    }
}
