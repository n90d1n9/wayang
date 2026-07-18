package tech.kayys.wayang.tool.service;

import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.UnifiedRegistryImportRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UnifiedRegistryImportServiceEditionGateTest {

    @Test
    void blocksMcpImportInCommunityMode() {
        UnifiedRegistryImportService service = new UnifiedRegistryImportService();
        service.editionModeService = communityEdition();

        assertThrows(ForbiddenException.class, () -> service.importSource(
                "community",
                "tester",
                new UnifiedRegistryImportRequest(
                        "RAW",
                        "{\"mcpServers\":{\"image-downloader\":{\"command\":\"node\",\"args\":[\"/tmp/index.js\"]}}}",
                        "MCP",
                        null,
                        null,
                        null,
                        null))
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
