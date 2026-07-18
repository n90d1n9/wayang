package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Result of assembling and optionally reconciling skill-management persistence.
 */
public record SkillManagementBootstrapResult(
        SkillManagementService service,
        SkillManagementServiceConfig config,
        SkillManagementInspection initialInspection,
        SkillLifecycleStateReconcileResult lifecycleStateReconcileResult,
        SkillManagementInspection finalInspection) {

    public SkillManagementBootstrapResult {
        service = Objects.requireNonNull(service, "service");
        config = SkillManagementConfigResolution.serviceConfig(config);
        initialInspection = Objects.requireNonNull(initialInspection, "initialInspection");
        lifecycleStateReconcileResult =
                Objects.requireNonNull(lifecycleStateReconcileResult, "lifecycleStateReconcileResult");
        finalInspection = Objects.requireNonNull(finalInspection, "finalInspection");
    }

    public boolean ready() {
        return finalInspection.ready();
    }

    public boolean lifecycleStateConsistent() {
        return finalInspection.lifecycleStateConsistent();
    }

    public boolean lifecycleStateChanged() {
        return !lifecycleStateReconcileResult.createdStateSkillIds().isEmpty()
                || !lifecycleStateReconcileResult.removedStateSkillIds().isEmpty();
    }
}
