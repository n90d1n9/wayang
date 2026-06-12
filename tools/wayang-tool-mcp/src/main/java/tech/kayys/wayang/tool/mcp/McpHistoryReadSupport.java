package tech.kayys.wayang.tool.mcp;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class McpHistoryReadSupport {

    private McpHistoryReadSupport() {
    }

    static <T> List<T> filterAndSort(
            List<T> entries,
            Predicate<T> predicate,
            Comparator<T> comparator) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        Stream<T> stream = entries.stream();
        if (predicate != null) {
            stream = stream.filter(predicate);
        }
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        return stream.toList();
    }

    static <T> List<T> filterAndSort(
            List<T> entries,
            Predicate<T> predicate,
            Comparator<T> comparator,
            int maxEntries) {
        if (maxEntries <= 0) {
            return List.of();
        }
        return filterAndSort(entries, predicate, comparator)
                .stream()
                .limit(maxEntries)
                .toList();
    }

    static <T> List<T> page(
            List<T> entries,
            int offset,
            int limit) {
        if (entries == null || entries.isEmpty() || limit <= 0) {
            return List.of();
        }
        return entries.stream()
                .skip(Math.max(0, offset))
                .limit(limit)
                .toList();
    }

    static boolean hasMore(
            List<?> entries,
            int offset,
            int limit) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        return entries.size() > (long) Math.max(0, offset) + Math.max(0, limit);
    }

    static <T> List<T> copyEntries(List<T> entries) {
        return entries == null ? List.of() : List.copyOf(entries);
    }

    static int returned(List<?> entries) {
        return entries == null ? 0 : entries.size();
    }

    static Integer nextOffset(
            int offset,
            int returned,
            boolean hasMore) {
        return hasMore ? Math.max(0, offset) + Math.max(0, returned) : null;
    }

    static <T, K> List<T> latestByKey(
            List<T> entries,
            Function<T, K> key,
            Comparator<T> comparator) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        Map<K, T> latestByKey = new LinkedHashMap<>();
        for (T entry : entries) {
            latestByKey.putIfAbsent(key.apply(entry), entry);
        }
        Stream<T> stream = latestByKey.values().stream();
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        return stream.toList();
    }
}
