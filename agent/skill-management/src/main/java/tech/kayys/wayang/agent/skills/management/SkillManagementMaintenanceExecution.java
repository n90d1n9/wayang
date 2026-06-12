package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Executes a maintenance plan against already-created target stores.
 */
final class SkillManagementMaintenanceExecution {

    private final SkillManagementStoreBundleFactory storeBundleFactory;
    private final SkillManagementMaintenanceRunnerFactory maintenanceRunnerFactory;

    SkillManagementMaintenanceExecution(
            SkillManagementStoreBundleFactory storeBundleFactory,
            SkillManagementMaintenanceRunnerFactory maintenanceRunnerFactory) {
        this.storeBundleFactory = Objects.requireNonNull(storeBundleFactory, "storeBundleFactory");
        this.maintenanceRunnerFactory =
                Objects.requireNonNull(maintenanceRunnerFactory, "maintenanceRunnerFactory");
    }

    SkillManagementMaintenanceResult run(
            SkillManagementStoreBundle targetStores,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan,
            SkillManagementOperationContext context) {
        SkillManagementMaintenanceStores stores =
                storeBundleFactory.maintenanceStores(sourceConfig, targetStores);
        SkillManagementMaintenanceRunner runner = maintenanceRunnerFactory.create(stores.eventSink());
        return runner.run(
                SkillManagementMaintenanceInputs.withArtifacts(
                        stores.sourceDefinitions(),
                        stores.targetDefinitions(),
                        stores.lifecycleStateStore(),
                        stores.sourceArtifacts(),
                        stores.targetArtifacts()),
                plan,
                context);
    }
}
