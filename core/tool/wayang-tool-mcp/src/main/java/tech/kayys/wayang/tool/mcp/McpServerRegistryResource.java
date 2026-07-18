package tech.kayys.wayang.tool.mcp;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
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
@Tag(name = "MCP Server Registry", description = "MCP server registry and lifecycle API")
public class McpServerRegistryResource {

    @Inject
    ToolRequestContext requestContext;

    @Inject
    McpServerRegistryInspectionService serverRegistryInspectionService;

    @Inject
    McpServerLifecycleService serverLifecycleService;

    /**
     * List registered MCP servers.
     */
    @GET
    @Path("/servers")
    @Authenticated
    @Operation(summary = "List MCP server registry entries")
    public Uni<RestResponse<List<McpServerRegistryEntry>>> listServers(
            @BeanParam McpServerRegistryQuery query) {

        return McpResourceSupport.ok(
                mcpServerRegistryInspectionService().list(requestId(), query.enabled, query.transport));
    }

    /**
     * Enable a registered MCP server.
     */
    @POST
    @Path("/servers/{serverName}/enable")
    @Authenticated
    @Operation(summary = "Enable MCP server registry entry")
    public Uni<RestResponse<McpServerLifecycleResult>> enableServer(
            @PathParam("serverName") String serverName) {

        return setServerEnabled(serverName, true);
    }

    /**
     * Disable a registered MCP server.
     */
    @POST
    @Path("/servers/{serverName}/disable")
    @Authenticated
    @Operation(summary = "Disable MCP server registry entry")
    public Uni<RestResponse<McpServerLifecycleResult>> disableServer(
            @PathParam("serverName") String serverName) {

        return setServerEnabled(serverName, false);
    }

    private Uni<RestResponse<McpServerLifecycleResult>> setServerEnabled(
            String serverName,
            boolean enabled) {
        return McpResourceSupport.okOrNotFound(
                mcpServerLifecycleService().setEnabled(requestId(), serverName, enabled));
    }

    /**
     * Preview the tool impact of MCP server lifecycle actions.
     */
    @GET
    @Path("/servers/{serverName}/lifecycle-impact")
    @Authenticated
    @Operation(summary = "Preview MCP server lifecycle impact")
    public Uni<RestResponse<McpServerLifecycleImpact>> previewServerLifecycleImpact(
            @PathParam("serverName") String serverName) {

        return McpResourceSupport.okOrNotFound(
                mcpServerLifecycleService().impact(requestId(), serverName));
    }

    /**
     * Retire a registered MCP server and mark its imported tools stale.
     */
    @DELETE
    @Path("/servers/{serverName}")
    @Authenticated
    @Operation(summary = "Retire MCP server registry entry")
    public Uni<RestResponse<McpServerRetirementResult>> retireServer(
            @PathParam("serverName") String serverName) {

        return McpResourceSupport.okOrNotFound(
                mcpServerLifecycleService().retire(requestId(), serverName));
    }

    /**
     * Get a registered MCP server.
     */
    @GET
    @Path("/servers/{serverName}")
    @Authenticated
    @Operation(summary = "Get MCP server registry entry")
    public Uni<RestResponse<McpServerRegistryEntry>> getServer(
            @PathParam("serverName") String serverName) {

        return McpResourceSupport.okOrNotFound(
                mcpServerRegistryInspectionService().get(requestId(), serverName));
    }

    private String requestId() {
        return McpResourceSupport.currentRequestId(requestContext);
    }

    private McpServerRegistryInspectionService mcpServerRegistryInspectionService() {
        return McpServerRegistryFallbacks.serverRegistryInspectionService(serverRegistryInspectionService);
    }

    private McpServerLifecycleService mcpServerLifecycleService() {
        return McpServerRegistryFallbacks.serverLifecycleService(serverLifecycleService);
    }

}
