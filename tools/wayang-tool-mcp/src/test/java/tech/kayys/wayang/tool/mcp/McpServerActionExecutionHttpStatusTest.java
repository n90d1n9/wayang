package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionExecutionHttpStatusTest {

    @Test
    void mapsSuccessfulExecutionToOk() {
        assertEquals(Response.Status.OK, McpServerActionExecutionHttpStatus.fromStatus(
                McpServerActionExecutionResult.STATUS_EXECUTED));
    }

    @Test
    void mapsInvalidExecutionToBadRequest() {
        assertEquals(Response.Status.BAD_REQUEST, McpServerActionExecutionHttpStatus.fromStatus(
                McpServerActionExecutionResult.STATUS_INVALID));
    }

    @Test
    void mapsMissingExecutionToNotFound() {
        assertEquals(Response.Status.NOT_FOUND, McpServerActionExecutionHttpStatus.fromStatus(
                McpServerActionExecutionResult.STATUS_NOT_FOUND));
    }

    @Test
    void mapsFailedExecutionToBadGateway() {
        assertEquals(Response.Status.BAD_GATEWAY, McpServerActionExecutionHttpStatus.fromStatus(
                McpServerActionExecutionResult.STATUS_FAILED));
    }

    @Test
    void mapsRejectedAndUnknownExecutionToConflict() {
        assertEquals(Response.Status.CONFLICT, McpServerActionExecutionHttpStatus.fromStatus(
                McpServerActionExecutionResult.STATUS_REJECTED));
        assertEquals(Response.Status.CONFLICT, McpServerActionExecutionHttpStatus.fromStatus(
                "UNKNOWN"));
    }
}
