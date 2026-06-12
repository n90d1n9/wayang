package tech.kayys.wayang.tool.mcp;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.util.List;

@Path("/mcp")
@Tag(name = "MCP Discovery", description = "Live MCP tool discovery API")
public class McpToolDiscoveryResource {

    @Inject
    ToolRequestContext requestContext;

    @Inject
    McpToolDiscoveryClient discoveryClient;

    @Inject
    McpToolDiscoveryImportService discoveryImportService;

    @Inject
    McpToolDiscoverySyncService discoverySyncService;

    /**
     * Discover tools from a live MCP server.
     */
    @POST
    @Path("/tools/discover")
    @Authenticated
    @Operation(summary = "Discover tools from an MCP server")
    public Uni<RestResponse<McpToolDiscoveryResult>> discoverTools(
            @Valid McpToolDiscoveryRequest request) {

        return mcpDiscoveryClient().discoverTools(request)
                .map(McpToolDiscoveryResponses::discovery);
    }

    /**
     * Discover tools from a live MCP server and import them into the tenant registry.
     */
    @POST
    @Path("/tools/discover/import")
    @Authenticated
    @Operation(summary = "Discover and import tools from an MCP server")
    public Uni<RestResponse<McpToolDiscoveryImportResult>> discoverAndImportTools(
            @Valid McpToolDiscoveryImportRequest request) {

        return mcpDiscoveryImportService().discoverAndImport(requestId(), request)
                .map(McpToolDiscoveryResponses::discoveryImport);
    }

    /**
     * Run scheduled live MCP tool discovery sync now.
     */
    @POST
    @Path("/tools/discover/sync-scheduled")
    @Authenticated
    @Operation(summary = "Run scheduled live MCP tool discovery sync")
    public Uni<RestResponse<McpToolDiscoverySyncResult>> syncScheduledDiscoveredTools() {

        return McpResourceSupport.ok(mcpDiscoverySyncService().syncScheduled());
    }

    /**
     * Run live MCP tool discovery sync for one registered server now.
     */
    @POST
    @Path("/tools/discover/sync/{serverName}")
    @Authenticated
    @Operation(summary = "Run live MCP tool discovery sync for one registered server")
    public Uni<RestResponse<McpToolDiscoverySyncResult>> syncRegisteredServerDiscoveredTools(
            @PathParam("serverName") String serverName) {

        return McpResourceSupport.ok(
                mcpDiscoverySyncService().syncRegisteredServer(requestId(), serverName));
    }

    /**
     * List recent live MCP tool discovery sync history.
     */
    @GET
    @Path("/tools/discover/sync/history")
    @Authenticated
    @Operation(summary = "List live MCP tool discovery sync history")
    public Uni<RestResponse<List<McpToolDiscoverySyncHistoryEntry>>> listDiscoverySyncHistory(
            @BeanParam McpToolDiscoverySyncHistoryQuery query) {

        return McpResourceSupport.ok(
                mcpDiscoverySyncService().listHistory(
                        requestId(),
                        query.serverName,
                        query.status,
                        query.limit));
    }

    /**
     * List the latest live MCP tool discovery sync history entry per server.
     */
    @GET
    @Path("/tools/discover/sync/history/latest")
    @Authenticated
    @Operation(summary = "List latest live MCP tool discovery sync history by server")
    public Uni<RestResponse<List<McpToolDiscoverySyncHistoryEntry>>> listLatestDiscoverySyncHistory(
            @BeanParam McpToolDiscoverySyncHistoryQuery query) {

        return McpResourceSupport.ok(
                mcpDiscoverySyncService().listLatestHistory(
                        requestId(),
                        query.serverName,
                        query.status,
                        query.limit));
    }

    /**
     * Summarize recent live MCP tool discovery sync history.
     */
    @GET
    @Path("/tools/discover/sync/history/summary")
    @Authenticated
    @Operation(summary = "Summarize live MCP tool discovery sync history")
    public Uni<RestResponse<McpToolDiscoverySyncHistorySummary>> summarizeDiscoverySyncHistory(
            @BeanParam McpToolDiscoverySyncHistoryQuery query) {

        return McpResourceSupport.ok(
                mcpDiscoverySyncService().summarizeHistory(
                        requestId(),
                        query.serverName,
                        query.status,
                        query.limit));
    }

    private String requestId() {
        return McpResourceSupport.currentRequestId(requestContext);
    }

    private McpToolDiscoveryClient mcpDiscoveryClient() {
        return McpToolDiscoveryFallbacks.discoveryClient(discoveryClient);
    }

    private McpToolDiscoveryImportService mcpDiscoveryImportService() {
        return McpToolDiscoveryFallbacks.discoveryImportService(discoveryImportService);
    }

    private McpToolDiscoverySyncService mcpDiscoverySyncService() {
        return McpToolDiscoveryFallbacks.discoverySyncService(discoverySyncService);
    }

}
