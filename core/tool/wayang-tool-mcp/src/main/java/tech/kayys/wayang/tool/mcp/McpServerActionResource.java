package tech.kayys.wayang.tool.mcp;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

@Path("/mcp")
@Tag(name = "MCP Server Actions", description = "MCP server health and action API")
public class McpServerActionResource {

    @Inject
    ToolRequestContext requestContext;

    @Inject
    McpToolServerHealthService serverHealthService;

    @Inject
    McpServerActionExecutionService actionExecutionService;

    @Inject
    McpServerActionPreviewService actionPreviewService;

    @Inject
    McpServerActionExecutionHistoryService actionExecutionHistoryService;

    /**
     * Summarize MCP server registry health.
     */
    @GET
    @Path("/servers/health")
    @Authenticated
    @Operation(summary = "Summarize MCP server health")
    public Uni<RestResponse<McpToolServerHealth>> summarizeServerHealth(
            @BeanParam McpServerHealthQuery query) {

        return McpResourceSupport.ok(
                mcpServerHealthService().summarize(requestId(), query.toFilters()));
    }

    /**
     * List prioritized MCP server actions.
     */
    @GET
    @Path("/servers/actions")
    @Authenticated
    @Operation(summary = "List prioritized MCP server actions")
    public Uni<RestResponse<McpServerActionQueue>> listServerActions(
            @BeanParam McpServerHealthQuery query) {

        return McpResourceSupport.ok(
                mcpServerHealthService().summarize(requestId(), query.toFilters())
                        .map(McpServerActionQueue::from));
    }

    /**
     * Preview one prioritized MCP server action without executing it.
     */
    @GET
    @Path("/servers/actions/{actionId}/preview")
    @Authenticated
    @Operation(summary = "Preview a prioritized MCP server action")
    public Uni<RestResponse<McpServerActionPreview>> previewServerAction(
            @PathParam("actionId") String actionId) {

        McpServerActionIdentity identity = actionIdentity(actionId);
        if (identity == null) {
            return McpServerActionResponses.invalidPreview(actionId);
        }

        return mcpServerActionPreviewService().preview(requestId(), identity)
                .map(McpServerActionResponses::preview);
    }

    /**
     * Execute one automatable MCP server action.
     */
    @POST
    @Path("/servers/actions/{actionId}/execute")
    @Authenticated
    @Operation(summary = "Execute an automatable MCP server action")
    public Uni<RestResponse<McpServerActionExecutionResult>> executeServerAction(
            @PathParam("actionId") String actionId) {

        String requestId = requestId();
        McpServerActionIdentity identity = actionIdentity(actionId);
        if (identity == null) {
            McpServerActionPreview preview = McpServerActionPreview.invalid(actionId);
            return recordServerActionExecution(
                    requestId,
                    McpServerActionExecutionResult.invalid(actionId, preview))
                    .map(McpServerActionResponses::execution);
        }

        return mcpServerActionPreviewService().preview(requestId, identity)
                .flatMap(preview -> mcpServerActionExecutionService().execute(requestId, preview))
                .flatMap(result -> recordServerActionExecution(requestId, result))
                .map(McpServerActionResponses::execution);
    }

    private String requestId() {
        return McpResourceSupport.currentRequestId(requestContext);
    }

    private McpServerActionIdentity actionIdentity(String actionId) {
        return McpServerActionPreview.identity(actionId);
    }

    private Uni<McpServerActionExecutionResult> recordServerActionExecution(
            String requestId,
            McpServerActionExecutionResult result) {
        if (actionExecutionHistoryService == null) {
            return Uni.createFrom().item(result);
        }
        return actionExecutionHistoryService.record(requestId, result);
    }

    private McpServerActionExecutionService mcpServerActionExecutionService() {
        return actionExecutionService;
    }

    private McpServerActionPreviewService mcpServerActionPreviewService() {
        if (actionPreviewService != null) {
            return actionPreviewService;
        }
        McpServerActionPreviewService fallback = new McpServerActionPreviewService();
        fallback.serverHealthService = serverHealthService;
        return fallback;
    }

    private McpToolServerHealthService mcpServerHealthService() {
        return McpServerHealthFallbacks.serverHealthService(serverHealthService);
    }
}
