package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementMaintenanceStepDiagnosticsTest {

    @Test
    void exposesOrderedStepDiagnostics() {
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
                        true,
                        List.of(new SkillArtifactStoreSyncChange(
                                SkillArtifactReference.resource("planner", "prompt", "v1"),
                                SkillArtifactStoreSyncAction.UPDATED,
                                "would update"))),
                new SkillLifecycleStateReconcileResult(
                        List.of("planner"),
                        List.of(),
                        List.of("planner"),
                        List.of(),
                        List.of(),
                        List.of()),
                SkillManagementEventPruneResult.failure(
                        SkillManagementEventPruneOptions.keepLatest(1),
                        "event store unavailable"));

        List<SkillManagementMaintenanceStepDiagnostic> diagnostics = result.stepDiagnostics();

        assertThat(diagnostics)
                .extracting(SkillManagementMaintenanceStepDiagnostic::step)
                .containsExactly(
                        SkillManagementMaintenanceStep.DEFINITION_SYNC,
                        SkillManagementMaintenanceStep.ARTIFACT_SYNC,
                        SkillManagementMaintenanceStep.LIFECYCLE_RECONCILE,
                        SkillManagementMaintenanceStep.EVENT_PRUNE);
        assertThat(diagnostics.get(0).status()).isEqualTo(SkillManagementMaintenanceStepStatus.CONFLICT);
        assertThat(diagnostics.get(0).changed()).isTrue();
        assertThat(diagnostics.get(0).changes()).isEqualTo(1);
        assertThat(diagnostics.get(0).conflicts()).isEqualTo(1);
        assertThat(diagnostics.get(0).consistent()).isFalse();
        assertThat(diagnostics.get(1).status()).isEqualTo(SkillManagementMaintenanceStepStatus.DRY_RUN);
        assertThat(diagnostics.get(1).dryRun()).isTrue();
        assertThat(diagnostics.get(1).changed()).isTrue();
        assertThat(diagnostics.get(2).status()).isEqualTo(SkillManagementMaintenanceStepStatus.INCONSISTENT);
        assertThat(diagnostics.get(2).conflicts()).isEqualTo(1);
        assertThat(diagnostics.get(2).consistent()).isFalse();
        assertThat(diagnostics.get(3).status()).isEqualTo(SkillManagementMaintenanceStepStatus.FAILED);
        assertThat(diagnostics.get(3).failure()).isEqualTo("event store unavailable");
        assertThat(diagnostics.get(3).successful()).isFalse();
    }

    @Test
    void normalizesDiagnosticCountsAndFailure() {
        SkillManagementMaintenanceStepDiagnostic diagnostic = new SkillManagementMaintenanceStepDiagnostic(
                SkillManagementMaintenanceStep.EVENT_PRUNE,
                SkillManagementMaintenanceStepStatus.FAILED,
                false,
                false,
                false,
                false,
                -1,
                -2,
                null);

        assertThat(diagnostic.changes()).isZero();
        assertThat(diagnostic.conflicts()).isZero();
        assertThat(diagnostic.failure()).isBlank();
        assertThat(diagnostic.successful()).isFalse();
    }
}
