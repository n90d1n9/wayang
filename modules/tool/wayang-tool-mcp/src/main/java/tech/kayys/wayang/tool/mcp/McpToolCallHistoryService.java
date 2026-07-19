package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class McpToolCallHistoryService {

    private final McpToolCallHistoryStore fallbackStore = new InMemoryMcpToolCallHistoryStore();

    @Inject
    McpToolCallHistoryStore historyStore;

    public McpToolCallHistoryService() {
    }

    McpToolCallHistoryService(McpToolCallHistoryStore historyStore) {
        this.historyStore = historyStore;
    }

    public Uni<McpToolCallResult> record(
            NodeExecutionTask task,
            String toolId,
            McpToolCallResult result,
            Instant startedAt) {
        if (result == null) {
            return Uni.createFrom().nullItem();
        }
        McpToolCallHistoryEntry entry = McpToolCallHistoryEntry.from(
                task,
                toolId,
                result,
                startedAt,
                Instant.now());
        return historyStore()
                .append(entry.runId(), entry)
                .replaceWith(result);
    }

    public Uni<List<McpToolCallHistoryEntry>> list(String runId) {
        return list(runId, McpToolCallHistoryQuery.of(null, null, null, null, 0));
    }

    public Uni<List<McpToolCallHistoryEntry>> list(
            String runId,
            String toolId,
            String status,
            Boolean success,
            String failureType,
            int limit) {
        return list(runId, McpToolCallHistoryQuery.of(toolId, status, success, failureType, limit));
    }

    Uni<List<McpToolCallHistoryEntry>> list(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.pageEntries(entries, filters));
    }

    Uni<McpToolCallHistoryPage> page(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.page(entries, filters));
    }

    Uni<List<McpToolCallHistoryEntry>> latest(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.latestEntries(entries, filters));
    }

    Uni<McpToolCallHistoryPage> latestPage(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.latestPage(entries, filters));
    }

    public Uni<McpToolCallHistorySummary> summarize(
            String runId,
            String toolId,
            String status,
            Boolean success,
            String failureType,
            int limit) {
        return summarize(runId, McpToolCallHistoryQuery.of(toolId, status, success, failureType, limit));
    }

    Uni<McpToolCallHistorySummary> summarize(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.summaryEntries(entries, filters))
                .map(McpToolCallHistorySummaries::summary);
    }

    Uni<McpToolCallHistoryToolSummaries> summarizeTools(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.summaryEntries(entries, filters))
                .map(McpToolCallHistorySummaries::toolSummaries);
    }

    Uni<McpToolCallHistoryFailureSummaries> summarizeFailures(String runId, McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = filters(query);
        return historyReader()
                .mapFilteredEntries(
                        runId,
                        filters,
                        entries -> McpToolCallHistoryViews.summaryEntries(entries, filters))
                .map(McpToolCallHistorySummaries::failureSummaries);
    }

    public Uni<McpToolCallHistoryStats> stats(String runId) {
        return historyStore().stats(runId);
    }

    public Uni<McpToolCallHistoryStats> stats() {
        return historyStore().stats();
    }

    public Uni<McpToolCallHistoryPruneResult> pruneExpired(String runId) {
        return McpHistoryMutationResultSupport.timestamped(
                runId,
                historyStore()::pruneExpired,
                McpToolCallHistoryPruneResult::new);
    }

    public Uni<McpToolCallHistoryPruneResult> pruneExpired() {
        return McpHistoryMutationResultSupport.timestamped(
                historyStore()::pruneExpired,
                McpToolCallHistoryPruneResult::new);
    }

    Uni<McpToolCallHistoryClearPreview> previewClear(
            String runId,
            McpToolCallHistoryQuery query) {
        McpToolCallHistoryFilters filters = query == null ? null : query.filters();
        return McpHistoryMutationResultSupport.timestamped(
                runId,
                filters,
                historyStore()::count,
                McpToolCallHistoryClearPreview::new);
    }

    public Uni<Integer> clear(String runId) {
        return historyStore().clear(runId);
    }

    Uni<Integer> clear(String runId, McpToolCallHistoryQuery query) {
        return McpHistoryMutationResultSupport.scopedMutation(
                runId,
                query == null ? null : query.filters(),
                historyStore()::clear,
                historyStore()::clear);
    }

    private static McpToolCallHistoryFilters filters(McpToolCallHistoryQuery query) {
        return query == null
                ? McpToolCallHistoryQuery.of(null, null, null, null, 0).filters()
                : query.filters();
    }

    private McpHistoryScopedReader<McpToolCallHistoryEntry, McpToolCallHistoryFilters> historyReader() {
        return McpHistoryScopedReader.of(
                historyStore()::list,
                McpToolCallHistoryViews::filteredEntries);
    }

    private McpToolCallHistoryStore historyStore() {
        return historyStore == null ? fallbackStore : historyStore;
    }
}
