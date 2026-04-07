package tech.kayys.wayang.tool.service;

import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.McpRegistryImportRequest;
import tech.kayys.wayang.tool.dto.McpServerConfigRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class McpRegistryServiceEditionGateTest {

    @Test
    void blocksMcpRegistryDatabaseOperationsInCommunityMode() {
        McpRegistryService service = new McpRegistryService();
        service.editionModeService = communityEdition();

        assertThrows(ForbiddenException.class, () -> service.importFromJson(
                "community",
                new McpRegistryImportRequest("RAW", "{\"mcpServers\":{}}", null))
                .await().indefinitely());

        assertThrows(ForbiddenException.class, () -> service.listServers("community")
                .await().indefinitely());

        assertThrows(ForbiddenException.class, () -> service.upsertServer(
                "community",
                "image-downloader",
                new McpServerConfigRequest(
                        "stdio",
                        "node",
                        null,
                        List.of("/tmp/index.js"),
                        Map.of(),
                        true,
                        "manual://api",
                        null))
                .await().indefinitely());

        assertThrows(ForbiddenException.class, () -> service.removeServer("community", "image-downloader")
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
