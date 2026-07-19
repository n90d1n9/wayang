package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface McpToolCallHistoryStore {

    Uni<Void> append(String runId, McpToolCallHistoryEntry entry);

    Uni<List<McpToolCallHistoryEntry>> list(String runId);

    Uni<Integer> count(String runId, McpToolCallHistoryFilters filters);

    default Uni<McpToolCallHistoryStats> stats() {
        return Uni.createFrom().item(McpToolCallHistoryStats.empty());
    }

    Uni<McpToolCallHistoryStats> stats(String runId);

    default Uni<Integer> pruneExpired() {
        return Uni.createFrom().item(0);
    }

    Uni<Integer> pruneExpired(String runId);

    Uni<Integer> clear(String runId);

    Uni<Integer> clear(String runId, McpToolCallHistoryFilters filters);
}
