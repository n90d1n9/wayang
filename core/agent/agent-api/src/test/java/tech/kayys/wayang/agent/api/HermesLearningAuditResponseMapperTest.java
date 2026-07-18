package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditDirective;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditService;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotion;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceipt;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptLedger;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditResponseMapperTest {

    private final HermesLearningAuditResponseMapper mapper = new HermesLearningAuditResponseMapper();

    @Test
    void inspectsConfiguredLearningAuditPort() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        ledger.record(receipt(
                "skill-a",
                "key-skill-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true));

        Response response = mapper.inspect(
                Optional.of(HermesLearningAuditPort.service(new HermesLearningAuditService(ledger))),
                HermesLearningAuditDirective.skill("skill-a", 5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditResponse body = (HermesLearningAuditResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesLearningAuditResponse::port,
                        HermesLearningAuditResponse::target,
                        HermesLearningAuditResponse::successful,
                        HermesLearningAuditResponse::matchedReceipts,
                        HermesLearningAuditResponse::totalMatchedReceipts,
                        HermesLearningAuditResponse::returnedReceipts,
                        HermesLearningAuditResponse::firstCursor,
                        HermesLearningAuditResponse::lastCursor,
                        HermesLearningAuditResponse::cursorResolved,
                        HermesLearningAuditResponse::persistedReceipts,
                        HermesLearningAuditResponse::latestSkillId,
                        HermesLearningAuditResponse::latestOutcome)
                .containsExactly(
                        "learning-audit",
                        "skill:skill-a",
                        true,
                        1,
                        1,
                        1,
                        "key-skill-a",
                        "key-skill-a",
                        true,
                        1L,
                        "skill-a",
                        HermesLearningPromotionReceipt.OUTCOME_PERSISTED);
        assertThat(body.metadata())
                .containsEntry("matchedReceipts", 1)
                .containsEntry("totalMatchedReceipts", 1)
                .containsEntry("returnedReceipts", 1)
                .containsEntry("cursorResolved", true)
                .containsEntry("retentionState", "unbounded")
                .containsKeys("learningAuditView", "learningAuditSummary", "learningAuditRetentionStatus", "query");
        assertThat(body.query())
                .containsEntry("skillId", "skill-a")
                .containsEntry("limit", 5);
        assertThat(body.learningAuditSummary())
                .containsEntry("latestSkillId", "skill-a")
                .containsEntry("persistedReceipts", 1L);
        assertThat(body.learningAuditRetentionStatus())
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("status", "unbounded")
                .containsEntry("severity", "info")
                .containsEntry("priority", 0)
                .containsEntry("requiresAttention", false)
                .containsKey("recommendedActions");
    }

    @Test
    void returnsNotFoundWhenLearningAuditPortIsMissing() {
        Response response = mapper.inspect(
                Optional.empty(),
                HermesLearningAuditDirective.latest(5));

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT));
    }

    private HermesLearningPromotionReceipt receipt(
            String skillId,
            String key,
            String status,
            String outcome,
            boolean persisted) {
        return new HermesLearningPromotionReceipt(
                "promotion-" + skillId,
                key,
                status,
                outcome,
                skillId,
                persisted,
                "test receipt",
                "in-memory",
                "definitions=in-memory,artifacts=in-memory",
                Map.of("adapterId", "in-memory"));
    }
}
