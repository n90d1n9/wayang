package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

class SkillManagementAdminDeploymentHistoryEntryAttributesTest {

    @Test
    void decodesDeploymentEntryFieldsFromEventAttributes() {
        SkillManagementEvent event = TestSkillManagementAdminFixtures.event(
                5,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                false,
                Map.ofEntries(
                        Map.entry(OPERATION_ID, " deploy-1 "),
                        Map.entry(PARENT_OPERATION_ID, " release-1 "),
                        Map.entry(DRY_RUN, "true"),
                        Map.entry(CHANGED, "true"),
                        Map.entry(CONSISTENT, "false"),
                        Map.entry(DEFINITION_CHANGES, "3"),
                        Map.entry(ARTIFACT_CHANGES, "invalid"),
                        Map.entry(ARTIFACT_CONFLICTS, "2"),
                        Map.entry(LIFECYCLE_CREATED, "1"),
                        Map.entry(LIFECYCLE_REMOVED, "-1"),
                        Map.entry(EVENT_PRUNE_ENABLED, "true"),
                        Map.entry(EVENT_PRUNE_SKIPPED, "false"),
                        Map.entry(EVENT_PRUNE_CHANGED, "true"),
                        Map.entry(EVENT_PRUNED, "4"),
                        Map.entry(PREFLIGHT_READY, "false"),
                        Map.entry(PREFLIGHT_DEPLOYABLE, "false"),
                        Map.entry(PREFLIGHT_ERRORS, "2"),
                        Map.entry(PREFLIGHT_CONFIGURATION_ERRORS, "1"),
                        Map.entry(PREFLIGHT_TARGET_STORE_ERRORS, "0"),
                        Map.entry(PREFLIGHT_SOURCE_STORE_ERRORS, "1"),
                        Map.entry(PREFLIGHT_CAPABILITY_ERRORS, "0"),
                        Map.entry(PREFLIGHT_MESSAGE, "source store unavailable"),
                        Map.entry(ERROR_TYPE, "SkillManagementDeploymentPreflightException"),
                        Map.entry(ERROR, "preflight failed"),
                        Map.entry("maintenanceStep.definition-sync.status", "CHANGED"),
                        Map.entry("maintenanceStep.definition-sync.changed", "true"),
                        Map.entry("maintenanceStep.definition-sync.consistent", "true"),
                        Map.entry("maintenanceStep.definition-sync.changes", "3")));

        SkillManagementAdminDeploymentHistoryEntry entry =
                SkillManagementAdminDeploymentHistoryEntryAttributes.from(event);

        assertThat(entry.occurredAt()).isEqualTo("2026-01-01T00:00:05Z");
        assertThat(entry.operationId()).isEqualTo("deploy-1");
        assertThat(entry.parentOperationId()).isEqualTo("release-1");
        assertThat(entry.success()).isFalse();
        assertThat(entry.dryRun()).isTrue();
        assertThat(entry.changed()).isTrue();
        assertThat(entry.consistent()).isFalse();
        assertThat(entry.definitionChanges()).isEqualTo(3);
        assertThat(entry.artifactChanges()).isZero();
        assertThat(entry.artifactConflicts()).isEqualTo(2);
        assertThat(entry.lifecycleCreated()).isEqualTo(1);
        assertThat(entry.lifecycleRemoved()).isZero();
        assertThat(entry.eventPruneEnabled()).isTrue();
        assertThat(entry.eventPruneSkipped()).isFalse();
        assertThat(entry.eventPruneChanged()).isTrue();
        assertThat(entry.eventPruned()).isEqualTo(4);
        assertThat(entry.preflightAvailable()).isTrue();
        assertThat(entry.preflightReady()).isFalse();
        assertThat(entry.preflightDeployable()).isFalse();
        assertThat(entry.preflightErrors()).isEqualTo(2);
        assertThat(entry.preflightConfigurationErrors()).isEqualTo(1);
        assertThat(entry.preflightTargetStoreErrors()).isZero();
        assertThat(entry.preflightSourceStoreErrors()).isEqualTo(1);
        assertThat(entry.preflightCapabilityErrors()).isZero();
        assertThat(entry.preflightMessage()).isEqualTo("source store unavailable");
        assertThat(entry.errorType()).isEqualTo("SkillManagementDeploymentPreflightException");
        assertThat(entry.error()).isEqualTo("preflight failed");
        assertThat(entry.steps())
                .extracting(SkillManagementAdminMaintenanceStepReport::step)
                .containsExactly("definition-sync");
        assertThat(entry.steps().get(0).changes()).isEqualTo(3);
    }

    @Test
    void emptyAttributesProduceStableEntryDefaults() {
        SkillManagementAdminDeploymentHistoryEntryAttributes attributes =
                SkillManagementAdminDeploymentHistoryEntryAttributes.from(
                        (SkillManagementEventAttributeReader) null);

        SkillManagementAdminDeploymentHistoryEntry entry = attributes.entry(null, true);

        assertThat(entry.occurredAt()).isEmpty();
        assertThat(entry.operationId()).isEmpty();
        assertThat(entry.parentOperationId()).isEmpty();
        assertThat(entry.success()).isTrue();
        assertThat(entry.dryRun()).isFalse();
        assertThat(entry.changed()).isFalse();
        assertThat(entry.consistent()).isFalse();
        assertThat(entry.definitionChanges()).isZero();
        assertThat(entry.artifactChanges()).isZero();
        assertThat(entry.artifactConflicts()).isZero();
        assertThat(entry.lifecycleCreated()).isZero();
        assertThat(entry.lifecycleRemoved()).isZero();
        assertThat(entry.eventPruneEnabled()).isFalse();
        assertThat(entry.eventPruneSkipped()).isFalse();
        assertThat(entry.eventPruneChanged()).isFalse();
        assertThat(entry.eventPruned()).isZero();
        assertThat(entry.preflightAvailable()).isFalse();
        assertThat(entry.preflightErrors()).isZero();
        assertThat(entry.preflightMessage()).isEmpty();
        assertThat(entry.errorType()).isEmpty();
        assertThat(entry.error()).isEmpty();
        assertThat(entry.steps()).isEmpty();
    }
}
