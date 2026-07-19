package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

final class McpInMemoryScopedHistoryOperations<T, F> {
    private final InMemoryMcpScopedHistoryStore<T> history;
    private final Function<F, Predicate<T>> filterPredicate;

    private McpInMemoryScopedHistoryOperations(
            InMemoryMcpScopedHistoryStore<T> history,
            Function<F, Predicate<T>> filterPredicate) {
        this.history = Objects.requireNonNull(history, "history");
        this.filterPredicate = Objects.requireNonNull(filterPredicate, "filterPredicate");
    }

    static <T, F> McpInMemoryScopedHistoryOperations<T, F> of(
            McpHistoryStoreSupport.RetentionPolicy retentionPolicy,
            Function<T, Instant> timestamp,
            Function<F, Predicate<T>> filterPredicate) {
        return new McpInMemoryScopedHistoryOperations<>(
                new InMemoryMcpScopedHistoryStore<>(retentionPolicy, timestamp),
                filterPredicate);
    }

    Uni<Void> append(String scopeId, T entry) {
        history.append(scopeId, entry);
        return Uni.createFrom().voidItem();
    }

    Uni<List<T>> list(String scopeId) {
        return Uni.createFrom().item(history.list(scopeId));
    }

    Uni<Integer> count(String scopeId, F filters) {
        return Uni.createFrom().item(history.count(scopeId, predicate(filters)));
    }

    Uni<InMemoryMcpScopedHistoryStore.Stats> stats() {
        return Uni.createFrom().item(history.stats());
    }

    Uni<InMemoryMcpScopedHistoryStore.Stats> stats(String scopeId) {
        return Uni.createFrom().item(history.stats(scopeId));
    }

    Uni<Integer> pruneExpired() {
        return Uni.createFrom().item(history.pruneExpired());
    }

    Uni<Integer> pruneExpired(String scopeId) {
        return Uni.createFrom().item(history.pruneExpired(scopeId));
    }

    Uni<Integer> clear(String scopeId) {
        return Uni.createFrom().item(history.clear(scopeId));
    }

    Uni<Integer> clear(String scopeId, F filters) {
        return Uni.createFrom().item(history.clear(scopeId, predicate(filters)));
    }

    private Predicate<T> predicate(F filters) {
        return filters == null ? null : filterPredicate.apply(filters);
    }
}
