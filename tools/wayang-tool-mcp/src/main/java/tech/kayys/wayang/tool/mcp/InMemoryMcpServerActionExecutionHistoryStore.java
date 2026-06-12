package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class InMemoryMcpServerActionExecutionHistoryStore implements McpServerActionExecutionHistoryStore {

    private static final int DEFAULT_MAX_ENTRIES_PER_REQUEST = McpHistoryRetention.DEFAULT_MAX_ENTRIES;
    private static final int MAX_CONFIGURED_ENTRIES_PER_REQUEST = McpHistoryRetention.MAX_CONFIGURED_ENTRIES;
    private static final Duration DEFAULT_RETENTION = McpHistoryRetention.DEFAULT_RETENTION;

    private final McpInMemoryScopedHistoryOperations<
            McpServerActionExecutionHistoryEntry,
            McpServerActionExecutionHistoryFilters> history;

    public InMemoryMcpServerActionExecutionHistoryStore() {
        this(DEFAULT_RETENTION, DEFAULT_MAX_ENTRIES_PER_REQUEST, Clock.systemUTC());
    }

    @Inject
    public InMemoryMcpServerActionExecutionHistoryStore(
            @ConfigProperty(
                    name = "wayang.mcp.action-history.retention",
                    defaultValue = "P7D") String retention,
            @ConfigProperty(
                    name = "wayang.mcp.action-history.max-entries-per-request",
                    defaultValue = "500") int maxEntriesPerRequest) {
        this(McpHistoryRetention.retentionFromConfig(retention), maxEntriesPerRequest, Clock.systemUTC());
    }

    InMemoryMcpServerActionExecutionHistoryStore(Duration retention) {
        this(retention, DEFAULT_MAX_ENTRIES_PER_REQUEST, Clock.systemUTC());
    }

    InMemoryMcpServerActionExecutionHistoryStore(Duration retention, Clock clock) {
        this(retention, DEFAULT_MAX_ENTRIES_PER_REQUEST, clock);
    }

    InMemoryMcpServerActionExecutionHistoryStore(
            Duration retention,
            int maxEntriesPerRequest,
            Clock clock) {
        this.history = McpInMemoryScopedHistoryOperations.of(
                McpHistoryStoreSupport.RetentionPolicy.of(
                        retention,
                        maxEntriesPerRequest,
                        DEFAULT_MAX_ENTRIES_PER_REQUEST,
                        MAX_CONFIGURED_ENTRIES_PER_REQUEST,
                        clock),
                McpServerActionExecutionHistorySummaryKeys::sortFinishedAt,
                filters -> filters::matches);
    }

    @Override
    public Uni<Void> append(String requestId, McpServerActionExecutionHistoryEntry entry) {
        return history.append(requestId, entry);
    }

    @Override
    public Uni<List<McpServerActionExecutionHistoryEntry>> list(String requestId) {
        return history.list(requestId);
    }

    @Override
    public Uni<Integer> count(String requestId, McpServerActionExecutionHistoryFilters filters) {
        return history.count(requestId, filters);
    }

    @Override
    public Uni<McpServerActionExecutionHistoryStats> stats() {
        return history.stats().map(this::stats);
    }

    @Override
    public Uni<McpServerActionExecutionHistoryStats> stats(String requestId) {
        return history.stats(requestId).map(this::stats);
    }

    @Override
    public Uni<Integer> pruneExpired() {
        return history.pruneExpired();
    }

    @Override
    public Uni<Integer> pruneExpired(String requestId) {
        return history.pruneExpired(requestId);
    }

    @Override
    public Uni<Integer> clear(String requestId) {
        return history.clear(requestId);
    }

    @Override
    public Uni<Integer> clear(String requestId, McpServerActionExecutionHistoryFilters filters) {
        return history.clear(requestId, filters);
    }

    private McpServerActionExecutionHistoryStats stats(InMemoryMcpScopedHistoryStore.Stats stats) {
        return new McpServerActionExecutionHistoryStats(
                stats.scopes(),
                stats.entries(),
                stats.maxEntries(),
                stats.retentionSeconds(),
                stats.oldestEntryAt(),
                stats.newestEntryAt(),
                stats.generatedAt());
    }

}
