package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.healthService;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionPreviewServiceTest {

    @Test
    void previewsActionUsingFilteredHealthAndNormalizedActionIdentity() {
        McpToolServerHealth.ActionQueueItem action = action("Due", "run-sync");
        McpServerActionPreviewService service = service(healthService(
                action,
                "tenant-1",
                "due",
                McpServerActionCatalog.ACTION_RUN_SYNC));

        McpServerActionPreview preview = service.preview(
                "tenant-1",
                McpServerActionIdentity.parse("due:RUN_SYNC"))
                .await().indefinitely();

        assertEquals(McpServerActionPreviewStatus.AUTOMATABLE, preview.status());
        assertEquals(true, preview.found());
        assertEquals("Due", preview.serverName());
        assertEquals("run-sync", preview.actionCode());
        assertEquals(action, preview.action());
        assertEquals(List.of("preview warning"), preview.warnings());
    }

    @Test
    void returnsNotFoundPreviewWhenFilteredHealthDoesNotContainAction() {
        McpServerActionPreviewService service = service(healthService(
                action("docs", "review-stale-tools"),
                "tenant-1",
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC));

        McpServerActionPreview preview = service.preview(
                "tenant-1",
                McpServerActionIdentity.parse("docs:run-sync"))
                .await().indefinitely();

        assertEquals(McpServerActionPreviewStatus.NOT_FOUND, preview.status());
        assertEquals(false, preview.found());
        assertEquals("docs", preview.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, preview.actionCode());
        assertEquals(List.of("preview warning"), preview.warnings());
    }

    private static McpServerActionPreviewService service(McpToolServerHealthService healthService) {
        McpServerActionPreviewService service = new McpServerActionPreviewService();
        service.serverHealthService = healthService;
        return service;
    }

}
