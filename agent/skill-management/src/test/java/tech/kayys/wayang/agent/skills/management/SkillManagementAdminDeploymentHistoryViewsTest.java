package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDeploymentHistoryViewsTest {

    @Test
    void mapsOnlyDeploymentEventsFromMixedEventPage() {
        SkillManagementEvent deployment = TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of("operationId", " deploy-1 "));
        SkillManagementEvent maintenance = TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of("operationId", "maintenance-1"));
        SkillManagementEventPage page = TestSkillManagementAdminFixtures.eventPage(4, deployment, maintenance);

        SkillManagementAdminDeploymentHistoryPage view =
                SkillManagementAdminDeploymentHistoryViews.deploymentHistory(page);

        assertThat(view.matchedDeployments()).isEqualTo(1);
        assertThat(view.returnedDeployments()).isEqualTo(1);
        assertThat(view.truncated()).isFalse();
        assertThat(view.successfulDeployments()).isEqualTo(1);
        assertThat(view.deployments()).extracting(SkillManagementAdminDeploymentHistoryEntry::operationId)
                .containsExactly("deploy-1");
    }

    @Test
    void mapsDeploymentHistoryToStableAdminProjection() {
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        eventSink.record(TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of()));
        eventSink.record(TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.ofEntries(
                        Map.entry("operationId", "deploy-1"),
                        Map.entry("dryRun", "false"),
                        Map.entry("changed", "true"),
                        Map.entry("consistent", "true"),
                        Map.entry("definitionChanges", "2"),
                        Map.entry("artifactChanges", "1"),
                        Map.entry("artifactConflicts", "0"),
                        Map.entry("lifecycleCreated", "1"),
                        Map.entry("lifecycleRemoved", "0"),
                        Map.entry("eventPruneEnabled", "true"),
                        Map.entry("eventPruneSkipped", "false"),
                        Map.entry("eventPruneChanged", "true"),
                        Map.entry("eventPruned", "3"),
                        Map.entry("maintenanceStep.definition-sync.status", "CHANGED"),
                        Map.entry("maintenanceStep.definition-sync.changed", "true"),
                        Map.entry("maintenanceStep.definition-sync.consistent", "true"),
                        Map.entry("maintenanceStep.definition-sync.changes", "2"),
                        Map.entry("maintenanceStep.artifact-sync.status", "CHANGED"),
                        Map.entry("maintenanceStep.artifact-sync.changed", "true"),
                        Map.entry("maintenanceStep.artifact-sync.consistent", "true"),
                        Map.entry("maintenanceStep.artifact-sync.changes", "1"),
                        Map.entry("maintenanceStep.lifecycle-reconcile.status", "CHANGED"),
                        Map.entry("maintenanceStep.lifecycle-reconcile.changed", "true"),
                        Map.entry("maintenanceStep.lifecycle-reconcile.consistent", "true"),
                        Map.entry("maintenanceStep.lifecycle-reconcile.changes", "1"),
                        Map.entry("maintenanceStep.event-prune.status", "CHANGED"),
                        Map.entry("maintenanceStep.event-prune.changed", "true"),
                        Map.entry("maintenanceStep.event-prune.consistent", "true"),
                        Map.entry("maintenanceStep.event-prune.changes", "3"))));
        eventSink.record(TestSkillManagementAdminFixtures.event(
                2,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                false,
                Map.ofEntries(
                        Map.entry("operationId", "deploy-2"),
                        Map.entry("parentOperationId", "release-1"),
                        Map.entry("preflightReady", "false"),
                        Map.entry("preflightDeployable", "false"),
                        Map.entry("preflightErrors", "1"),
                        Map.entry("preflightMessage", "No custom source registered"),
                        Map.entry("preflightSourceStoreErrors", "1"),
                        Map.entry("preflightSourceStoreMessage", "No custom source registered"),
                        Map.entry("preflightCapabilityErrors", "0"),
                        Map.entry("errorType", "IllegalStateException"),
                        Map.entry("error", "No custom source registered"))));

        SkillManagementAdminDeploymentHistoryPage view =
                SkillManagementAdminDeploymentHistoryViews.deploymentHistory(
                        eventSink.query(SkillManagementEventQuery.deployments(10)));

        assertThat(view.matchedDeployments()).isEqualTo(2);
        assertThat(view.returnedDeployments()).isEqualTo(2);
        assertThat(view.truncated()).isFalse();
        assertThat(view.successfulDeployments()).isEqualTo(1);
        assertThat(view.failedDeployments()).isEqualTo(1);
        assertThat(view.changedDeployments()).isEqualTo(1);
        assertThat(view.consistentDeployments()).isEqualTo(1);
        assertThat(view.preflightDeployments()).isEqualTo(1);
        assertThat(view.preflightConfigurationFailures()).isZero();
        assertThat(view.preflightTargetStoreFailures()).isZero();
        assertThat(view.preflightSourceStoreFailures()).isEqualTo(1);
        assertThat(view.preflightCapabilityFailures()).isZero();
        assertThat(view.stepSummaries())
                .extracting(SkillManagementAdminMaintenanceStepHistorySummary::step)
                .containsExactly(
                        "definition-sync",
                        "artifact-sync",
                        "lifecycle-reconcile",
                        "event-prune");
        assertThat(view.stepSummaries())
                .extracting(SkillManagementAdminMaintenanceStepHistorySummary::deployments)
                .containsExactly(1, 1, 1, 1);
        assertThat(view.stepSummaries())
                .extracting(SkillManagementAdminMaintenanceStepHistorySummary::changedDeployments)
                .containsExactly(1, 1, 1, 1);
        assertThat(view.stepSummaries().get(0).changes()).isEqualTo(2);
        assertThat(view.stepSummaries().get(3).changes()).isEqualTo(3);
        assertThat(view.deployments()).hasSize(2);
        SkillManagementAdminDeploymentHistoryEntry success = view.deployments().get(0);
        assertThat(success.occurredAt()).isEqualTo("2026-01-01T00:00:01Z");
        assertThat(success.operationId()).isEqualTo("deploy-1");
        assertThat(success.parentOperationId()).isEmpty();
        assertThat(success.success()).isTrue();
        assertThat(success.changed()).isTrue();
        assertThat(success.consistent()).isTrue();
        assertThat(success.definitionChanges()).isEqualTo(2);
        assertThat(success.artifactChanges()).isEqualTo(1);
        assertThat(success.lifecycleCreated()).isEqualTo(1);
        assertThat(success.eventPruneEnabled()).isTrue();
        assertThat(success.eventPruned()).isEqualTo(3);
        assertThat(success.preflightAvailable()).isFalse();
        assertThat(success.steps())
                .extracting(SkillManagementAdminMaintenanceStepReport::step)
                .containsExactly(
                        "definition-sync",
                        "artifact-sync",
                        "lifecycle-reconcile",
                        "event-prune");
        assertThat(success.steps())
                .extracting(SkillManagementAdminMaintenanceStepReport::status)
                .containsExactly("CHANGED", "CHANGED", "CHANGED", "CHANGED");
        assertThat(success.steps().get(0).changes()).isEqualTo(2);
        assertThat(success.steps().get(3).changes()).isEqualTo(3);
        SkillManagementAdminDeploymentHistoryEntry failure = view.deployments().get(1);
        assertThat(failure.operationId()).isEqualTo("deploy-2");
        assertThat(failure.parentOperationId()).isEqualTo("release-1");
        assertThat(failure.success()).isFalse();
        assertThat(failure.preflightAvailable()).isTrue();
        assertThat(failure.preflightReady()).isFalse();
        assertThat(failure.preflightDeployable()).isFalse();
        assertThat(failure.preflightErrors()).isEqualTo(1);
        assertThat(failure.preflightSourceStoreErrors()).isEqualTo(1);
        assertThat(failure.preflightCapabilityErrors()).isZero();
        assertThat(failure.preflightMessage()).isEqualTo("No custom source registered");
        assertThat(failure.errorType()).isEqualTo("IllegalStateException");
        assertThat(failure.error()).isEqualTo("No custom source registered");
        assertThat(failure.steps()).isEmpty();
    }

    @Test
    void deploymentHistoryEntryDecodesAndNormalizesEventAttributes() {
        SkillManagementEvent event = TestSkillManagementAdminFixtures.event(
                3,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                false,
                Map.ofEntries(
                        Map.entry("operationId", " deploy-3 "),
                        Map.entry("parentOperationId", " release-1 "),
                        Map.entry("changed", "true"),
                        Map.entry("consistent", "false"),
                        Map.entry("definitionChanges", "-3"),
                        Map.entry("artifactChanges", "invalid"),
                        Map.entry("artifactConflicts", "2"),
                        Map.entry("eventPruned", "-1"),
                        Map.entry("preflightReady", "false"),
                        Map.entry("preflightDeployable", "false"),
                        Map.entry("preflightErrors", "4"),
                        Map.entry("preflightConfigurationErrors", "-2"),
                        Map.entry("preflightSourceStoreErrors", "invalid"),
                        Map.entry("preflightCapabilityErrors", "1"),
                        Map.entry("preflightMessage", "capability failed")));

        SkillManagementAdminDeploymentHistoryEntry entry =
                SkillManagementAdminDeploymentHistoryViews.deploymentHistoryEntry(event);

        assertThat(entry.occurredAt()).isEqualTo("2026-01-01T00:00:03Z");
        assertThat(entry.operationId()).isEqualTo("deploy-3");
        assertThat(entry.parentOperationId()).isEqualTo("release-1");
        assertThat(entry.success()).isFalse();
        assertThat(entry.changed()).isTrue();
        assertThat(entry.consistent()).isFalse();
        assertThat(entry.definitionChanges()).isZero();
        assertThat(entry.artifactChanges()).isZero();
        assertThat(entry.artifactConflicts()).isEqualTo(2);
        assertThat(entry.eventPruned()).isZero();
        assertThat(entry.preflightAvailable()).isTrue();
        assertThat(entry.preflightErrors()).isEqualTo(4);
        assertThat(entry.preflightConfigurationErrors()).isZero();
        assertThat(entry.preflightSourceStoreErrors()).isZero();
        assertThat(entry.preflightCapabilityErrors()).isEqualTo(1);
        assertThat(entry.preflightMessage()).isEqualTo("capability failed");
        assertThat(entry.errorType()).isEmpty();
        assertThat(entry.error()).isEmpty();

        SkillManagementAdminDeploymentHistoryEntry messageOnlyPreflight =
                SkillManagementAdminDeploymentHistoryViews.deploymentHistoryEntry(
                        TestSkillManagementAdminFixtures.event(
                                4,
                                SkillManagementEventOperation.DEPLOYMENT,
                                "",
                                false,
                                Map.of("preflightMessage", "checked elsewhere")));
        assertThat(messageOnlyPreflight.preflightAvailable()).isTrue();
        assertThat(messageOnlyPreflight.preflightErrors()).isZero();
    }

    @Test
    void deploymentHistoryPageDerivesSummaryCountsFromEntries() {
        SkillManagementAdminDeploymentHistoryEntry success = new SkillManagementAdminDeploymentHistoryEntry(
                "2026-01-01T00:00:01Z",
                "deploy-1",
                "",
                true,
                false,
                true,
                true,
                1,
                0,
                0,
                0,
                0,
                false,
                true,
                false,
                0,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                "",
                "",
                "",
                List.of(
                        new SkillManagementAdminMaintenanceStepReport(
                                "definition-sync",
                                "CHANGED",
                                false,
                                false,
                                true,
                                true,
                                1,
                                0,
                                ""),
                        new SkillManagementAdminMaintenanceStepReport(
                                "event-prune",
                                "SKIPPED",
                                false,
                                true,
                                false,
                                true,
                                0,
                                0,
                                "")));
        SkillManagementAdminDeploymentHistoryEntry preflightFailure =
                new SkillManagementAdminDeploymentHistoryEntry(
                        "2026-01-01T00:00:02Z",
                        "deploy-2",
                        "release-1",
                        false,
                        false,
                        false,
                        false,
                        0,
                        0,
                        0,
                        0,
                        0,
                        false,
                        false,
                        false,
                        0,
                        true,
                        false,
                        false,
                        2,
                        1,
                        0,
                        1,
                        0,
                        "config and source failed",
                        "SkillManagementDeploymentPreflightException",
                        "preflight failed");

        SkillManagementAdminDeploymentHistoryPage page = new SkillManagementAdminDeploymentHistoryPage(
                0,
                99,
                false,
                99,
                99,
                99,
                99,
                99,
                99,
                99,
                99,
                99,
                java.util.Arrays.asList(success, null, preflightFailure));

        assertThat(page.matchedDeployments()).isEqualTo(2);
        assertThat(page.returnedDeployments()).isEqualTo(2);
        assertThat(page.truncated()).isFalse();
        assertThat(page.successfulDeployments()).isEqualTo(1);
        assertThat(page.failedDeployments()).isEqualTo(1);
        assertThat(page.changedDeployments()).isEqualTo(1);
        assertThat(page.consistentDeployments()).isEqualTo(1);
        assertThat(page.preflightDeployments()).isEqualTo(1);
        assertThat(page.preflightConfigurationFailures()).isEqualTo(1);
        assertThat(page.preflightTargetStoreFailures()).isZero();
        assertThat(page.preflightSourceStoreFailures()).isEqualTo(1);
        assertThat(page.preflightCapabilityFailures()).isZero();
        assertThat(page.stepSummaries())
                .extracting(SkillManagementAdminMaintenanceStepHistorySummary::step)
                .containsExactly("definition-sync", "event-prune");
        assertThat(page.stepSummaries().get(0).changedDeployments()).isEqualTo(1);
        assertThat(page.stepSummaries().get(0).changes()).isEqualTo(1);
        assertThat(page.stepSummaries().get(1).skippedDeployments()).isEqualTo(1);
    }

    @Test
    void maintenanceStepHistorySummaryNormalizesCounts() {
        SkillManagementAdminMaintenanceStepHistorySummary summary =
                new SkillManagementAdminMaintenanceStepHistorySummary(
                        " ",
                        -1,
                        -2,
                        -3,
                        -4,
                        -5,
                        -6,
                        -7,
                        -8);

        assertThat(summary.step()).isBlank();
        assertThat(summary.deployments()).isZero();
        assertThat(summary.dryRunDeployments()).isZero();
        assertThat(summary.skippedDeployments()).isZero();
        assertThat(summary.changedDeployments()).isZero();
        assertThat(summary.consistentDeployments()).isZero();
        assertThat(summary.failedDeployments()).isZero();
        assertThat(summary.changes()).isZero();
        assertThat(summary.conflicts()).isZero();
    }
}
