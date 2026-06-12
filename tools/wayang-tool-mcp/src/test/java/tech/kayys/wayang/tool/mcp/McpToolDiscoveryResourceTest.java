package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveryFailure;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveryRequest;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoverySuccess;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.httpImportRequest;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importFailureResult;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importSuccessResult;
import static tech.kayys.wayang.tool.mcp.McpResourceTestFixtures.requestContext;

class McpToolDiscoveryResourceTest {

    @Test
    void discoverToolsReturnsDiscoveredTools() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.discoveryClient = McpToolDiscoveryClientTestDouble.succeeding(discoverySuccess(
                "docs",
                12,
                Map.of("endpoint", "http://localhost/mcp"),
                discoveredTool(
                        "docs",
                        "search",
                        "Search Docs",
                        "Search documentation",
                        Map.of())));

        RestResponse<McpToolDiscoveryResult> response = resource.discoverTools(
                        discoveryRequest("docs", "http://localhost/mcp"))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity().success());
        assertEquals("docs:search", response.getEntity().tools().getFirst().id());
    }

    @Test
    void discoverToolsReturnsBadGatewayOnDiscoveryFailure() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.discoveryClient = McpToolDiscoveryClientTestDouble.succeeding(
                discoveryFailure("docs", "server unavailable", 8, Map.of()));

        RestResponse<McpToolDiscoveryResult> response = resource.discoverTools(
                        discoveryRequest("docs", "http://localhost/mcp"))
                .await().indefinitely();

        assertEquals(502, response.getStatus());
        assertFalse(response.getEntity().success());
        assertEquals("server unavailable", response.getEntity().error());
    }

    @Test
    void discoverAndImportToolsReturnsImportSummary() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.requestContext = requestContext("tenant-1");
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(
                        importSuccessResult("docs", "docs", 1, List.of("docs:search")));
        resource.discoveryImportService = importService;

        RestResponse<McpToolDiscoveryImportResult> response = resource.discoverAndImportTools(
                        httpImportRequest("docs"))
                .await().indefinitely();

        assertEquals("tenant-1", importService.lastRequestId());
        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity().success());
        assertEquals("docs", response.getEntity().namespace());
        assertEquals(List.of("docs:search"), response.getEntity().toolIds());
    }

    @Test
    void discoverAndImportToolsReturnsBadGatewayOnImportFailure() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.discoveryImportService = McpToolDiscoveryImportServiceTestDouble.succeeding(
                importFailureResult("docs", "docs", "blocked"));

        RestResponse<McpToolDiscoveryImportResult> response = resource.discoverAndImportTools(
                        httpImportRequest("docs"))
                .await().indefinitely();

        assertEquals(502, response.getStatus());
        assertFalse(response.getEntity().success());
        assertEquals("blocked", response.getEntity().error());
    }

    @Test
    void syncScheduledDiscoveredToolsReturnsSummary() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.discoverySyncService = McpToolDiscoveryResourceSyncServiceTestDouble.scheduled(
                new McpToolDiscoverySyncResult(
                        2,
                        3,
                        List.of("minor warning")));

        RestResponse<McpToolDiscoverySyncResult> response = resource.syncScheduledDiscoveredTools()
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().scanned());
        assertEquals(3, response.getEntity().imported());
        assertEquals(List.of("minor warning"), response.getEntity().warnings());
    }

    @Test
    void syncRegisteredServerDiscoveredToolsUsesTenantContext() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.requestContext = requestContext("tenant-1");
        McpToolDiscoveryResourceSyncServiceTestDouble syncService =
                McpToolDiscoveryResourceSyncServiceTestDouble.registered(
                        new McpToolDiscoverySyncResult(
                                1,
                                2,
                                1,
                                1,
                                List.of()));
        resource.discoverySyncService = syncService;

        RestResponse<McpToolDiscoverySyncResult> response = resource.syncRegisteredServerDiscoveredTools("docs")
                .await().indefinitely();

        assertEquals("tenant-1", syncService.lastRequestId());
        assertEquals("docs", syncService.lastServerName());
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().scanned());
        assertEquals(2, response.getEntity().imported());
        assertEquals(1, response.getEntity().stale());
        assertEquals(1, response.getEntity().reactivated());
    }

    @Test
    void listDiscoverySyncHistoryUsesTenantContextAndFilters() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant startedAt = Instant.now();
        McpToolDiscoveryResourceSyncServiceTestDouble syncService =
                McpToolDiscoveryResourceSyncServiceTestDouble.history(List.of(
                        new McpToolDiscoverySyncHistoryEntry(
                                "docs",
                                "SUCCESS",
                                "docs synced",
                                2,
                                10,
                                startedAt,
                                startedAt.plusMillis(10))));
        resource.discoverySyncService = syncService;

        RestResponse<List<McpToolDiscoverySyncHistoryEntry>> response =
                resource.listDiscoverySyncHistory(
                                McpToolDiscoverySyncHistoryQuery.of("docs", "SUCCESS", 5))
                        .await().indefinitely();

        assertEquals("tenant-1", syncService.lastRequestId());
        assertEquals("docs", syncService.lastServerName());
        assertEquals("SUCCESS", syncService.lastStatus());
        assertEquals(5, syncService.lastLimit());
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("docs", response.getEntity().getFirst().serverName());
        assertEquals("SUCCESS", response.getEntity().getFirst().status());
        assertEquals(2, response.getEntity().getFirst().itemsAffected());
        assertEquals(10, response.getEntity().getFirst().durationMs());
    }

    @Test
    void summarizeDiscoverySyncHistoryUsesTenantContextAndLimit() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant startedAt = Instant.now();
        McpToolDiscoveryResourceSyncServiceTestDouble syncService =
                McpToolDiscoveryResourceSyncServiceTestDouble.summary(
                        new McpToolDiscoverySyncHistorySummary(
                                2,
                                1,
                                1,
                                2,
                                20,
                                "ERROR",
                                "docs blocked",
                                startedAt,
                                startedAt.plusMillis(10),
                                List.of(new McpToolDiscoverySyncHistorySummary.ServerSummary(
                                        "docs",
                                        2,
                                        1,
                                        1,
                                        2,
                                        20,
                                        "ERROR",
                                        "docs blocked",
                                        startedAt,
                                        startedAt.plusMillis(10)))));
        resource.discoverySyncService = syncService;

        RestResponse<McpToolDiscoverySyncHistorySummary> response =
                resource.summarizeDiscoverySyncHistory(
                                McpToolDiscoverySyncHistoryQuery.of("docs", "ERROR", 5))
                        .await().indefinitely();

        assertEquals("tenant-1", syncService.lastRequestId());
        assertEquals("docs", syncService.lastServerName());
        assertEquals("ERROR", syncService.lastStatus());
        assertEquals(5, syncService.lastLimit());
        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().total());
        assertEquals(1, response.getEntity().success());
        assertEquals(1, response.getEntity().error());
        assertEquals("ERROR", response.getEntity().latestStatus());
        assertEquals(1, response.getEntity().servers().size());
        assertEquals("docs", response.getEntity().servers().getFirst().serverName());
    }

    @Test
    void listLatestDiscoverySyncHistoryUsesTenantContextAndFilters() {
        McpToolDiscoveryResource resource = new McpToolDiscoveryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant startedAt = Instant.now();
        McpToolDiscoveryResourceSyncServiceTestDouble syncService =
                McpToolDiscoveryResourceSyncServiceTestDouble.latestHistory(List.of(
                        new McpToolDiscoverySyncHistoryEntry(
                                "docs",
                                "ERROR",
                                "docs blocked",
                                0,
                                10,
                                startedAt,
                                startedAt.plusMillis(10))));
        resource.discoverySyncService = syncService;

        RestResponse<List<McpToolDiscoverySyncHistoryEntry>> response =
                resource.listLatestDiscoverySyncHistory(
                                McpToolDiscoverySyncHistoryQuery.of("docs", "ERROR", 5))
                        .await().indefinitely();

        assertEquals("tenant-1", syncService.lastRequestId());
        assertEquals("docs", syncService.lastServerName());
        assertEquals("ERROR", syncService.lastStatus());
        assertEquals(5, syncService.lastLimit());
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("docs", response.getEntity().getFirst().serverName());
        assertEquals("ERROR", response.getEntity().getFirst().status());
        assertEquals("docs blocked", response.getEntity().getFirst().message());
    }
}
