package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningMetadataKeysTest {

    @Test
    void exposesStableLearningResultKeys() {
        assertThat(HermesLearningMetadataKeys.RESULT).isEqualTo("learningResult");
        assertThat(HermesLearningMetadataKeys.LIFECYCLE).isEqualTo("learningLifecycle");
        assertThat(HermesLearningMetadataKeys.PLANNING_LIFECYCLE).isEqualTo("planningLifecycle");
        assertThat(HermesLearningMetadataKeys.PROMOTION).isEqualTo("promotion");
        assertThat(HermesLearningMetadataKeys.PROMOTION_RECEIPT).isEqualTo("promotionReceipt");
        assertThat(HermesLearningMetadataKeys.PROMOTION_RECEIPT_LEDGER)
                .isEqualTo("promotionReceiptLedger");
        assertThat(HermesLearningMetadataKeys.SKILL_INDEXING_RECEIPT)
                .isEqualTo("skillIndexingReceipt");
        assertThat(HermesLearningMetadataKeys.RESULT_FIELDS)
                .containsExactly(
                        "promotion",
                        "promotionReceipt",
                        "promotionReceiptLedger",
                        "skillIndexingReceipt",
                        "learningLifecycle");
    }

    @Test
    void exposesStableLearningLifecycleFields() {
        assertThat(HermesLearningMetadataKeys.LIFECYCLE_FIELDS)
                .containsExactly(
                        "terminalStage",
                        "completedStages",
                        "skippedStages",
                        "failedStages",
                        "pendingStages",
                        "stages");
        assertThat(HermesLearningMetadataKeys.STAGE_FIELDS)
                .containsExactly(
                        "stage",
                        "status",
                        "reason",
                        "metadata");
    }

    @Test
    void learningKeyGroupsHaveNoDuplicates() {
        assertThat(new LinkedHashSet<>(HermesLearningMetadataKeys.RESULT_FIELDS))
                .hasSameSizeAs(HermesLearningMetadataKeys.RESULT_FIELDS);
        assertThat(new LinkedHashSet<>(HermesLearningMetadataKeys.LIFECYCLE_FIELDS))
                .hasSameSizeAs(HermesLearningMetadataKeys.LIFECYCLE_FIELDS);
        assertThat(new LinkedHashSet<>(HermesLearningMetadataKeys.STAGE_FIELDS))
                .hasSameSizeAs(HermesLearningMetadataKeys.STAGE_FIELDS);
    }
}
