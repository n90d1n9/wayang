package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningLifecycleReportTest {

    @Test
    void rendersStableMetadataAndRestoresFromMetadata() {
        HermesLearningLifecycleReport report = HermesLearningLifecycleReport.fromStages(
                        HermesLearningStageReport.completed(
                                HermesLearningStageCatalog.SIGNAL_DETECTION,
                                "signal captured",
                                Map.of("requestId", "req-lifecycle")),
                        HermesLearningStageReport.skipped(
                                HermesLearningStageCatalog.SKILL_DISTILLATION,
                                "below threshold"),
                        HermesLearningStageReport.failed(
                                HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                                "invalid skill",
                                Map.of("skillId", "invalid")))
                .withStage(HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.PROMOTION_RECEIPT,
                        "receipt recorded",
                        Map.of("persisted", false)));

        Map<String, Object> metadata = report.toMetadata();
        HermesLearningLifecycleReport restored = HermesLearningLifecycleReport.fromMetadata(metadata);

        assertThat(report.completedStages())
                .containsExactly("signal-detection", "promotion-receipt");
        assertThat(report.skippedStages()).containsExactly("skill-distillation");
        assertThat(report.failedStages()).containsExactly("candidate-validation");
        assertThat(metadata)
                .containsEntry(HermesLearningMetadataKeys.TERMINAL_STAGE, "promotion-receipt")
                .containsEntry(HermesLearningMetadataKeys.PENDING_STAGES, List.of());
        assertThat(restored.completedStages()).containsExactlyElementsOf(report.completedStages());
        assertThat(restored.skippedStages()).containsExactlyElementsOf(report.skippedStages());
        assertThat(restored.failedStages()).containsExactlyElementsOf(report.failedStages());
    }

    @Test
    void replacesExistingStageWithoutDuplicatingIt() {
        HermesLearningLifecycleReport report = HermesLearningLifecycleReport.fromStages(
                        HermesLearningStageReport.pending(
                                HermesLearningStageCatalog.SKILL_PERSISTENCE,
                                "not attempted"))
                .withStage(HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.SKILL_PERSISTENCE,
                        "persisted",
                        Map.of("adapterId", "database")));

        assertThat(report.stages()).hasSize(1);
        assertThat(report.completedStages()).containsExactly("skill-persistence");
        assertThat(report.pendingStages()).isEmpty();
    }
}
