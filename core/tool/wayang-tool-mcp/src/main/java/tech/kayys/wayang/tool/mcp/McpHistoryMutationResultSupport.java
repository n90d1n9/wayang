package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class McpHistoryMutationResultSupport {

    private McpHistoryMutationResultSupport() {
    }

    static <R> Uni<R> timestamped(
            Uni<Integer> count,
            BiFunction<Integer, Instant, R> result) {
        return count.map(value -> result.apply(value == null ? 0 : value, Instant.now()));
    }

    static <R> Uni<R> timestamped(
            Supplier<Uni<Integer>> count,
            BiFunction<Integer, Instant, R> result) {
        return timestamped(count.get(), result);
    }

    static <R> Uni<R> timestamped(
            String scopeId,
            Function<String, Uni<Integer>> count,
            BiFunction<Integer, Instant, R> result) {
        return timestamped(count.apply(scopeId), result);
    }

    static <F, R> Uni<R> timestamped(
            String scopeId,
            F filters,
            BiFunction<String, F, Uni<Integer>> count,
            BiFunction<Integer, Instant, R> result) {
        return timestamped(count.apply(scopeId, filters), result);
    }

    static <F> Uni<Integer> scopedMutation(
            String scopeId,
            F filters,
            Function<String, Uni<Integer>> mutation,
            BiFunction<String, F, Uni<Integer>> filteredMutation) {
        if (filters == null) {
            return mutation.apply(scopeId);
        }
        return filteredMutation.apply(scopeId, filters);
    }
}
