package tech.kayys.wayang.tool.mcp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

final class McpHistoryStoreSupport {

    private McpHistoryStoreSupport() {
    }

    static String scopeKey(String scopeId) {
        return scopeId == null || scopeId.isBlank() ? "" : scopeId;
    }

    static <T> List<T> snapshot(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId) {
        ArrayDeque<T> entries = entriesByScope.get(scopeKey(scopeId));
        return entries == null ? List.of() : new ArrayList<>(entries);
    }

    static <T> int count(
            ArrayDeque<T> entries,
            Predicate<T> predicate) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        if (predicate == null) {
            return entries.size();
        }
        return Math.toIntExact(entries.stream()
                .filter(predicate)
                .count());
    }

    static <T> int count(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId,
            Predicate<T> predicate) {
        return count(entriesByScope.get(scopeKey(scopeId)), predicate);
    }

    static <T> int trimToMaxEntries(ArrayDeque<T> entries, int maxEntries) {
        if (entries == null) {
            return 0;
        }
        int before = entries.size();
        int effectiveMaxEntries = Math.max(0, maxEntries);
        while (entries.size() > effectiveMaxEntries) {
            entries.removeLast();
        }
        return before - entries.size();
    }

    static <T> void appendNewest(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId,
            T entry,
            Instant cutoff,
            Function<T, Instant> timestamp,
            int maxEntries) {
        if (entry == null) {
            return;
        }
        String key = scopeKey(scopeId);
        ArrayDeque<T> entries = entriesByScope.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        entries.addFirst(entry);
        pruneExpired(entries, cutoff, timestamp);
        trimToMaxEntries(entries, maxEntries);
        removeIfEmpty(entriesByScope, key, entries);
    }

    static <T> int clearScope(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId) {
        ArrayDeque<T> removed = entriesByScope.remove(scopeKey(scopeId));
        return removed == null ? 0 : removed.size();
    }

    static <T> int removeMatching(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId,
            Predicate<T> predicate) {
        if (predicate == null) {
            return clearScope(entriesByScope, scopeId);
        }
        String key = scopeKey(scopeId);
        ArrayDeque<T> entries = entriesByScope.get(key);
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        int removed = removeMatching(entries, predicate);
        if (entries.isEmpty()) {
            entriesByScope.remove(key);
        }
        return removed;
    }

    static <T> EntryStats entryStats(
            ArrayDeque<T> entries,
            Function<T, Instant> timestamp) {
        if (entries == null) {
            return EntryStats.empty();
        }
        return entryStats(List.of(entries), timestamp);
    }

    static <T> EntryStats entryStats(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId,
            Function<T, Instant> timestamp) {
        return entryStats(entriesByScope.get(scopeKey(scopeId)), timestamp);
    }

    static <T> EntryStats entryStats(
            Map<String, ArrayDeque<T>> entriesByScope,
            Function<T, Instant> timestamp) {
        return entryStats(entriesByScope.values(), timestamp);
    }

    static <T> int pruneExpired(
            Map<String, ArrayDeque<T>> entriesByScope,
            Instant cutoff,
            Function<T, Instant> timestamp) {
        int pruned = 0;
        Iterator<Map.Entry<String, ArrayDeque<T>>> iterator = entriesByScope.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ArrayDeque<T>> entry = iterator.next();
            pruned += pruneExpired(entry.getValue(), cutoff, timestamp);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
        return pruned;
    }

    static <T> int pruneExpired(
            ArrayDeque<T> entries,
            Instant cutoff,
            Function<T, Instant> timestamp) {
        int before = entries.size();
        entries.removeIf(entry -> timestamp.apply(entry).isBefore(cutoff));
        return before - entries.size();
    }

    static <T> int pruneExpiredScope(
            Map<String, ArrayDeque<T>> entriesByScope,
            String scopeId,
            Instant cutoff,
            Function<T, Instant> timestamp) {
        String key = scopeKey(scopeId);
        ArrayDeque<T> entries = entriesByScope.get(key);
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        int pruned = pruneExpired(entries, cutoff, timestamp);
        removeIfEmpty(entriesByScope, key, entries);
        return pruned;
    }

    private static <T> int removeMatching(
            ArrayDeque<T> entries,
            Predicate<T> predicate) {
        int before = entries.size();
        entries.removeIf(predicate);
        return before - entries.size();
    }

    private static <T> void removeIfEmpty(
            Map<String, ArrayDeque<T>> entriesByScope,
            String key,
            ArrayDeque<T> entries) {
        if (entries.isEmpty()) {
            entriesByScope.remove(key);
        }
    }

    static Instant min(Instant left, Instant right) {
        if (right == null) {
            return left;
        }
        if (left == null || right.isBefore(left)) {
            return right;
        }
        return left;
    }

    static Instant max(Instant left, Instant right) {
        if (right == null) {
            return left;
        }
        if (left == null || right.isAfter(left)) {
            return right;
        }
        return left;
    }

    private static <T> EntryStats entryStats(
            Iterable<ArrayDeque<T>> scopes,
            Function<T, Instant> timestamp) {
        int count = 0;
        Instant oldestEntryAt = null;
        Instant newestEntryAt = null;
        for (ArrayDeque<T> entries : scopes) {
            if (entries == null) {
                continue;
            }
            for (T entry : entries) {
                count++;
                Instant entryAt = timestamp.apply(entry);
                oldestEntryAt = min(oldestEntryAt, entryAt);
                newestEntryAt = max(newestEntryAt, entryAt);
            }
        }
        return new EntryStats(count, oldestEntryAt, newestEntryAt);
    }

    record EntryStats(
            int entries,
            Instant oldestEntryAt,
            Instant newestEntryAt) {

        private static EntryStats empty() {
            return new EntryStats(0, null, null);
        }
    }

    record RetentionPolicy(
            Duration retention,
            int maxEntries,
            Clock clock) {

        static RetentionPolicy of(
                Duration retention,
                int maxEntries,
                int defaultMaxEntries,
                int maxConfiguredEntries,
                Clock clock) {
            return new RetentionPolicy(
                    retention,
                    McpHistoryRetention.normalizeMaxEntries(
                            maxEntries,
                            defaultMaxEntries,
                            maxConfiguredEntries),
                    clock);
        }

        RetentionPolicy {
            retention = McpHistoryRetention.normalizeRetention(retention);
            maxEntries = Math.max(0, maxEntries);
            clock = clock == null ? Clock.systemUTC() : clock;
        }

        Instant cutoff() {
            return now().minus(retention);
        }

        long retentionSeconds() {
            return retention.toSeconds();
        }

        Instant now() {
            return clock.instant();
        }
    }
}
