package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpResourceTestFixtures.requestContext;
import static tech.kayys.wayang.tool.mcp.McpServerRegistryResourceTestFixtures.lifecycleImpact;
import static tech.kayys.wayang.tool.mcp.McpServerRegistryResourceTestFixtures.lifecycleResult;
import static tech.kayys.wayang.tool.mcp.McpServerRegistryResourceTestFixtures.retirementResult;
import static tech.kayys.wayang.tool.mcp.McpServerRegistryResourceTestFixtures.serverEntry;

class McpServerRegistryResourceTest {

    @Test
    void listServersUsesTenantContextAndFilters() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant now = Instant.now();
        McpServerRegistryInspectionServiceTestDouble inspectionService =
                McpServerRegistryInspectionServiceTestDouble.listing(List.of(serverEntry("docs", true, now)));
        resource.serverRegistryInspectionService = inspectionService;

        RestResponse<List<McpServerRegistryEntry>> response = resource.listServers(
                        McpServerRegistryQuery.of(true, "http"))
                .await().indefinitely();

        assertEquals("tenant-1", inspectionService.lastRequestId());
        assertEquals(Boolean.TRUE, inspectionService.lastEnabled());
        assertEquals("http", inspectionService.lastTransport());
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("docs", response.getEntity().getFirst().serverName());
        assertEquals("http://docs.local/mcp", response.getEntity().getFirst().endpoint());
    }

    @Test
    void disableServerUsesTenantContext() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant now = Instant.now();
        McpServerLifecycleServiceTestDouble lifecycleService = McpServerLifecycleServiceTestDouble.changing(
                lifecycleResult(
                        serverEntry("docs", false, now),
                        List.of("docs:search"),
                        List.of()));
        resource.serverLifecycleService = lifecycleService;

        RestResponse<McpServerLifecycleResult> response = resource.disableServer("docs")
                .await().indefinitely();

        assertEquals("tenant-1", lifecycleService.lastRequestId());
        assertEquals("docs", lifecycleService.lastServerName());
        assertEquals(false, lifecycleService.lastEnabled());
        assertEquals(200, response.getStatus());
        assertEquals("docs", response.getEntity().server().serverName());
        assertEquals(false, response.getEntity().server().enabled());
        assertEquals(List.of("docs:search"), response.getEntity().disabledToolIds());
    }

    @Test
    void enableServerReturnsNotFoundWhenMissing() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        McpServerLifecycleServiceTestDouble lifecycleService = McpServerLifecycleServiceTestDouble.changing(null);
        resource.serverLifecycleService = lifecycleService;

        RestResponse<McpServerLifecycleResult> response = resource.enableServer("missing")
                .await().indefinitely();

        assertEquals("tenant-1", lifecycleService.lastRequestId());
        assertEquals("missing", lifecycleService.lastServerName());
        assertEquals(true, lifecycleService.lastEnabled());
        assertEquals(404, response.getStatus());
    }

    @Test
    void previewServerLifecycleImpactUsesTenantContext() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant now = Instant.now();
        McpServerLifecycleServiceTestDouble lifecycleService = McpServerLifecycleServiceTestDouble.impacting(
                lifecycleImpact(serverEntry("docs", true, now)));
        resource.serverLifecycleService = lifecycleService;

        RestResponse<McpServerLifecycleImpact> response = resource.previewServerLifecycleImpact("docs")
                .await().indefinitely();

        assertEquals("tenant-1", lifecycleService.lastRequestId());
        assertEquals("docs", lifecycleService.lastServerName());
        assertEquals(200, response.getStatus());
        assertEquals("docs", response.getEntity().server().serverName());
        assertEquals(List.of("docs:search"), response.getEntity().disableAffectedToolIds());
        assertEquals(List.of("docs:lookup"), response.getEntity().enableAffectedToolIds());
    }

    @Test
    void previewServerLifecycleImpactReturnsNotFoundWhenMissing() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        McpServerLifecycleServiceTestDouble lifecycleService = McpServerLifecycleServiceTestDouble.impacting(null);
        resource.serverLifecycleService = lifecycleService;

        RestResponse<McpServerLifecycleImpact> response = resource.previewServerLifecycleImpact("missing")
                .await().indefinitely();

        assertEquals("tenant-1", lifecycleService.lastRequestId());
        assertEquals("missing", lifecycleService.lastServerName());
        assertEquals(404, response.getStatus());
    }

    @Test
    void retireServerUsesTenantContext() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        Instant now = Instant.now();
        McpServerLifecycleServiceTestDouble lifecycleService = McpServerLifecycleServiceTestDouble.retiring(
                retirementResult(
                        serverEntry("docs", false, now),
                        List.of("docs:search", "docs:lookup")));
        resource.serverLifecycleService = lifecycleService;

        RestResponse<McpServerRetirementResult> response = resource.retireServer("docs")
                .await().indefinitely();

        assertEquals("tenant-1", lifecycleService.lastRequestId());
        assertEquals("docs", lifecycleService.lastServerName());
        assertEquals(200, response.getStatus());
        assertEquals("docs", response.getEntity().server().serverName());
        assertEquals(List.of("docs:search", "docs:lookup"), response.getEntity().retiredToolIds());
        assertEquals(2, response.getEntity().affectedTools());
    }

    @Test
    void retireServerReturnsNotFoundWhenMissing() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        McpServerLifecycleServiceTestDouble lifecycleService = McpServerLifecycleServiceTestDouble.retiring(null);
        resource.serverLifecycleService = lifecycleService;

        RestResponse<McpServerRetirementResult> response = resource.retireServer("missing")
                .await().indefinitely();

        assertEquals("tenant-1", lifecycleService.lastRequestId());
        assertEquals("missing", lifecycleService.lastServerName());
        assertEquals(404, response.getStatus());
    }

    @Test
    void getServerUsesTenantContextAndReturnsNotFound() {
        McpServerRegistryResource resource = new McpServerRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        McpServerRegistryInspectionServiceTestDouble inspectionService =
                McpServerRegistryInspectionServiceTestDouble.getting(null);
        resource.serverRegistryInspectionService = inspectionService;

        RestResponse<McpServerRegistryEntry> response = resource.getServer("missing")
                .await().indefinitely();

        assertEquals("tenant-1", inspectionService.lastRequestId());
        assertEquals("missing", inspectionService.lastServerName());
        assertEquals(404, response.getStatus());
    }
}
