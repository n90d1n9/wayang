package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesMetadataKeys;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HermesDiagnosticsResponseTest {

    @Test
    void projectsDiagnosticsMetadataToTypedFields() {
        HermesDiagnosticsResponse response = HermesDiagnosticsResponse.from(new HermesPortResponse(
                "runtime-diagnostics",
                "inspect",
                "runtime-diagnostics:capabilities",
                true,
                true,
                true,
                "inspected",
                "runtime diagnostics inspected",
                Map.of(
                        "view", "capabilities",
                        "ready", true,
                        "runtimePortsReady", "true",
                        "skillPersistenceReady", false,
                        "learningAuditConfigured", true,
                        "learningAuditReady", true,
                        "attention", List.of("needs journal"),
                        HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                        Map.of("outcome", "suppressed", "reason", "duplicate-state"),
                        "diagnostics", Map.of("supportsSkillLearning", true))));

        assertThat(response)
                .extracting(
                        HermesDiagnosticsResponse::port,
                        HermesDiagnosticsResponse::target,
                        HermesDiagnosticsResponse::view,
                        HermesDiagnosticsResponse::ready,
                        HermesDiagnosticsResponse::runtimePortsReady,
                        HermesDiagnosticsResponse::skillPersistenceReady,
                        HermesDiagnosticsResponse::learningAuditConfigured,
                        HermesDiagnosticsResponse::learningAuditReady)
                .containsExactly(
                        "runtime-diagnostics",
                        "runtime-diagnostics:capabilities",
                        "capabilities",
                        true,
                        true,
                        false,
                        true,
                        true);
        assertThat(response.attention()).containsExactly("needs journal");
        assertThat(response.attentionItems())
                .extracting(HermesOperationalAttention::message)
                .containsExactly("needs journal");
        assertThat(response.attentionItems())
                .extracting(HermesOperationalAttention::source, HermesOperationalAttention::severity)
                .containsExactly(tuple("runtime-diagnostics", "warning"));
        assertThat(response.attentionSummary())
                .extracting(
                        HermesOperationalAttentionSummaryResponse::totalItems,
                        HermesOperationalAttentionSummaryResponse::highestPriority,
                        HermesOperationalAttentionSummaryResponse::requiresAttention)
                .containsExactly(1, 2, true);
        assertThat(response.attentionSummary().sourceCounts())
                .containsEntry("runtime-diagnostics", 1L);
        assertThat(response.diagnostics()).containsEntry("supportsSkillLearning", true);
        assertThat(response.learningAuditRetentionObservation())
                .containsEntry("outcome", "suppressed")
                .containsEntry("reason", "duplicate-state");
        assertThat(response.metadata()).containsEntry("view", "capabilities");
    }

    @Test
    void fallsBackToDiagnosticsRetentionObservation() {
        HermesDiagnosticsResponse response = HermesDiagnosticsResponse.from(new HermesPortResponse(
                "runtime-diagnostics",
                "inspect",
                "runtime-diagnostics:summary",
                true,
                true,
                true,
                "inspected",
                "runtime diagnostics inspected",
                Map.of(
                        "diagnostics", Map.of(
                                HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                                Map.of("outcome", "emitted", "reason", "capacity-warning")))));

        assertThat(response.learningAuditRetentionObservation())
                .containsEntry("outcome", "emitted")
                .containsEntry("reason", "capacity-warning");
    }

    @Test
    void handlesMissingMetadata() {
        HermesDiagnosticsResponse response = HermesDiagnosticsResponse.from(null);

        assertThat(response.port()).isEqualTo("unknown");
        assertThat(response.view()).isEmpty();
        assertThat(response.ready()).isFalse();
        assertThat(response.attention()).isEmpty();
        assertThat(response.attentionItems()).isEmpty();
        assertThat(response.attentionSummary().totalItems()).isZero();
        assertThat(response.diagnostics()).isEmpty();
        assertThat(response.learningAuditRetentionObservation()).isEmpty();
    }
}
