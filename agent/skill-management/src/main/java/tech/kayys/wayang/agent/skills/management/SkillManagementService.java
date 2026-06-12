package tech.kayys.wayang.agent.skills.management;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wayang-native skill lifecycle service backed by a configurable skill
 * definition store.
 */
public class SkillManagementService {

    private final SkillManagementServiceRuntime runtime;

    public SkillManagementService(SkillRegistry registry) {
        this(SkillManagementServiceDefaults.definitionStore(registry));
    }

    public SkillManagementService(SkillDefinitionStore store) {
        this(store,
                SkillManagementServiceDefaults.definitionStoreInspector(),
                SkillManagementServiceDefaults.lifecycleStateStore());
    }

    public SkillManagementService(SkillDefinitionStore store, SkillDefinitionStoreInspector storeInspector) {
        this(store, storeInspector, SkillManagementServiceDefaults.lifecycleStateStore());
    }

    public SkillManagementService(SkillDefinitionStore store, SkillLifecycleStateStore lifecycleStateStore) {
        this(store, SkillManagementServiceDefaults.definitionStoreInspector(), lifecycleStateStore);
    }

    public SkillManagementService(
            SkillDefinitionStore store,
            SkillDefinitionStoreInspector storeInspector,
            SkillLifecycleStateStore lifecycleStateStore) {
        this(store, storeInspector, lifecycleStateStore,
                SkillManagementServiceDefaults.lifecycleStateStoreInspector());
    }

    public SkillManagementService(
            SkillDefinitionStore store,
            SkillDefinitionStoreInspector storeInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector) {
        this(store,
                storeInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                SkillManagementServiceDefaults.eventSink());
    }

    public SkillManagementService(
            SkillDefinitionStore store,
            SkillDefinitionStoreInspector storeInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventSink eventSink) {
        this(store,
                storeInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                eventSink,
                SkillManagementServiceDefaults.eventReader(eventSink));
    }

    public SkillManagementService(
            SkillDefinitionStore store,
            SkillDefinitionStoreInspector storeInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventSink eventSink,
            SkillManagementEventReader eventReader) {
        this(store,
                storeInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                SkillManagementServiceDefaults.artifactStore(),
                eventSink,
                eventReader);
    }

    public SkillManagementService(
            SkillDefinitionStore store,
            SkillDefinitionStoreInspector storeInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillArtifactStore artifactStore,
            SkillManagementEventSink eventSink) {
        this(store,
                storeInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                artifactStore,
                eventSink,
                SkillManagementServiceDefaults.eventReader(eventSink));
    }

    public SkillManagementService(
            SkillDefinitionStore store,
            SkillDefinitionStoreInspector storeInspector,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillArtifactStore artifactStore,
            SkillManagementEventSink eventSink,
            SkillManagementEventReader eventReader) {
        this.runtime = SkillManagementServiceRuntime.assemble(
                store,
                storeInspector,
                lifecycleStateStore,
                lifecycleStateStoreInspector,
                artifactStore,
                eventSink,
                eventReader);
    }

    public Uni<SkillDefinition> createSkill(SkillDefinition skill) {
        return SkillManagementAsync.item(() -> {
            runtime.definitionValidator().requireValid(skill);
            return runtime.definitionMutationRunner().create(skill);
        });
    }

    public Uni<Optional<SkillDefinition>> getSkill(String skillId) {
        return SkillManagementAsync.item(() -> runtime.catalogReader().get(skillId));
    }

    public Uni<List<SkillDefinition>> listSkills() {
        return SkillManagementAsync.item(runtime.catalogReader()::list);
    }

    public Uni<List<SkillDefinition>> listActiveSkills() {
        return SkillManagementAsync.item(runtime.catalogReader()::listActive);
    }

    public Uni<List<SkillDefinition>> listByCategory(String category) {
        return SkillManagementAsync.item(() -> runtime.catalogReader().listByCategory(category));
    }

    public Uni<List<SkillDefinition>> search(String query, String category, boolean includeDisabled) {
        return SkillManagementAsync.item(() -> runtime.catalogReader().search(query, category, includeDisabled));
    }

    public Uni<SkillDefinition> updateSkill(String skillId, SkillDefinition skill) {
        return SkillManagementAsync.item(() -> {
            runtime.definitionValidator().requireValid(skill);
            return runtime.definitionMutationRunner().update(skillId, skill);
        });
    }

    public Uni<Boolean> deleteSkill(String skillId) {
        return SkillManagementAsync.item(() -> runtime.definitionMutationRunner().delete(skillId));
    }

    public Uni<SkillLifecycleState> enableSkill(String skillId) {
        return SkillManagementAsync.item(
                () -> runtime.lifecycleRunner().transition(skillId, SkillLifecycleStatus.ACTIVE));
    }

    public Uni<SkillLifecycleState> disableSkill(String skillId) {
        return SkillManagementAsync.item(
                () -> runtime.lifecycleRunner().transition(skillId, SkillLifecycleStatus.DISABLED));
    }

    public Uni<SkillLifecycleState> deprecateSkill(String skillId) {
        return SkillManagementAsync.item(
                () -> runtime.lifecycleRunner().transition(skillId, SkillLifecycleStatus.DEPRECATED));
    }

    public Uni<SkillLifecycleState> getLifecycleState(String skillId) {
        return SkillManagementAsync.item(() -> runtime.lifecycleRunner().stateForExisting(skillId));
    }

    public Uni<Map<String, SkillLifecycleState>> lifecycleSnapshot() {
        return SkillManagementAsync.item(runtime.lifecycleRunner()::snapshot);
    }

    public Uni<SkillDefinitionStoreInspection> inspectStore() {
        return SkillManagementAsync.item(runtime.inspectionReader()::definitionStore);
    }

    public Uni<SkillLifecycleStateStoreInspection> inspectLifecycleStateStore() {
        return SkillManagementAsync.item(runtime.inspectionReader()::lifecycleStateStore);
    }

    public Uni<SkillArtifactStoreInspection> inspectArtifactStore() {
        return SkillManagementAsync.item(runtime.inspectionReader()::artifactStore);
    }

    public Uni<SkillLifecycleStateReconcileResult> reconcileLifecycleState(
            SkillLifecycleStateReconcileOptions options) {
        return SkillManagementAsync.item(() -> runtime.lifecycleRunner().reconcile(options));
    }

    public Uni<SkillManagementInspection> inspectManagement() {
        return SkillManagementAsync.item(runtime.inspectionReader()::management);
    }

    public Uni<SkillManagementEventPage> eventHistory(SkillManagementEventQuery query) {
        return SkillManagementAsync.item(() -> runtime.eventHistory().query(query));
    }

    public Uni<SkillManagementEventPage> eventHistoryForOperation(String operationId, int limit) {
        return eventHistory(SkillManagementEventQuery.forOperationId(operationId, limit));
    }

    public Uni<SkillManagementEventPage> eventHistoryForParentOperation(String parentOperationId, int limit) {
        return eventHistory(SkillManagementEventQuery.forParentOperationId(parentOperationId, limit));
    }

    public Uni<SkillManagementAdminOperationTrace> operationTrace(String operationId, int limit) {
        return SkillManagementAsync.item(() -> runtime.operationTraceReader().trace(operationId, limit));
    }

    public Uni<SkillManagementAdminOperationTracePage> deploymentOperationTraces(
            int operationLimit,
            int childEventLimit) {
        return deploymentOperationTraces(
                SkillManagementOperationTraceQuery.deployments(operationLimit, childEventLimit));
    }

    public Uni<SkillManagementAdminOperationTracePage> deploymentOperationTraces(
            int operationLimit,
            int childEventLimit,
            String status) {
        return deploymentOperationTraces(
                SkillManagementOperationTraceQuery.deploymentsByStatusName(
                        operationLimit,
                        childEventLimit,
                        status));
    }

    public Uni<SkillManagementAdminOperationTracePage> deploymentOperationTraces(
            SkillManagementOperationTraceQuery query) {
        return SkillManagementAsync.item(() -> runtime.operationTraceReader().deploymentTraces(query));
    }

    public Uni<SkillManagementEventPruneResult> pruneEventHistory(SkillManagementEventPruneOptions options) {
        return SkillManagementAsync.item(() -> runtime.eventHistory().prune(options));
    }

    public Uni<Optional<SkillArtifact>> getArtifact(SkillArtifactReference reference) {
        return SkillManagementAsync.item(() -> runtime.artifactReader().get(reference));
    }

    public Uni<List<SkillArtifactReference>> listArtifacts(SkillArtifactQuery query) {
        return SkillManagementAsync.item(() -> runtime.artifactReader().list(query));
    }

    public Uni<List<SkillArtifactReference>> listArtifacts(String skillId) {
        return SkillManagementAsync.item(() -> runtime.artifactReader().list(skillId));
    }

    public Uni<SkillArtifact> putArtifact(SkillArtifact artifact) {
        return SkillManagementAsync.item(() -> runtime.artifactMutationRunner().put(artifact));
    }

    public Uni<Boolean> deleteArtifact(SkillArtifactReference reference) {
        return SkillManagementAsync.item(() -> runtime.artifactMutationRunner().delete(reference));
    }

    public Uni<SkillArtifactStoreSyncResult> syncArtifacts(
            SkillArtifactStore sourceArtifacts,
            SkillArtifactStoreSyncOptions options) {
        return SkillManagementAsync.item(() -> runtime.artifactSyncWorkflow().sync(sourceArtifacts, options));
    }

    public Uni<SkillManagementMaintenanceResult> runMaintenance(
            SkillDefinitionStore sourceDefinitions,
            SkillManagementMaintenancePlan plan) {
        return SkillManagementAsync.item(() -> runtime.maintenanceWorkflow().run(sourceDefinitions, plan));
    }

    public Uni<SkillManagementMaintenanceResult> runMaintenance(
            SkillDefinitionStore sourceDefinitions,
            SkillArtifactStore sourceArtifacts,
            SkillManagementMaintenancePlan plan) {
        return SkillManagementAsync.item(
                () -> runtime.maintenanceWorkflow().run(sourceDefinitions, sourceArtifacts, plan));
    }

    public Uni<SkillManagementMaintenanceResult> runMaintenance(SkillDefinitionStore sourceDefinitions) {
        return runMaintenance(sourceDefinitions, SkillManagementConfigResolution.maintenancePlan(null));
    }

    public SkillValidation validateSkill(SkillDefinition skill) {
        return runtime.definitionValidator().validate(skill);
    }

}
