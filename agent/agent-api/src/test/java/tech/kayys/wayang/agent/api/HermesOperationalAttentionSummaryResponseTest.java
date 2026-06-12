package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOperationalAttentionSummaryResponseTest {

    @Test
    void summarizesOperationalAttentionPressure() {
        HermesOperationalAttentionSummaryResponse response = HermesOperationalAttentionSummaryResponse.from(List.of(
                new HermesOperationalAttention(
                        "learning-audit-retention",
                        "warning",
                        2,
                        "capacity warning",
                        "monitor-learning-audit-retention",
                        true,
                        Map.of()),
                new HermesOperationalAttention(
                        "runtime-diagnostics",
                        "critical",
                        3,
                        "journal missing",
                        "",
                        false,
                        Map.of())));

        assertThat(response)
                .extracting(
                        HermesOperationalAttentionSummaryResponse::totalItems,
                        HermesOperationalAttentionSummaryResponse::retryableItems,
                        HermesOperationalAttentionSummaryResponse::highestPriority,
                        HermesOperationalAttentionSummaryResponse::requiresAttention)
                .containsExactly(2, 1, 3, true);
        assertThat(response.actions()).containsExactly("monitor-learning-audit-retention");
        assertThat(response.sourceCounts())
                .containsEntry("learning-audit-retention", 1L)
                .containsEntry("runtime-diagnostics", 1L);
        assertThat(response.severityCounts())
                .containsEntry("warning", 1L)
                .containsEntry("critical", 1L);
    }

    @Test
    void handlesEmptyAttentionItems() {
        HermesOperationalAttentionSummaryResponse response = HermesOperationalAttentionSummaryResponse.empty();

        assertThat(response.totalItems()).isZero();
        assertThat(response.retryableItems()).isZero();
        assertThat(response.requiresAttention()).isFalse();
        assertThat(response.actions()).isEmpty();
        assertThat(response.sourceCounts()).isEmpty();
        assertThat(response.severityCounts()).isEmpty();
    }
}
