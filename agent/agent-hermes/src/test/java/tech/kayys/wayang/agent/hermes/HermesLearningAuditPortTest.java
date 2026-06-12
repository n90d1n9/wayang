package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditPortTest {

    @Test
    void directiveBuildsStableTargetsAndMetadata() {
        HermesLearningAuditDirective skillDirective = HermesLearningAuditDirective.skill("skill-a", 5);
        HermesLearningAuditDirective persistedDirective = HermesLearningAuditDirective.persisted(2);
        HermesLearningAuditDirective inactive = HermesLearningAuditDirective.none();

        assertThat(skillDirective.target()).isEqualTo("skill:skill-a");
        assertThat(skillDirective.toMetadata())
                .containsEntry("active", true)
                .containsEntry("operation", "inspect")
                .containsEntry("target", "skill:skill-a")
                .containsKey("query");
        assertThat(persistedDirective.target()).isEqualTo("persisted");
        assertThat(inactive.active()).isFalse();
        assertThat(inactive.operation()).isEqualTo("none");
    }

    @Test
    void serviceBackedPortReturnsLearningAuditViewMetadata() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt receipt = new HermesLearningPromotionReceipt(
                "promotion-skill-a",
                "key-skill-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                "skill-a",
                true,
                "persisted",
                "database",
                "definitions=database,artifacts=database",
                Map.of("adapterId", "database"));
        ledger.record(receipt);
        HermesLearningAuditPort port = HermesLearningAuditPort.service(
                new HermesLearningAuditService(ledger));

        HermesPortDispatchResult result = port.inspect(HermesLearningAuditDirective.skill("skill-a", 10));

        assertThat(result.port()).isEqualTo("learning-audit");
        assertThat(result.status()).isEqualTo("inspected");
        assertThat(result.successful()).isTrue();
        assertThat(result.metadata())
                .containsEntry("matchedReceipts", 1)
                .containsEntry("returnedReceipts", 1)
                .containsEntry("truncated", false)
                .containsEntry("persistedReceipts", 1L)
                .containsEntry("latestSkillId", "skill-a")
                .containsEntry("latestOutcome", HermesLearningPromotionReceipt.OUTCOME_PERSISTED)
                .containsEntry("retentionState", "unbounded")
                .containsEntry("retentionSeverity", "info")
                .containsEntry("retentionPriority", 0)
                .containsEntry("retentionRequiresAttention", false)
                .containsKeys("learningAuditView", "learningAuditSummary", "learningAuditRetentionStatus");
        assertThat(metadataMap(result.metadata(), "learningAuditView"))
                .containsEntry("matchedReceipts", 1)
                .containsEntry("returnedReceipts", 1)
                .containsEntry("latestSkillId", "skill-a");
        assertThat(metadataMap(result.metadata(), "learningAuditSummary"))
                .containsEntry("matchedReceipts", 1)
                .containsEntry("latestSkillId", "skill-a");
        assertThat(metadataMap(result.metadata(), "learningAuditRetentionStatus"))
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("status", "unbounded")
                .containsEntry("severity", "info")
                .containsEntry("priority", 0)
                .containsEntry("requiresAttention", false);
        assertThat(metadataMap(port.descriptor().metadata(), "retentionStatus"))
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("status", "unbounded");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
