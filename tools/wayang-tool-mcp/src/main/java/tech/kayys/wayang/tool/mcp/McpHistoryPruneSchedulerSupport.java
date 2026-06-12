package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

final class McpHistoryPruneSchedulerSupport {

    private McpHistoryPruneSchedulerSupport() {
    }

    static <R> Uni<R> pruneIfEnabled(
            boolean enabled,
            Supplier<Uni<R>> prune,
            Function<Instant, R> disabledResult) {
        if (!enabled) {
            return Uni.createFrom().item(disabledResult.apply(Instant.now()));
        }
        return prune.get();
    }

    static <R> Uni<R> pruneIfEnabled(
            boolean enabled,
            Supplier<Uni<R>> prune,
            BiFunction<Integer, Instant, R> disabledResult) {
        return pruneIfEnabled(enabled, prune, prunedAt -> disabledResult.apply(0, prunedAt));
    }

    static <R> void dispatch(
            String label,
            Supplier<Uni<R>> prune,
            ToIntFunction<R> pruned,
            Logger log) {
        try {
            prune.get()
                    .subscribe()
                    .with(result -> log.debug("{}: pruned={}", label, pruned.applyAsInt(result)),
                            error -> log.warn("{} failed: {}", label, error.getMessage()));
        } catch (Exception error) {
            log.warn("{} dispatch failed: {}", label, error.getMessage());
        }
    }
}
