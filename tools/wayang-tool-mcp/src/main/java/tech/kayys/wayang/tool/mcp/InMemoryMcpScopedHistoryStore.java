package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

final class InMemoryMcpScopedHistoryStore<T> {

    private final Map<String, ArrayDeque<T>> entriesByScope = new LinkedHashMap<>();
    private final McpHistoryStoreSupport.RetentionPolicy retentionPolicy;
    private final Function<T, Instant> timestamp;

    InMemoryMcpScopedHistoryStore(
            McpHistoryStoreSupport.RetentionPolicy retentionPolicy,
            Function<T, Instant> timestamp) {
        this.retentionPolicy = Objects.requireNonNull(retentionPolicy, "retentionPolicy");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    synchronized void append(String scopeId, T entry) {
        if (entry == null) {
            return;
        }
        pruneExpiredEntries();
        McpHistoryStoreSupport.appendNewest(
                entriesByScope,
                scopeId,
                entry,
                retentionPolicy.cutoff(),
                timestamp,
                retentionPolicy.maxEntries());
    }

    synchronized List<T> list(String scopeId) {
        pruneExpiredEntries();
        return McpHistoryStoreSupport.snapshot(entriesByScope, scopeId);
    }

    synchronized int count(String scopeId, Predicate<T> predicate) {
        pruneExpiredEntries();
        return McpHistoryStoreSupport.count(entriesByScope, scopeId, predicate);
    }

    synchronized Stats stats() {
        pruneExpiredEntries();
        return stats(entriesByScope.size(), McpHistoryStoreSupport.entryStats(entriesByScope, timestamp));
    }

    synchronized Stats stats(String scopeId) {
        pruneExpiredEntries();
        McpHistoryStoreSupport.EntryStats entryStats =
                McpHistoryStoreSupport.entryStats(entriesByScope, scopeId, timestamp);
        return stats(entryStats.entries() > 0 ? 1 : 0, entryStats);
    }

    synchronized int pruneExpired() {
        return pruneExpiredEntries();
    }

    synchronized int pruneExpired(String scopeId) {
        return McpHistoryStoreSupport.pruneExpiredScope(
                entriesByScope,
                scopeId,
                retentionPolicy.cutoff(),
                timestamp);
    }

    synchronized int clear(String scopeId) {
        return McpHistoryStoreSupport.clearScope(entriesByScope, scopeId);
    }

    synchronized int clear(String scopeId, Predicate<T> predicate) {
        if (predicate == null) {
            return clear(scopeId);
        }
        pruneExpiredEntries();
        return McpHistoryStoreSupport.removeMatching(entriesByScope, scopeId, predicate);
    }

    private int pruneExpiredEntries() {
        return McpHistoryStoreSupport.pruneExpired(entriesByScope, retentionPolicy.cutoff(), timestamp);
    }

    private Stats stats(int scopes, McpHistoryStoreSupport.EntryStats entryStats) {
        return new Stats(
                scopes,
                entryStats.entries(),
                retentionPolicy.maxEntries(),
                retentionPolicy.retentionSeconds(),
                entryStats.oldestEntryAt(),
                entryStats.newestEntryAt(),
                retentionPolicy.now());
    }

    record Stats(
            int scopes,
            int entries,
            int maxEntries,
            long retentionSeconds,
            Instant oldestEntryAt,
            Instant newestEntryAt,
            Instant generatedAt) {
    }
}
