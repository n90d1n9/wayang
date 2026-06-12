package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

final class McpHistoryScopedReader<T, F> {
    private final Function<String, Uni<List<T>>> snapshots;
    private final BiFunction<List<T>, F, List<T>> filterer;

    private McpHistoryScopedReader(
            Function<String, Uni<List<T>>> snapshots,
            BiFunction<List<T>, F, List<T>> filterer) {
        this.snapshots = snapshots;
        this.filterer = filterer;
    }

    static <T, F> McpHistoryScopedReader<T, F> of(
            Function<String, Uni<List<T>>> snapshots,
            BiFunction<List<T>, F, List<T>> filterer) {
        return new McpHistoryScopedReader<>(snapshots, filterer);
    }

    Uni<List<T>> filteredEntries(String scopeId, F filters) {
        return snapshots.apply(scopeId)
                .map(snapshot -> filterer.apply(snapshot, filters));
    }

    <R> Uni<R> mapFilteredEntries(
            String scopeId,
            F filters,
            Function<List<T>, R> mapper) {
        return filteredEntries(scopeId, filters).map(mapper);
    }
}
