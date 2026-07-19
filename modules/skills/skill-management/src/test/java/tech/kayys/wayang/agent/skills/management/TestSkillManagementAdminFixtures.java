package tech.kayys.wayang.agent.skills.management;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Shared admin projection inputs for tests that exercise DTO/view mappers.
 */
final class TestSkillManagementAdminFixtures {

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private TestSkillManagementAdminFixtures() {
    }

    static SkillManagementService service() {
        return new SkillManagementService(new TestSkillDefinitionStore());
    }

    static SkillArtifactReference promptReference() {
        return SkillArtifactReference.resource("planner", "prompt", "v1");
    }

    static SkillDefinitionStoreSyncChange copiedDefinitionChange() {
        return new SkillDefinitionStoreSyncChange(
                "planner",
                SkillDefinitionStoreSyncAction.COPIED,
                "created");
    }

    static SkillDefinitionStoreSyncResult definitionSyncResult() {
        return new SkillDefinitionStoreSyncResult(
                false,
                List.of(copiedDefinitionChange()));
    }

    static SkillArtifactStoreSyncChange updatedPromptArtifactChange() {
        return new SkillArtifactStoreSyncChange(
                promptReference(),
                SkillArtifactStoreSyncAction.UPDATED,
                "updated bytes");
    }

    static SkillArtifactStoreSyncResult artifactSyncResult() {
        return new SkillArtifactStoreSyncResult(
                false,
                List.of(updatedPromptArtifactChange()));
    }

    static SkillLifecycleStateReconcileResult reconciliation() {
        return new SkillLifecycleStateReconcileResult(
                List.of("planner"),
                List.of(),
                List.of("planner"),
                List.of(),
                List.of("planner"),
                List.of());
    }

    static SkillManagementMaintenanceResult maintenanceResult() {
        return maintenanceResult(definitionSyncResult(), artifactSyncResult(), reconciliation());
    }

    static SkillManagementMaintenanceResult maintenanceResult(
            SkillDefinitionStoreSyncResult definitionSync,
            SkillArtifactStoreSyncResult artifactSync,
            SkillLifecycleStateReconcileResult reconciliation) {
        return new SkillManagementMaintenanceResult(
                definitionSync,
                artifactSync,
                reconciliation,
                SkillManagementEventPruneResult.skippedResult());
    }

    static SkillManagementDeploymentResult deploymentResult(SkillManagementMaintenanceResult maintenance) {
        return new SkillManagementDeploymentResult(
                service(),
                SkillManagementDeploymentConfig.defaults(),
                maintenance);
    }

    static SkillManagementInspection inspection(SkillLifecycleStateReconcileResult reconciliation) {
        SkillManagementEvent event = event(
                0,
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of());
        return new SkillManagementInspection(
                SkillDefinitionStoreInspection.ready("definitions", "memory", List.of("planner"), List.of()),
                SkillLifecycleStateStoreInspection.ready(
                        "lifecycle",
                        "memory",
                        List.of("planner"),
                        Map.of(SkillLifecycleStatus.ACTIVE, 1),
                        List.of()),
                SkillManagementEventStoreInspection.ready(
                        "events",
                        "memory",
                        eventPage(1, event),
                        List.of()),
                SkillArtifactStoreInspection.ready(
                        "artifacts",
                        "memory",
                        List.of(promptReference()),
                        List.of(),
                        SkillStoreCapabilities.definitionStore()),
                reconciliation,
                "");
    }

    static SkillManagementEvent event(
            long secondsAfterBase,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success,
            Map<String, String> attributes) {
        return new SkillManagementEvent(
                BASE_TIME.plusSeconds(secondsAfterBase),
                operation,
                skillId,
                success,
                attributes == null ? Map.of() : attributes);
    }

    static SkillManagementEventPage eventPage(int matchedEvents, SkillManagementEvent... events) {
        return new SkillManagementEventPage(List.of(events), matchedEvents);
    }
}
