package tech.kayys.wayang.tool.service;

import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistryScheduleServiceEditionGateTest {

    @Test
    void blocksMcpScheduleInCommunityMode() {
        RegistryScheduleService service = new RegistryScheduleService();
        service.editionModeService = communityEdition();

        assertThrows(ForbiddenException.class, () -> service.setMcpSchedule("community", "image-downloader", "15m")
                .await().indefinitely());
    }

    private EditionModeService communityEdition() {
        EditionModeService mode = new EditionModeService();
        mode.edition = "community";
        mode.enterpriseEnabled = false;
        mode.multitenancyEnabled = false;
        return mode;
    }
}
