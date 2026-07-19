package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class McpServerActionExecutionHistoryService {

    private final McpServerActionExecutionHistoryStore fallbackStore =
            new InMemoryMcpServerActionExecutionHistoryStore();

    @Inject
    McpServerActionExecutionHistoryStore historyStore;

    public Uni<McpServerActionExecutionResult> record(
            String requestId,
            McpServerActionExecutionResult result) {
        if (result == null) {
            return Uni.createFrom().nullItem();
        }
        return historyStore()
                .append(requestId, McpServerActionExecutionHistoryEntry.from(result))
                .replaceWith(result);
    }

    public Uni<McpServerActionExecutionHistoryClearResult> clear(String requestId) {
        return McpHistoryMutationResultSupport.timestamped(
                requestId,
                historyStore()::clear,
                McpServerActionExecutionHistoryClearResult::new);
    }

    public Uni<McpServerActionExecutionHistoryPruneResult> pruneExpired(String requestId) {
        return McpHistoryMutationResultSupport.timestamped(
                requestId,
                historyStore()::pruneExpired,
                McpServerActionExecutionHistoryPruneResult::new);
    }

    public Uni<McpServerActionExecutionHistoryPruneResult> pruneExpired() {
        return McpHistoryMutationResultSupport.timestamped(
                historyStore()::pruneExpired,
                McpServerActionExecutionHistoryPruneResult::new);
    }

    public Uni<McpServerActionExecutionHistoryStats> stats(String requestId) {
        return historyStore().stats(requestId);
    }

    public Uni<McpServerActionExecutionHistoryStats> stats() {
        return historyStore().stats();
    }

    Uni<McpServerActionExecutionHistoryClearPreview> previewClear(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        McpServerActionExecutionHistoryFilters filters = query == null ? null : query.filters();
        return McpHistoryMutationResultSupport.timestamped(
                requestId,
                filters,
                historyStore()::count,
                McpServerActionExecutionHistoryClearPreview::new);
    }

    Uni<McpServerActionExecutionHistoryClearResult> clear(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        return McpHistoryMutationResultSupport.timestamped(
                requestId,
                query.filters(),
                historyStore()::clear,
                McpServerActionExecutionHistoryClearResult::new);
    }

    public Uni<List<McpServerActionExecutionHistoryEntry>> list(
            String requestId,
            String serverName,
            String actionCode,
            String status,
            int limit) {
        return list(requestId, serverName, actionCode, status, null, null, null, null, limit);
    }

    public Uni<List<McpServerActionExecutionHistoryEntry>> list(
            String requestId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            int limit) {
        return list(requestId, query(
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                hasWarnings,
                limit));
    }

    Uni<List<McpServerActionExecutionHistoryEntry>> list(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        McpServerActionExecutionHistoryFilters filters = query.filters();
        return historyReader()
                .mapFilteredEntries(
                        requestId,
                        filters,
                        entries -> McpServerActionExecutionHistoryViews.pageEntries(entries, filters));
    }

    Uni<McpServerActionExecutionHistoryPage> page(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        McpServerActionExecutionHistoryFilters filters = query.filters();
        return historyReader()
                .mapFilteredEntries(
                        requestId,
                        filters,
                        entries -> McpServerActionExecutionHistoryViews.page(entries, filters));
    }

    public Uni<List<McpServerActionExecutionHistoryEntry>> listLatest(
            String requestId,
            String serverName,
            String actionCode,
            String status,
            int limit) {
        return listLatest(requestId, serverName, actionCode, status, null, null, null, null, limit);
    }

    public Uni<List<McpServerActionExecutionHistoryEntry>> listLatest(
            String requestId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            int limit) {
        return listLatest(requestId, query(
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                hasWarnings,
                limit));
    }

    Uni<List<McpServerActionExecutionHistoryEntry>> listLatest(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        McpServerActionExecutionHistoryFilters filters = query.latestFilters();
        return historyReader()
                .mapFilteredEntries(
                        requestId,
                        filters,
                        entries -> McpServerActionExecutionHistoryViews.latestEntries(entries, filters));
    }

    Uni<McpServerActionExecutionHistoryPage> pageLatest(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        McpServerActionExecutionHistoryFilters filters = query.latestFilters();
        return historyReader()
                .mapFilteredEntries(
                        requestId,
                        filters,
                        entries -> McpServerActionExecutionHistoryViews.latestPage(entries, filters));
    }

    public Uni<McpServerActionExecutionHistorySummary> summarize(
            String requestId,
            String serverName,
            String actionCode,
            String status,
            int limit) {
        return summarize(requestId, serverName, actionCode, status, null, null, null, null, limit);
    }

    public Uni<McpServerActionExecutionHistorySummary> summarize(
            String requestId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            int limit) {
        return summarize(requestId, query(
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                hasWarnings,
                limit));
    }

    Uni<McpServerActionExecutionHistorySummary> summarize(
            String requestId,
            McpServerActionExecutionHistoryQuery query) {
        McpServerActionExecutionHistoryFilters filters = query.filters();
        return historyReader()
                .mapFilteredEntries(
                        requestId,
                        filters,
                        McpServerActionExecutionHistorySummaries::summary);
    }

    private static McpServerActionExecutionHistoryQuery query(
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            int limit) {
        return McpServerActionExecutionHistoryQuery.builder()
                .withServerName(serverName)
                .withActionCode(actionCode)
                .withStatus(status)
                .withExecuted(executed)
                .withExecutionMode(executionMode)
                .withRiskLevel(riskLevel)
                .withWarnings(hasWarnings)
                .withLimit(limit)
                .build();
    }

    private McpHistoryScopedReader<
            McpServerActionExecutionHistoryEntry,
            McpServerActionExecutionHistoryFilters> historyReader() {
        return McpHistoryScopedReader.of(
                historyStore()::list,
                McpServerActionExecutionHistoryViews::filteredEntries);
    }

    private McpServerActionExecutionHistoryStore historyStore() {
        return historyStore == null ? fallbackStore : historyStore;
    }
}
