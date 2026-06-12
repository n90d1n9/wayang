package tech.kayys.wayang.tool.mcp;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.util.List;

@Path("/mcp")
@Tag(name = "MCP Action Execution History", description = "MCP server action execution history API")
public class McpServerActionExecutionHistoryResource {

    private final McpServerActionExecutionHistoryService fallbackService =
            new McpServerActionExecutionHistoryService();

    @Inject
    ToolRequestContext requestContext;

    @Inject
    McpServerActionExecutionHistoryService actionExecutionHistoryService;

    /**
     * Clear recent MCP server action execution outcomes for the current request scope.
     */
    @DELETE
    @Path("/servers/actions/executions")
    @Authenticated
    @Operation(summary = "Clear recent MCP server action executions")
    public Uni<RestResponse<McpServerActionExecutionHistoryClearResult>> clearServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {
        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .clear(resourceQuery.requestId(), resourceQuery.mutationQuery()));
    }

    /**
     * Preview how many MCP server action execution outcomes would be cleared.
     */
    @GET
    @Path("/servers/actions/executions/clear-preview")
    @Authenticated
    @Operation(summary = "Preview clearing MCP server action executions")
    public Uni<RestResponse<McpServerActionExecutionHistoryClearPreview>> previewClearServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {
        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .previewClear(resourceQuery.requestId(), resourceQuery.mutationQuery()));
    }

    /**
     * Prune expired MCP server action execution outcomes for the current request scope.
     */
    @POST
    @Path("/servers/actions/executions/prune-expired")
    @Authenticated
    @Operation(summary = "Prune expired MCP server action executions")
    public Uni<RestResponse<McpServerActionExecutionHistoryPruneResult>> pruneExpiredServerActionExecutions() {
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .pruneExpired(requestId()));
    }

    /**
     * Prune expired MCP server action execution outcomes across all request scopes.
     */
    @POST
    @Path("/servers/actions/executions/prune-expired/all")
    @Authenticated
    @Operation(summary = "Prune expired MCP server action executions across all request scopes")
    public Uni<RestResponse<McpServerActionExecutionHistoryPruneResult>> pruneAllExpiredServerActionExecutions() {
        return McpResourceSupport.ok(actionExecutionHistoryService().pruneExpired());
    }

    /**
     * Inspect MCP server action execution history storage for the current request scope.
     */
    @GET
    @Path("/servers/actions/executions/stats")
    @Authenticated
    @Operation(summary = "Inspect MCP server action execution history storage")
    public Uni<RestResponse<McpServerActionExecutionHistoryStats>> getServerActionExecutionHistoryStats() {
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .stats(requestId()));
    }

    /**
     * Inspect MCP server action execution history storage across all request scopes.
     */
    @GET
    @Path("/servers/actions/executions/stats/all")
    @Authenticated
    @Operation(summary = "Inspect all MCP server action execution history storage")
    public Uni<RestResponse<McpServerActionExecutionHistoryStats>> getAllServerActionExecutionHistoryStats() {
        return McpResourceSupport.ok(actionExecutionHistoryService().stats());
    }

    /**
     * List recent MCP server action execution outcomes.
     */
    @GET
    @Path("/servers/actions/executions")
    @Authenticated
    @Operation(summary = "List recent MCP server action executions")
    public Uni<RestResponse<List<McpServerActionExecutionHistoryEntry>>> listServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {

        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .list(resourceQuery.requestId(), resourceQuery.historyQuery()));
    }

    /**
     * Page recent MCP server action execution outcomes with paging metadata.
     */
    @GET
    @Path("/servers/actions/executions/page")
    @Authenticated
    @Operation(summary = "Page recent MCP server action executions")
    public Uni<RestResponse<McpServerActionExecutionHistoryPage>> pageServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {

        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .page(resourceQuery.requestId(), resourceQuery.historyQuery()));
    }

    /**
     * List the latest MCP server action execution outcome per server action.
     */
    @GET
    @Path("/servers/actions/executions/latest")
    @Authenticated
    @Operation(summary = "List latest MCP server action executions")
    public Uni<RestResponse<List<McpServerActionExecutionHistoryEntry>>> listLatestServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {

        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .listLatest(resourceQuery.requestId(), resourceQuery.historyQuery()));
    }

    /**
     * Page the latest MCP server action execution outcome per server action with metadata.
     */
    @GET
    @Path("/servers/actions/executions/latest/page")
    @Authenticated
    @Operation(summary = "Page latest MCP server action executions")
    public Uni<RestResponse<McpServerActionExecutionHistoryPage>> pageLatestServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {

        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .pageLatest(resourceQuery.requestId(), resourceQuery.historyQuery()));
    }

    /**
     * Summarize recent MCP server action execution outcomes.
     */
    @GET
    @Path("/servers/actions/executions/summary")
    @Authenticated
    @Operation(summary = "Summarize recent MCP server action executions")
    public Uni<RestResponse<McpServerActionExecutionHistorySummary>> summarizeServerActionExecutions(
            @BeanParam McpServerActionExecutionHistoryQueryParams query) {

        McpServerActionExecutionHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(actionExecutionHistoryService()
                .summarize(resourceQuery.requestId(), resourceQuery.summaryQuery()));
    }

    private McpServerActionExecutionHistoryResourceQuery resourceQuery(
            McpServerActionExecutionHistoryQueryParams query) {
        return McpServerActionExecutionHistoryResourceQuery.from(query, requestContext);
    }

    private String requestId() {
        return McpServerActionExecutionHistoryResourceQuery.requestId(requestContext);
    }

    private McpServerActionExecutionHistoryService actionExecutionHistoryService() {
        return actionExecutionHistoryService == null ? fallbackService : actionExecutionHistoryService;
    }

}
