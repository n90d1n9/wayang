package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditRetentionStatusTest {

    @Test
    void classifiesBoundedLedgerPressure() {
        HermesLearningAuditRetentionStatus status = HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "file-system",
                "recordCount", 8,
                "retentionPolicy", Map.of(
                        "retentionMode", "max-entries",
                        "maxEntries", 10)));

        assertThat(status)
                .extracting(
                        HermesLearningAuditRetentionStatus::ledgerType,
                        HermesLearningAuditRetentionStatus::bounded,
                        HermesLearningAuditRetentionStatus::recordCount,
                        HermesLearningAuditRetentionStatus::maxEntries,
                        HermesLearningAuditRetentionStatus::remainingEntries,
                        HermesLearningAuditRetentionStatus::utilizationPercent,
                        HermesLearningAuditRetentionStatus::nearCapacity,
                        HermesLearningAuditRetentionStatus::atCapacity,
                        HermesLearningAuditRetentionStatus::status,
                        HermesLearningAuditRetentionStatus::severity,
                        HermesLearningAuditRetentionStatus::priority)
                .containsExactly(
                        "file-system",
                        true,
                        8,
                        10,
                        2,
                        80,
                        true,
                        false,
                        "near-capacity",
                        "warning",
                        2);
        assertThat(status.requiresAttention()).isTrue();
        assertThat(status.attention())
                .containsExactly("Learning-audit receipt ledger is at 80% of retention capacity.");
        assertThat(status.recommendedActions())
                .containsExactly(
                        "monitor-learning-audit-retention",
                        "plan-learning-audit-retention-capacity");
    }

    @Test
    void classifiesCapacityBoundaryAndOverflow() {
        assertThat(HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                        "ledgerType", "database",
                        "recordCount", 10,
                        "maxRecords", 10))
                .status())
                .isEqualTo("at-capacity");

        HermesLearningAuditRetentionStatus overCapacity = HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "database",
                "recordCount", 12,
                "maxRecords", 10));

        assertThat(overCapacity.status()).isEqualTo("over-capacity");
        assertThat(overCapacity.severity()).isEqualTo("critical");
        assertThat(overCapacity.priority()).isEqualTo(3);
        assertThat(overCapacity.overflowEntries()).isEqualTo(2);
        assertThat(overCapacity.remainingEntries()).isZero();
        assertThat(overCapacity.requiresAttention()).isTrue();
        assertThat(overCapacity.recommendedActions())
                .contains(
                        "verify-learning-audit-ledger-pruning",
                        "increase-learning-audit-retention-limit");
    }

    @Test
    void treatsUnboundedLedgersAsObservableButNotPressured() {
        HermesLearningAuditRetentionStatus status = HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "in-memory",
                "recordCount", 3));

        assertThat(status.bounded()).isFalse();
        assertThat(status.status()).isEqualTo("unbounded");
        assertThat(status.toMetadata())
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("bounded", false)
                .containsEntry("recordCount", 3)
                .containsEntry("status", "unbounded")
                .containsEntry("severity", "info")
                .containsEntry("priority", 0)
                .containsEntry("requiresAttention", false)
                .containsKey("ledgerMetadata");
        assertThat(status.attention()).isEmpty();
        assertThat(status.recommendedActions()).isEmpty();
    }

    @Test
    void derivesHybridLimitFromChildLedgers() {
        HermesLearningAuditRetentionStatus status = HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "hybrid",
                "recordCount", 4,
                "primaryLedger", Map.of("ledgerType", "database", "maxRecords", 5),
                "fallbackLedger", Map.of("ledgerType", "file-system", "maxRecords", 5)));

        assertThat(status.bounded()).isTrue();
        assertThat(status.maxEntries()).isEqualTo(5);
        assertThat(status.remainingEntries()).isEqualTo(1);
        assertThat(status.status()).isEqualTo("near-capacity");
    }
}
