package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesMetadataKeys;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Verifies typed status projections derived from Hermes diagnostics metadata.
 */
class HermesStatusResponseTest {

    @Test
    void projectsNestedRetentionObservationFromDiagnosticsMetadata() {
        HermesStatusResponse response = HermesStatusResponse.from(new HermesPortResponse(
                "runtime-diagnostics",
                "inspect",
                "runtime-diagnostics:summary",
                true,
                true,
                true,
                "inspected",
                "runtime diagnostics inspected",
                Map.of(
                        "ready", true,
                        "diagnostics", Map.of(
                                HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                                Map.of("outcome", "emitted", "reason", "capacity-warning")))),
                true,
                true,
                true,
                true);

        assertThat(response.ready()).isTrue();
        assertThat(response.attentionSummary().totalItems()).isZero();
        assertThat(response.learningAuditRetentionObservation())
                .containsEntry("outcome", "emitted")
                .containsEntry("reason", "capacity-warning");
    }

    @Test
    void leavesRetentionObservationEmptyWhenUnavailable() {
        HermesStatusResponse response = HermesStatusResponse.unavailable(
                true,
                true,
                true,
                "diagnostics offline");

        assertThat(response.status()).isEqualTo(HermesStatusResponse.STATUS_UNAVAILABLE);
        assertThat(response.attentionItems())
                .extracting(HermesOperationalAttention::message)
                .containsExactly("diagnostics offline");
        assertThat(response.attentionItems())
                .extracting(HermesOperationalAttention::source, HermesOperationalAttention::severity)
                .containsExactly(tuple("hermes-status", "warning"));
        assertThat(response.attentionSummary())
                .extracting(
                        HermesOperationalAttentionSummaryResponse::totalItems,
                        HermesOperationalAttentionSummaryResponse::highestPriority,
                        HermesOperationalAttentionSummaryResponse::requiresAttention)
                .containsExactly(1, 2, true);
        assertThat(response.attentionSummary().sourceCounts())
                .containsEntry("hermes-status", 1L);
        assertThat(response.learningAuditRetentionObservation()).isEmpty();
    }
}
