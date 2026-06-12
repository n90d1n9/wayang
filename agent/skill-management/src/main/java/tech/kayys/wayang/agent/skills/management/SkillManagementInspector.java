package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Composes definition-store, lifecycle-store, and reconciliation diagnostics.
 */
public final class SkillManagementInspector {

    private static final String RECONCILE_SKIPPED =
            "Skipped lifecycle reconciliation because one or more stores are unavailable";

    private final SkillDefinitionStoreInspector definitionStoreInspector;
    private final SkillLifecycleStateStoreInspector lifecycleStateStoreInspector;
    private final SkillManagementEventStoreInspector eventStoreInspector;
    private final SkillArtifactStoreInspector artifactStoreInspector;
    private final SkillLifecycleStateReconciler lifecycleStateReconciler;

    public SkillManagementInspector() {
        this(new SkillDefinitionStoreInspector(),
                new SkillLifecycleStateStoreInspector(),
                new SkillManagementEventStoreInspector(),
                new SkillArtifactStoreInspector(),
                new SkillLifecycleStateReconciler());
    }

    public SkillManagementInspector(
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreInspector eventStoreInspector,
            SkillLifecycleStateReconciler lifecycleStateReconciler) {
        this(definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreInspector,
                new SkillArtifactStoreInspector(),
                lifecycleStateReconciler);
    }

    public SkillManagementInspector(
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreInspector eventStoreInspector,
            SkillArtifactStoreInspector artifactStoreInspector,
            SkillLifecycleStateReconciler lifecycleStateReconciler) {
        this.definitionStoreInspector = Objects.requireNonNull(definitionStoreInspector, "definitionStoreInspector");
        this.lifecycleStateStoreInspector =
                Objects.requireNonNull(lifecycleStateStoreInspector, "lifecycleStateStoreInspector");
        this.eventStoreInspector = Objects.requireNonNull(eventStoreInspector, "eventStoreInspector");
        this.artifactStoreInspector = Objects.requireNonNull(artifactStoreInspector, "artifactStoreInspector");
        this.lifecycleStateReconciler =
                Objects.requireNonNull(lifecycleStateReconciler, "lifecycleStateReconciler");
    }

    public SkillManagementInspection inspect(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore lifecycleStateStore) {
        return inspect(
                definitionStore,
                lifecycleStateStore,
                SkillManagementEventReader.empty(),
                new InMemorySkillArtifactStore());
    }

    public SkillManagementInspection inspect(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillManagementEventReader eventReader) {
        return inspect(definitionStore, lifecycleStateStore, eventReader, new InMemorySkillArtifactStore());
    }

    public SkillManagementInspection inspect(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillManagementEventReader eventReader,
            SkillArtifactStore artifactStore) {
        Objects.requireNonNull(definitionStore, "definitionStore");
        Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        Objects.requireNonNull(eventReader, "eventReader");
        Objects.requireNonNull(artifactStore, "artifactStore");

        SkillDefinitionStoreInspection definitionInspection =
                definitionStoreInspector.inspect("skills", definitionStore);
        SkillLifecycleStateStoreInspection lifecycleInspection =
                lifecycleStateStoreInspector.inspect("lifecycle", lifecycleStateStore);
        SkillManagementEventStoreInspection eventInspection =
                eventStoreInspector.inspect("events", eventReader);
        SkillArtifactStoreInspection artifactInspection =
                artifactStoreInspector.inspect("artifacts", artifactStore);
        if (!definitionInspection.ready() || !lifecycleInspection.ready()) {
            return new SkillManagementInspection(
                    definitionInspection,
                    lifecycleInspection,
                    eventInspection,
                    artifactInspection,
                    emptyReconciliation(definitionInspection, lifecycleInspection),
                    RECONCILE_SKIPPED);
        }

        try {
            return new SkillManagementInspection(
                    definitionInspection,
                    lifecycleInspection,
                    eventInspection,
                    artifactInspection,
                    lifecycleStateReconciler.reconcile(
                            definitionStore,
                            lifecycleStateStore,
                            SkillLifecycleStateReconcileOptions.inspectOnly()),
                    "");
        } catch (RuntimeException error) {
            return new SkillManagementInspection(
                    definitionInspection,
                    lifecycleInspection,
                    eventInspection,
                    artifactInspection,
                    emptyReconciliation(definitionInspection, lifecycleInspection),
                    SkillStoreInspectionSupport.errorMessage(error));
        }
    }

    private SkillLifecycleStateReconcileResult emptyReconciliation(
            SkillDefinitionStoreInspection definitionInspection,
            SkillLifecycleStateStoreInspection lifecycleInspection) {
        return new SkillLifecycleStateReconcileResult(
                definitionInspection.ready() ? definitionInspection.skillIds() : List.of(),
                lifecycleInspection.ready() ? lifecycleInspection.skillIds() : List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
