package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

final class McpServerActionResponses {

    private McpServerActionResponses() {
    }

    static Uni<RestResponse<McpServerActionPreview>> invalidPreview(String actionId) {
        return Uni.createFrom().item(preview(McpServerActionPreview.invalid(actionId)));
    }

    static RestResponse<McpServerActionPreview> preview(McpServerActionPreview preview) {
        if (McpServerActionPreviewStatus.INVALID.equals(preview.status())) {
            return RestResponse.status(Response.Status.BAD_REQUEST, preview);
        }
        return preview.found()
                ? RestResponse.ok(preview)
                : RestResponse.status(Response.Status.NOT_FOUND, preview);
    }

    static RestResponse<McpServerActionExecutionResult> execution(
            McpServerActionExecutionResult result) {
        return RestResponse.status(McpServerActionExecutionHttpStatus.from(result), result);
    }
}
