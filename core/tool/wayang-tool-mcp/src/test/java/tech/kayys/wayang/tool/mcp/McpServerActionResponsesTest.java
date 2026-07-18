package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.preview;

class McpServerActionResponsesTest {

    @Test
    void invalidPreviewMapsToBadRequest() {
        RestResponse<McpServerActionPreview> response = McpServerActionResponses.invalidPreview("malformed")
                .await().indefinitely();

        assertEquals(400, response.getStatus());
        assertEquals(McpServerActionPreviewStatus.INVALID, response.getEntity().status());
    }

    @Test
    void missingPreviewMapsToNotFound() {
        RestResponse<McpServerActionPreview> response = McpServerActionResponses.preview(
                McpServerActionPreview.notFound(
                        McpServerActionIdentity.parse("docs:run-sync"),
                        List.of()));

        assertEquals(404, response.getStatus());
        assertEquals(McpServerActionPreviewStatus.NOT_FOUND, response.getEntity().status());
    }

    @Test
    void foundPreviewMapsToOk() {
        RestResponse<McpServerActionPreview> response = McpServerActionResponses.preview(
                preview(action("docs", McpServerActionCatalog.ACTION_RUN_SYNC), List.of()));

        assertEquals(200, response.getStatus());
        assertEquals(McpServerActionPreviewStatus.AUTOMATABLE, response.getEntity().status());
    }

    @Test
    void executionUsesExecutionHttpStatus() {
        McpServerActionPreview preview = McpServerActionPreview.invalid("malformed");

        RestResponse<McpServerActionExecutionResult> response = McpServerActionResponses.execution(
                McpServerActionExecutionResult.invalid("malformed", preview));

        assertEquals(400, response.getStatus());
        assertEquals(McpServerActionExecutionResult.STATUS_INVALID, response.getEntity().status());
    }
}
