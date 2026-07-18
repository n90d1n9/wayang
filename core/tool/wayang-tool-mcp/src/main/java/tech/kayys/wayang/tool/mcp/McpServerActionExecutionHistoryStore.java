package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface McpServerActionExecutionHistoryStore {

    Uni<Void> append(String requestId, McpServerActionExecutionHistoryEntry entry);

    Uni<List<McpServerActionExecutionHistoryEntry>> list(String requestId);

    Uni<Integer> count(String requestId, McpServerActionExecutionHistoryFilters filters);

    default Uni<McpServerActionExecutionHistoryStats> stats() {
        return Uni.createFrom().item(McpServerActionExecutionHistoryStats.empty());
    }

    Uni<McpServerActionExecutionHistoryStats> stats(String requestId);

    default Uni<Integer> pruneExpired() {
        return Uni.createFrom().item(0);
    }

    Uni<Integer> pruneExpired(String requestId);

    Uni<Integer> clear(String requestId);

    Uni<Integer> clear(String requestId, McpServerActionExecutionHistoryFilters filters);
}
