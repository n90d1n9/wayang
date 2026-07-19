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
@Tag(name = "MCP Tool Call History", description = "MCP tool-call execution history API")
public class McpToolCallHistoryResource {

    private final McpToolCallHistoryService fallbackService = new McpToolCallHistoryService();

    @Inject
    ToolRequestContext requestContext;

    @Inject
    McpToolCallHistoryService toolCallHistoryService;

    /**
     * List recent MCP tool-call execution outcomes.
     */
    @GET
    @Path("/tools/calls")
    @Authenticated
    @Operation(summary = "List recent MCP tool calls")
    public Uni<RestResponse<List<McpToolCallHistoryEntry>>> listToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .list(resourceQuery.runId(), resourceQuery.historyQuery()));
    }

    /**
     * Page recent MCP tool-call execution outcomes with paging metadata.
     */
    @GET
    @Path("/tools/calls/page")
    @Authenticated
    @Operation(summary = "Page recent MCP tool calls")
    public Uni<RestResponse<McpToolCallHistoryPage>> pageToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .page(resourceQuery.runId(), resourceQuery.historyQuery()));
    }

    /**
     * List the latest MCP tool-call execution outcome per tool.
     */
    @GET
    @Path("/tools/calls/latest")
    @Authenticated
    @Operation(summary = "List latest MCP tool calls per tool")
    public Uni<RestResponse<List<McpToolCallHistoryEntry>>> listLatestToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .latest(resourceQuery.runId(), resourceQuery.historyQuery()));
    }

    /**
     * Page the latest MCP tool-call execution outcome per tool with metadata.
     */
    @GET
    @Path("/tools/calls/latest/page")
    @Authenticated
    @Operation(summary = "Page latest MCP tool calls per tool")
    public Uni<RestResponse<McpToolCallHistoryPage>> pageLatestToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .latestPage(resourceQuery.runId(), resourceQuery.historyQuery()));
    }

    /**
     * Summarize recent MCP tool-call execution outcomes.
     */
    @GET
    @Path("/tools/calls/summary")
    @Authenticated
    @Operation(summary = "Summarize recent MCP tool calls")
    public Uni<RestResponse<McpToolCallHistorySummary>> summarizeToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .summarize(resourceQuery.runId(), resourceQuery.summaryQuery()));
    }

    /**
     * Summarize recent MCP tool-call execution outcomes per tool.
     */
    @GET
    @Path("/tools/calls/tools/summary")
    @Authenticated
    @Operation(summary = "Summarize recent MCP tool calls per tool")
    public Uni<RestResponse<McpToolCallHistoryToolSummaries>> summarizeToolCallsByTool(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .summarizeTools(resourceQuery.runId(), resourceQuery.summaryQuery()));
    }

    /**
     * Summarize recent MCP tool-call failures by failure type.
     */
    @GET
    @Path("/tools/calls/failures/summary")
    @Authenticated
    @Operation(summary = "Summarize recent MCP tool-call failures")
    public Uni<RestResponse<McpToolCallHistoryFailureSummaries>> summarizeToolCallFailures(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .summarizeFailures(resourceQuery.runId(), resourceQuery.summaryQuery()));
    }

    /**
     * Inspect MCP tool-call execution history storage.
     */
    @GET
    @Path("/tools/calls/stats")
    @Authenticated
    @Operation(summary = "Inspect MCP tool-call history storage")
    public Uni<RestResponse<McpToolCallHistoryStats>> getToolCallHistoryStats(
            @BeanParam McpToolCallHistoryQueryParams query) {

        return McpResourceSupport.ok(toolCallHistoryService()
                .stats(resourceQuery(query).runId()));
    }

    /**
     * Inspect MCP tool-call execution history storage across all runs.
     */
    @GET
    @Path("/tools/calls/stats/all")
    @Authenticated
    @Operation(summary = "Inspect all MCP tool-call history storage")
    public Uni<RestResponse<McpToolCallHistoryStats>> getAllToolCallHistoryStats() {
        return McpResourceSupport.ok(toolCallHistoryService().stats());
    }

    /**
     * Prune expired MCP tool-call execution outcomes for a run.
     */
    @POST
    @Path("/tools/calls/prune-expired")
    @Authenticated
    @Operation(summary = "Prune expired MCP tool calls")
    public Uni<RestResponse<McpToolCallHistoryPruneResult>> pruneExpiredToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        return McpResourceSupport.ok(toolCallHistoryService()
                .pruneExpired(resourceQuery(query).runId()));
    }

    /**
     * Prune expired MCP tool-call execution outcomes across all runs.
     */
    @POST
    @Path("/tools/calls/prune-expired/all")
    @Authenticated
    @Operation(summary = "Prune expired MCP tool calls across all runs")
    public Uni<RestResponse<McpToolCallHistoryPruneResult>> pruneAllExpiredToolCalls() {
        return McpResourceSupport.ok(toolCallHistoryService().pruneExpired());
    }

    /**
     * Preview how many MCP tool-call execution outcomes would be cleared.
     */
    @GET
    @Path("/tools/calls/clear-preview")
    @Authenticated
    @Operation(summary = "Preview clearing MCP tool calls")
    public Uni<RestResponse<McpToolCallHistoryClearPreview>> previewClearToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(toolCallHistoryService()
                .previewClear(resourceQuery.runId(), resourceQuery.historyQuery()));
    }

    /**
     * Clear recent MCP tool-call execution outcomes for a run.
     */
    @DELETE
    @Path("/tools/calls")
    @Authenticated
    @Operation(summary = "Clear recent MCP tool calls")
    public Uni<RestResponse<McpToolCallHistoryClearResult>> clearToolCalls(
            @BeanParam McpToolCallHistoryQueryParams query) {

        McpToolCallHistoryResourceQuery resourceQuery = resourceQuery(query);
        return McpResourceSupport.ok(McpHistoryMutationResultSupport.timestamped(
                        toolCallHistoryService().clear(resourceQuery.runId(), resourceQuery.historyQuery()),
                        McpToolCallHistoryClearResult::new));
    }

    private McpToolCallHistoryResourceQuery resourceQuery(McpToolCallHistoryQueryParams query) {
        return McpToolCallHistoryResourceQuery.from(query, requestContext);
    }

    private McpToolCallHistoryService toolCallHistoryService() {
        return toolCallHistoryService == null ? fallbackService : toolCallHistoryService;
    }
}
