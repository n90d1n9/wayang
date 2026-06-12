package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementMaintenanceSummaryTest {

    @Test
    void summarizesMaintenanceStepResults() {
        SkillManagementMaintenanceResult result = new SkillManagementMaintenanceResult(
                new SkillDefinitionStoreSyncResult(
                        false,
                        List.of(
                                new SkillDefinitionStoreSyncChange(
                                        "planner",
                                        SkillDefinitionStoreSyncAction.COPIED,
                                        "copied"),
                                new SkillDefinitionStoreSyncChange(
                                        "legacy",
                                        SkillDefinitionStoreSyncAction.CONFLICT,
                                        "conflict"))),
                new SkillArtifactStoreSyncResult(
                        false,
                        List.of(
                                new SkillArtifactStoreSyncChange(
                                        SkillArtifactReference.resource("planner", "prompt", "v1"),
                                        SkillArtifactStoreSyncAction.UPDATED,
                                        "updated"),
                                new SkillArtifactStoreSyncChange(
                                        SkillArtifactReference.resource("legacy", "prompt", "v1"),
                                        SkillArtifactStoreSyncAction.CONFLICT,
                                        "conflict"))),
                new SkillLifecycleStateReconcileResult(
                        List.of("planner"),
                        List.of("legacy"),
                        List.of("planner"),
                        List.of("legacy"),
                        List.of("planner"),
                        List.of("legacy")),
                SkillManagementEventPruneResult.success(
                        SkillManagementEventPruneOptions.keepLatest(1),
                        3,
                        List.of("event-1")));

        SkillManagementMaintenanceSummary summary = result.summary();

        assertThat(summary.dryRun()).isFalse();
        assertThat(summary.changed()).isTrue();
        assertThat(summary.consistent()).isFalse();
        assertThat(summary.definitionChanged()).isTrue();
        assertThat(summary.definitionChanges()).isEqualTo(1);
        assertThat(summary.definitionConflicts()).isEqualTo(1);
        assertThat(summary.artifactChanged()).isTrue();
        assertThat(summary.artifactChanges()).isEqualTo(1);
        assertThat(summary.artifactConflicts()).isEqualTo(1);
        assertThat(summary.lifecycleChanged()).isTrue();
        assertThat(summary.lifecycleCreated()).isEqualTo(1);
        assertThat(summary.lifecycleRemoved()).isEqualTo(1);
        assertThat(summary.lifecycleConsistent()).isTrue();
        assertThat(summary.eventPruneSkipped()).isFalse();
        assertThat(summary.eventPruneChanged()).isTrue();
        assertThat(summary.eventPruned()).isEqualTo(1);
        assertThat(summary.eventPruneSuccess()).isTrue();
        assertThat(result.dryRun()).isEqualTo(summary.dryRun());
        assertThat(result.changed()).isEqualTo(summary.changed());
        assertThat(result.consistent()).isEqualTo(summary.consistent());
    }

    @Test
    void normalizesNegativeConstructorCounts() {
        SkillManagementMaintenanceSummary summary = new SkillManagementMaintenanceSummary(
                true,
                true,
                true,
                true,
                -1,
                -2,
                true,
                -3,
                -4,
                true,
                -5,
                -6,
                true,
                false,
                true,
                -7,
                true);

        assertThat(summary.definitionChanges()).isZero();
        assertThat(summary.definitionConflicts()).isZero();
        assertThat(summary.artifactChanges()).isZero();
        assertThat(summary.artifactConflicts()).isZero();
        assertThat(summary.lifecycleCreated()).isZero();
        assertThat(summary.lifecycleRemoved()).isZero();
        assertThat(summary.eventPruned()).isZero();
    }
}
