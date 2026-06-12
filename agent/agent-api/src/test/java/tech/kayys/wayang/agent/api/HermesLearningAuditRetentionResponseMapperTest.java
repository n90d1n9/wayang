package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceipt;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptLedger;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptPage;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptQuery;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HermesLearningAuditRetentionResponseMapperTest {

    private final HermesLearningAuditRetentionResponseMapper mapper =
            new HermesLearningAuditRetentionResponseMapper();

    @Test
    void mapsConfiguredLearningAuditPortRetentionDescriptor() {
        Response response = mapper.inspect(Optional.of(HermesLearningAuditPort.service(retentionLedger(4, 5))));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditRetentionResponse body =
                (HermesLearningAuditRetentionResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesLearningAuditRetentionResponse::port,
                        HermesLearningAuditRetentionResponse::configured,
                        HermesLearningAuditRetentionResponse::ready,
                        HermesLearningAuditRetentionResponse::ledgerType,
                        HermesLearningAuditRetentionResponse::bounded,
                        HermesLearningAuditRetentionResponse::recordCount,
                        HermesLearningAuditRetentionResponse::maxEntries,
                        HermesLearningAuditRetentionResponse::remainingEntries,
                        HermesLearningAuditRetentionResponse::utilizationPercent,
                        HermesLearningAuditRetentionResponse::retentionStatus,
                        HermesLearningAuditRetentionResponse::retentionSeverity,
                        HermesLearningAuditRetentionResponse::retentionRequiresAttention)
                .containsExactly(
                        "learning-audit",
                        true,
                        true,
                        "file-system",
                        true,
                        4,
                        5,
                        1,
                        80,
                        "near-capacity",
                        "warning",
                        true);
        assertThat(body.retentionAttention())
                .containsExactly("Learning-audit receipt ledger is at 80% of retention capacity.");
        assertThat(body.retentionAttentionItems())
                .extracting(
                        HermesOperationalAttention::source,
                        HermesOperationalAttention::severity,
                        HermesOperationalAttention::priority,
                        HermesOperationalAttention::message)
                .containsExactly(tuple(
                        "learning-audit-retention",
                        "warning",
                        2,
                        "Learning-audit receipt ledger is at 80% of retention capacity."));
        assertThat(body.retentionRecommendedActions())
                .containsExactly("monitor-learning-audit-retention", "plan-learning-audit-retention-capacity");
        assertThat(body.retentionRecommendedActionItems())
                .extracting(
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::severity,
                        HermesOperationalAction::priority,
                        HermesOperationalAction::riskLevel,
                        HermesOperationalAction::safe,
                        HermesOperationalAction::dryRunSupported)
                .containsExactly(
                        tuple(
                                "monitor-learning-audit-retention",
                                "warning",
                                2,
                                "low",
                                true,
                                false),
                        tuple(
                                "plan-learning-audit-retention-capacity",
                                "warning",
                                2,
                                "low",
                                true,
                                false));
        assertThat(body.learningAuditRetentionStatus())
                .containsEntry("status", "near-capacity")
                .containsEntry("requiresAttention", true);
    }

    @Test
    void mapsNoopLearningAuditPortToUnavailableRetentionResponse() {
        Response response = mapper.inspect(Optional.of(HermesLearningAuditPort.noop()));

        assertThat(response.getStatus()).isEqualTo(503);
        HermesLearningAuditRetentionResponse body =
                (HermesLearningAuditRetentionResponse) response.getEntity();
        assertThat(body.configured()).isFalse();
        assertThat(body.noop()).isTrue();
        assertThat(body.retentionStatus()).isEmpty();
        assertThat(body.retentionAttentionItems()).isEmpty();
        assertThat(body.retentionRecommendedActionItems()).isEmpty();
        assertThat(body.learningAuditRetentionStatus()).isEmpty();
    }

    @Test
    void returnsNotFoundWhenLearningAuditPortIsMissing() {
        Response response = mapper.inspect(Optional.empty());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT));
    }

    private HermesLearningPromotionReceiptLedger retentionLedger(int recordCount, int maxEntries) {
        return new HermesLearningPromotionReceiptLedger() {
            @Override
            public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
                return Optional.empty();
            }

            @Override
            public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
                return receipt;
            }

            @Override
            public HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
                return HermesLearningPromotionReceiptPage.empty(query);
            }

            @Override
            public int recordCount() {
                return recordCount;
            }

            @Override
            public Map<String, Object> toMetadata() {
                return Map.of(
                        "ledgerType", "file-system",
                        "recordCount", recordCount,
                        "retentionPolicy", Map.of(
                                "retentionMode", "max-entries",
                                "maxEntries", maxEntries));
            }
        };
    }
}
