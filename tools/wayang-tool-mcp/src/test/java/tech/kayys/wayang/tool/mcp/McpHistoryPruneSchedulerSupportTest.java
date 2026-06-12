package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class McpHistoryPruneSchedulerSupportTest {

    @Test
    void pruneIfEnabledReturnsDisabledResultWithoutCallingPrune() {
        AtomicInteger calls = new AtomicInteger();

        PruneResult result = McpHistoryPruneSchedulerSupport.pruneIfEnabled(
                        false,
                        () -> {
                            calls.incrementAndGet();
                            return Uni.createFrom().item(new PruneResult(99, Instant.EPOCH));
                        },
                        prunedAt -> new PruneResult(0, prunedAt))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(0, calls.get());
    }

    @Test
    void pruneIfEnabledDelegatesToPrune() {
        AtomicInteger calls = new AtomicInteger();

        PruneResult result = McpHistoryPruneSchedulerSupport.pruneIfEnabled(
                        true,
                        () -> {
                            calls.incrementAndGet();
                            return Uni.createFrom().item(new PruneResult(7, Instant.EPOCH));
                        },
                        prunedAt -> new PruneResult(0, prunedAt))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(7, result.pruned());
        assertEquals(Instant.EPOCH, result.prunedAt());
        assertEquals(1, calls.get());
    }

    @Test
    void pruneIfEnabledCanUseCountTimestampResultFactoryForDisabledResult() {
        AtomicInteger calls = new AtomicInteger();

        PruneResult result = McpHistoryPruneSchedulerSupport.pruneIfEnabled(
                        false,
                        () -> {
                            calls.incrementAndGet();
                            return Uni.createFrom().item(new PruneResult(99, Instant.EPOCH));
                        },
                        PruneResult::new)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(0, calls.get());
    }

    private record PruneResult(
            int pruned,
            Instant prunedAt) {
    }
}
