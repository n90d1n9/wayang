package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class McpHistoryMutationResultSupportTest {

    @Test
    void timestampedMapsCountAndTimestamp() {
        MutationResult result = McpHistoryMutationResultSupport.timestamped(
                        Uni.createFrom().item(7),
                        MutationResult::new)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(7, result.count());
        assertNotNull(result.at());
    }

    @Test
    void timestampedTreatsNullCountAsZero() {
        MutationResult result = McpHistoryMutationResultSupport.timestamped(
                        Uni.createFrom().nullItem(),
                        MutationResult::new)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.count());
        assertNotNull(result.at());
    }

    @Test
    void timestampedCanRunSupplierMutation() {
        MutationResult result = McpHistoryMutationResultSupport.timestamped(
                        () -> Uni.createFrom().item(3),
                        MutationResult::new)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(3, result.count());
        assertNotNull(result.at());
    }

    @Test
    void timestampedCanRunScopedMutation() {
        MutationResult result = McpHistoryMutationResultSupport.timestamped(
                        "scope-1",
                        scopeId -> Uni.createFrom().item(scopeId.length()),
                        MutationResult::new)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(7, result.count());
        assertNotNull(result.at());
    }

    @Test
    void timestampedCanRunFilteredScopedMutation() {
        MutationResult result = McpHistoryMutationResultSupport.timestamped(
                        "scope-1",
                        "filter",
                        (scopeId, filter) -> Uni.createFrom().item(scopeId.length() + filter.length()),
                        MutationResult::new)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(13, result.count());
        assertNotNull(result.at());
    }

    @Test
    void scopedMutationUsesUnfilteredPathWhenFiltersAreMissing() {
        int count = McpHistoryMutationResultSupport.scopedMutation(
                        "scope-1",
                        null,
                        ignored -> Uni.createFrom().item(4),
                        (scopeId, filter) -> Uni.createFrom().item(9))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(4, count);
    }

    @Test
    void scopedMutationUsesFilteredPathWhenFiltersExist() {
        int count = McpHistoryMutationResultSupport.scopedMutation(
                        "scope-1",
                        "filter",
                        ignored -> Uni.createFrom().item(4),
                        (scopeId, filter) -> Uni.createFrom().item(9))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(9, count);
    }

    private record MutationResult(
            int count,
            Instant at) {
    }
}
