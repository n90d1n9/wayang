package tech.kayys.wayang.tool.mcp;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.util.List;

@Path("/mcp")
@Tag(name = "MCP Tool Registry", description = "MCP tool catalog and registry API")
public class McpToolRegistryResource {

    @Inject
    McpToolRegistryService toolRegistryService;

    @Inject
    ToolRequestContext requestContext;

    /**
     * List available tools.
     */
    @GET
    @Path("/tools")
    @Authenticated
    @Operation(summary = "List available tools")
    public Uni<RestResponse<List<McpTool>>> listTools(@BeanParam McpToolRegistryQuery query) {

        return McpResourceSupport.ok(
                toolRegistryService.listAvailableTools(requestId(), query.availableFilters()));
    }

    /**
     * List registry entries, including disabled/stale tools for operations.
     */
    @GET
    @Path("/tools/registry")
    @Authenticated
    @Operation(summary = "List MCP tool registry entries")
    public Uni<RestResponse<List<McpToolRegistryEntry>>> listToolRegistry(
            @BeanParam McpToolRegistryQuery query) {

        return McpResourceSupport.ok(
                toolRegistryService.listRegistryEntries(requestId(), query.registryFilters()));
    }

    /**
     * Summarize registry entries by MCP server and lifecycle state.
     */
    @GET
    @Path("/tools/registry/summary")
    @Authenticated
    @Operation(summary = "Summarize MCP tool registry entries")
    public Uni<RestResponse<McpToolRegistrySummary>> summarizeToolRegistry(
            @BeanParam McpToolRegistryQuery query) {

        return McpResourceSupport.ok(
                toolRegistryService.summarizeRegistry(requestId(), query.registryFilters()));
    }

    /**
     * Get a registry entry by ID, including disabled/stale tools for operations.
     */
    @GET
    @Path("/tools/registry/{toolId}")
    @Authenticated
    @Operation(summary = "Get MCP tool registry entry")
    public Uni<RestResponse<McpToolRegistryEntry>> getToolRegistryEntry(@PathParam("toolId") String toolId) {

        return McpResourceSupport.okOrNotFound(
                toolRegistryService.findRegistryEntry(requestId(), toolId));
    }

    /**
     * Get tool by ID.
     */
    @GET
    @Path("/tools/{toolId}")
    @Authenticated
    @Operation(summary = "Get tool by ID")
    public Uni<RestResponse<McpTool>> getTool(@PathParam("toolId") String toolId) {

        return McpResourceSupport.okOrNotFound(
                toolRegistryService.findAvailableTool(requestId(), toolId));
    }

    private String requestId() {
        return McpResourceSupport.currentRequestId(requestContext);
    }

}
