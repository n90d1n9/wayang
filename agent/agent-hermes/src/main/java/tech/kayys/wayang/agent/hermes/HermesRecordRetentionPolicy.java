package tech.kayys.wayang.agent.hermes;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared bounded-retention rule for append-only Hermes journals and ledgers.
 */
record HermesRecordRetentionPolicy(int maxEntries) {

    static final int DEFAULT_MAX_ENTRIES = 10_000;

    HermesRecordRetentionPolicy {
        maxEntries = Math.max(maxEntries, 1);
    }

    static HermesRecordRetentionPolicy bounded(int maxEntries) {
        return new HermesRecordRetentionPolicy(maxEntries);
    }

    static HermesRecordRetentionPolicy orDefault(HermesRecordRetentionPolicy policy) {
        return policy == null ? bounded(DEFAULT_MAX_ENTRIES) : policy;
    }

    int staleCount(int totalEntries) {
        return Math.max(totalEntries - maxEntries, 0);
    }

    boolean allowsAll(int totalEntries) {
        return staleCount(totalEntries) == 0;
    }

    <T> List<T> retainNewestFromAppendOrder(List<T> appendOrderedEntries) {
        List<T> entries = appendOrderedEntries == null ? List.of() : appendOrderedEntries;
        int staleEntries = staleCount(entries.size());
        if (staleEntries == 0) {
            return List.copyOf(entries);
        }
        return List.copyOf(entries.subList(staleEntries, entries.size()));
    }

    <T> List<T> staleFromNewestFirst(List<T> newestFirstEntries) {
        List<T> entries = newestFirstEntries == null ? List.of() : newestFirstEntries;
        int staleEntries = staleCount(entries.size());
        if (staleEntries == 0) {
            return List.of();
        }
        return List.copyOf(entries.subList(maxEntries, entries.size()));
    }

    <T> List<T> staleFromOldestFirst(
            Collection<T> entries,
            Comparator<T> oldestFirstComparator) {
        List<T> orderedEntries = entries == null ? List.of() : entries.stream()
                .sorted(oldestFirstComparator)
                .toList();
        int staleEntries = staleCount(orderedEntries.size());
        if (staleEntries == 0) {
            return List.of();
        }
        return List.copyOf(orderedEntries.subList(0, staleEntries));
    }

    Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("retentionMode", "max-entries");
        values.put("maxEntries", maxEntries);
        return Map.copyOf(values);
    }
}
