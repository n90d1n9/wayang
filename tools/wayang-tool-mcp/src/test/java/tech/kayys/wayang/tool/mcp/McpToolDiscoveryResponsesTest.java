package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveryFailure;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoverySuccess;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importFailureResult;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importSuccessResult;

class McpToolDiscoveryResponsesTest {

    @Test
    void discoverySuccessMapsToOk() {
        RestResponse<McpToolDiscoveryResult> response = McpToolDiscoveryResponses.discovery(
                discoverySuccess(
                        "docs",
                        12,
                        Map.of(),
                        discoveredTool("docs", "search", "Search", "Search docs", Map.of())));

        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity().success());
    }

    @Test
    void discoveryFailureMapsToBadGateway() {
        RestResponse<McpToolDiscoveryResult> response = McpToolDiscoveryResponses.discovery(
                discoveryFailure("docs", "blocked", 3, Map.of()));

        assertEquals(502, response.getStatus());
        assertEquals(false, response.getEntity().success());
    }

    @Test
    void discoveryImportSuccessMapsToOk() {
        RestResponse<McpToolDiscoveryImportResult> response = McpToolDiscoveryResponses.discoveryImport(
                importSuccessResult("docs", "docs", 1, List.of("docs:search")));

        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity().success());
    }

    @Test
    void discoveryImportFailureMapsToBadGateway() {
        RestResponse<McpToolDiscoveryImportResult> response = McpToolDiscoveryResponses.discoveryImport(
                importFailureResult("docs", "docs", "blocked"));

        assertEquals(502, response.getStatus());
        assertEquals(false, response.getEntity().success());
    }
}
