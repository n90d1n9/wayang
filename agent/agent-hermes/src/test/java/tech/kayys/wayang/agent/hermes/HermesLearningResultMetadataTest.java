package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningResultMetadataTest {

    @Test
    void exposesTypedLearningResultMetadataViews() {
        HermesLearningLifecycleReport lifecycle = HermesLearningLifecycleReport.fromStages(
                HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.PROMOTION_RECEIPT,
                        "receipt recorded",
                        Map.of("persisted", true)));
        HermesLearningResultMetadata metadata = new HermesLearningResultMetadata(Map.of(
                HermesLearningMetadataKeys.PROMOTION, Map.of("status", "approved"),
                HermesLearningMetadataKeys.PROMOTION_RECEIPT, Map.of("outcome", "persisted"),
                HermesLearningMetadataKeys.PROMOTION_RECEIPT_LEDGER, Map.of("ledgerType", "in-memory"),
                HermesLearningMetadataKeys.SKILL_INDEXING_RECEIPT, Map.of("indexed", true),
                HermesLearningMetadataKeys.LIFECYCLE, lifecycle.toMetadata()));

        assertThat(metadata.emptyMetadata()).isFalse();
        assertThat(metadata.promotion()).containsEntry("status", "approved");
        assertThat(metadata.promotionReceipt()).containsEntry("outcome", "persisted");
        assertThat(metadata.promotionReceiptLedger()).containsEntry("ledgerType", "in-memory");
        assertThat(metadata.skillIndexingReceipt()).containsEntry("indexed", true);
        assertThat(metadata.lifecycleReport().completedStages())
                .containsExactly(HermesLearningStageCatalog.PROMOTION_RECEIPT);
    }

    @Test
    void missingNestedMetadataReturnsEmptyViews() {
        HermesLearningResultMetadata metadata = HermesLearningResultMetadata.empty();

        assertThat(metadata.emptyMetadata()).isTrue();
        assertThat(metadata.promotion()).isEmpty();
        assertThat(metadata.promotionReceipt()).isEmpty();
        assertThat(metadata.promotionReceiptLedger()).isEmpty();
        assertThat(metadata.skillIndexingReceipt()).isEmpty();
        assertThat(metadata.lifecycle()).isEmpty();
        assertThat(metadata.lifecycleReport().emptyReport()).isTrue();
    }
}
