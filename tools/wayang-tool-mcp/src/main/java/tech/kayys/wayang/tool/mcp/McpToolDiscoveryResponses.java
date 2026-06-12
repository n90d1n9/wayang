package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

final class McpToolDiscoveryResponses {

    private McpToolDiscoveryResponses() {
    }

    static RestResponse<McpToolDiscoveryResult> discovery(McpToolDiscoveryResult result) {
        return result.success()
                ? RestResponse.ok(result)
                : RestResponse.status(Response.Status.BAD_GATEWAY, result);
    }

    static RestResponse<McpToolDiscoveryImportResult> discoveryImport(
            McpToolDiscoveryImportResult result) {
        return result.success()
                ? RestResponse.ok(result)
                : RestResponse.status(Response.Status.BAD_GATEWAY, result);
    }
}
