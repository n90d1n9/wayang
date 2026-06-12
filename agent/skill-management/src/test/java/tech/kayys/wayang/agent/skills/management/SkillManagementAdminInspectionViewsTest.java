package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminInspectionViewsTest {

    @Test
    void mapsRuntimeAggregateInspectionToStableAdminProjection() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        definitions.registerSkill(TestSkillDefinitions.basic("writer"));
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        lifecycle.save(SkillLifecycleState.created("planner"));
        lifecycle.save(SkillLifecycleState.created("orphan").withStatus(SkillLifecycleStatus.DISABLED));
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        artifacts.putArtifact(SkillArtifact.of(SkillArtifactReference.packageArtifact("planner", "v1"),
                new byte[] {1}));

        SkillManagementInspection inspection = new SkillManagementInspector().inspect(
                definitions,
                lifecycle,
                SkillManagementEventReader.empty(),
                artifacts);

        SkillManagementAdminInspection view = SkillManagementAdminInspectionViews.inspection(inspection);

        assertThat(view.ready()).isTrue();
        assertThat(view.lifecycleStateConsistent()).isFalse();
        assertThat(view.definitionStore().status()).isEqualTo("READY");
        assertThat(view.definitionStore().itemIds()).containsExactly("planner", "writer");
        assertThat(view.definitionStore().capabilities()).contains("read", "write", "delete", "list");
        assertThat(view.lifecycleStateStore().statusCounts()).containsEntry("ACTIVE", 1)
                .containsEntry("DISABLED", 1);
        assertThat(view.lifecycleStateStore().children()).isEmpty();
        assertThat(view.eventStore().status()).isEqualTo("READY");
        assertThat(view.eventStore().itemCount()).isZero();
        assertThat(view.eventStore().capabilities()).contains("query-events");
        assertThat(view.eventStore().children()).isEmpty();
        assertThat(view.artifactStore().status()).isEqualTo("READY");
        assertThat(view.artifactStore().itemIds()).containsExactly("planner:package:package:v1");
        assertThat(view.artifactStore().statusCounts()).containsEntry("package", 1);
        assertThat(view.artifactStore().capabilities()).contains("read", "write", "delete", "list");
        assertThat(view.lifecycleStateReconciliation().missingStateSkillIds()).containsExactly("writer");
        assertThat(view.lifecycleStateReconciliation().orphanedStateSkillIds()).containsExactly("orphan");
    }

    @Test
    void mapsAggregateInspectionToStableAdminProjection() {
        SkillLifecycleStateReconcileResult reconciliation = new SkillLifecycleStateReconcileResult(
                List.of("planner", "writer"),
                List.of("planner"),
                List.of("writer"),
                List.of(),
                List.of("writer"),
                List.of());
        SkillManagementInspection inspection = new SkillManagementInspection(
                SkillDefinitionStoreInspection.ready("definitions", "memory", List.of("planner", "writer"), List.of()),
                SkillLifecycleStateStoreInspection.ready(
                        "lifecycle",
                        "memory",
                        List.of("planner"),
                        Map.of(SkillLifecycleStatus.ACTIVE, 1),
                        List.of()),
                SkillManagementEventStoreInspection.ready(
                        "events",
                        "memory",
                        new SkillManagementEventPage(List.of(), 0),
                        List.of()),
                SkillArtifactStoreInspection.ready(
                        "artifacts",
                        "memory",
                        List.of(),
                        List.of(),
                        SkillStoreCapabilities.none()),
                reconciliation,
                "");

        SkillManagementAdminInspection view = SkillManagementAdminInspectionViews.inspection(inspection);

        assertThat(view.ready()).isTrue();
        assertThat(view.lifecycleStateConsistent()).isTrue();
        assertThat(view.definitionStore().itemIds()).containsExactly("planner", "writer");
        assertThat(view.lifecycleStateStore().statusCounts()).containsEntry("ACTIVE", 1);
        assertThat(view.lifecycleStateReconciliation().missingStateSkillIds()).containsExactly("writer");
        assertThat(view.lifecycleStateReconciliation().createdStateSkillIds()).containsExactly("writer");
    }

    @Test
    void mapsReconciliationFailureAsInconsistent() {
        SkillLifecycleStateReconcileResult reconciliation = new SkillLifecycleStateReconcileResult(
                List.of("planner"),
                List.of(),
                List.of("planner"),
                List.of(),
                List.of("planner"),
                List.of());

        SkillManagementAdminReconcileStatus view =
                SkillManagementAdminInspectionViews.reconcile(reconciliation, "state store unavailable");

        assertThat(view.consistent()).isFalse();
        assertThat(view.changed()).isTrue();
        assertThat(view.failure()).isEqualTo("state store unavailable");
        assertThat(view.definitionSkillIds()).containsExactly("planner");
    }

    @Test
    void mapsBootstrapResultWithActionReconciliation() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.basic("planner"));
        SkillManagementServiceFactory factory = new SkillManagementServiceFactory(
                new SkillDefinitionStoreFactory(null, null, Map.of("definitions", definitions)),
                new SkillLifecycleStateStoreFactory());
        SkillManagementBootstrapResult result = new SkillManagementBootstrapper(factory).bootstrap(
                SkillManagementServiceConfig.of(
                        SkillDefinitionStoreConfig.custom("definitions"),
                        SkillLifecycleStateStoreConfig.memory(),
                        SkillLifecycleStateReconcileOptions.createMissing()));

        SkillManagementAdminBootstrapReport view = SkillManagementAdminInspectionViews.bootstrap(result);

        assertThat(view.ready()).isTrue();
        assertThat(view.lifecycleStateChanged()).isTrue();
        assertThat(view.initialInspection().lifecycleStateReconciliation().missingStateSkillIds())
                .containsExactly("planner");
        assertThat(view.lifecycleStateReconciliation().createdStateSkillIds()).containsExactly("planner");
        assertThat(view.lifecycleStateReconciliation().changed()).isTrue();
        assertThat(view.finalInspection().lifecycleStateConsistent()).isTrue();
    }

    @Test
    void reconcileStatusDerivesConsistencyFromNormalizedIdsAndFailure() {
        SkillManagementAdminReconcileStatus resolved = new SkillManagementAdminReconcileStatus(
                false,
                java.util.Arrays.asList("writer", null, "planner", "planner"),
                List.of("planner"),
                List.of("writer"),
                List.of("old"),
                List.of("writer"),
                List.of("old"),
                " ");
        SkillManagementAdminReconcileStatus failed = new SkillManagementAdminReconcileStatus(
                true,
                List.of("writer"),
                List.of(),
                List.of("writer"),
                List.of(),
                List.of(),
                List.of(),
                "reconcile failed");

        assertThat(resolved.consistent()).isTrue();
        assertThat(resolved.changed()).isTrue();
        assertThat(resolved.definitionSkillIds()).containsExactly("planner", "writer");
        assertThat(resolved.failure()).isBlank();
        assertThat(failed.consistent()).isFalse();
        assertThat(failed.changed()).isFalse();
    }

    @Test
    void bootstrapReportDerivesSummaryFromFinalInspectionAndReconcileStatus() {
        SkillManagementAdminReconcileStatus reconcile = new SkillManagementAdminReconcileStatus(
                List.of("planner"),
                List.of(),
                List.of("planner"),
                List.of(),
                List.of("planner"),
                List.of(),
                "");

        SkillManagementAdminBootstrapReport view = new SkillManagementAdminBootstrapReport(
                false,
                true,
                false,
                adminInspection(false, false),
                reconcile,
                adminInspection(true, false));

        assertThat(view.ready()).isTrue();
        assertThat(view.lifecycleStateConsistent()).isFalse();
        assertThat(view.lifecycleStateChanged()).isTrue();
    }

    private SkillManagementAdminInspection adminInspection(boolean ready, boolean lifecycleStateConsistent) {
        SkillManagementAdminStoreStatus store = new SkillManagementAdminStoreStatus(
                "store",
                "memory",
                ready ? "READY" : "UNAVAILABLE",
                ready,
                0,
                List.of(),
                Map.of(),
                "",
                List.of(),
                List.of());
        return new SkillManagementAdminInspection(
                ready,
                lifecycleStateConsistent,
                store,
                store,
                store,
                store,
                new SkillManagementAdminReconcileStatus(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        ""));
    }
}
