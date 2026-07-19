package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class InMemoryMcpToolCallHistoryStore implements McpToolCallHistoryStore {

    private static final int DEFAULT_MAX_ENTRIES_PER_RUN = McpHistoryRetention.DEFAULT_MAX_ENTRIES;
    private static final int MAX_CONFIGURED_ENTRIES_PER_RUN = McpHistoryRetention.MAX_CONFIGURED_ENTRIES;
    private static final Duration DEFAULT_RETENTION = McpHistoryRetention.DEFAULT_RETENTION;

    private final McpInMemoryScopedHistoryOperations<McpToolCallHistoryEntry, McpToolCallHistoryFilters> history;

    public InMemoryMcpToolCallHistoryStore() {
        this(DEFAULT_RETENTION, DEFAULT_MAX_ENTRIES_PER_RUN, Clock.systemUTC());
    }

    @Inject
    public InMemoryMcpToolCallHistoryStore(
            @ConfigProperty(
                    name = "wayang.mcp.tool-call-history.retention",
                    defaultValue = "P7D") String retention,
            @ConfigProperty(
                    name = "wayang.mcp.tool-call-history.max-entries-per-run",
                    defaultValue = "500") int maxEntriesPerRun) {
        this(McpHistoryRetention.retentionFromConfig(retention), maxEntriesPerRun, Clock.systemUTC());
    }

    InMemoryMcpToolCallHistoryStore(int maxEntriesPerRun) {
        this(DEFAULT_RETENTION, maxEntriesPerRun, Clock.systemUTC());
    }

    InMemoryMcpToolCallHistoryStore(Duration retention, Clock clock) {
        this(retention, DEFAULT_MAX_ENTRIES_PER_RUN, clock);
    }

    InMemoryMcpToolCallHistoryStore(
            Duration retention,
            int maxEntriesPerRun,
            Clock clock) {
        this.history = McpInMemoryScopedHistoryOperations.of(
                McpHistoryStoreSupport.RetentionPolicy.of(
                        retention,
                        maxEntriesPerRun,
                        DEFAULT_MAX_ENTRIES_PER_RUN,
                        MAX_CONFIGURED_ENTRIES_PER_RUN,
                        clock),
                McpToolCallHistorySummaryKeys::sortFinishedAt,
                filters -> filters::matches);
    }

    @Override
    public Uni<Void> append(String runId, McpToolCallHistoryEntry entry) {
        return history.append(runId, entry);
    }

    @Override
    public Uni<List<McpToolCallHistoryEntry>> list(String runId) {
        return history.list(runId);
    }

    @Override
    public Uni<Integer> count(String runId, McpToolCallHistoryFilters filters) {
        return history.count(runId, filters);
    }

    @Override
    public Uni<McpToolCallHistoryStats> stats() {
        return history.stats().map(this::stats);
    }

    @Override
    public Uni<McpToolCallHistoryStats> stats(String runId) {
        return history.stats(runId).map(this::stats);
    }

    @Override
    public Uni<Integer> pruneExpired() {
        return history.pruneExpired();
    }

    @Override
    public Uni<Integer> pruneExpired(String runId) {
        return history.pruneExpired(runId);
    }

    @Override
    public Uni<Integer> clear(String runId) {
        return history.clear(runId);
    }

    @Override
    public Uni<Integer> clear(String runId, McpToolCallHistoryFilters filters) {
        return history.clear(runId, filters);
    }

    private McpToolCallHistoryStats stats(InMemoryMcpScopedHistoryStore.Stats stats) {
        return new McpToolCallHistoryStats(
                stats.scopes(),
                stats.entries(),
                stats.maxEntries(),
                stats.retentionSeconds(),
                stats.oldestEntryAt(),
                stats.newestEntryAt(),
                stats.generatedAt());
    }

}
