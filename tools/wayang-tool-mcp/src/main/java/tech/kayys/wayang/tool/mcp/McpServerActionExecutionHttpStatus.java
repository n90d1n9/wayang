package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.core.Response;

final class McpServerActionExecutionHttpStatus {

    private McpServerActionExecutionHttpStatus() {
    }

    static Response.Status from(McpServerActionExecutionResult result) {
        return fromStatus(result.status());
    }

    static Response.Status fromStatus(String status) {
        return switch (status) {
            case McpServerActionExecutionResult.STATUS_EXECUTED -> Response.Status.OK;
            case McpServerActionExecutionResult.STATUS_INVALID -> Response.Status.BAD_REQUEST;
            case McpServerActionExecutionResult.STATUS_NOT_FOUND -> Response.Status.NOT_FOUND;
            case McpServerActionExecutionResult.STATUS_FAILED -> Response.Status.BAD_GATEWAY;
            default -> Response.Status.CONFLICT;
        };
    }
}
