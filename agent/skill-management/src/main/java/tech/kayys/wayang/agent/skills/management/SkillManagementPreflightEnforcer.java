package tech.kayys.wayang.agent.skills.management;

/**
 * Shared enforcement for operation preflight reports.
 */
final class SkillManagementPreflightEnforcer {

    private SkillManagementPreflightEnforcer() {
    }

    static void enforce(
            SkillManagementEventOperation operation,
            SkillManagementPreflightReport preflight) {
        SkillManagementPreflightReport resolved = neutral(preflight);
        if (resolved.ready()) {
            return;
        }
        throw exception(operation, resolved);
    }

    static void enforce(
            SkillManagementEventOperation operation,
            SkillManagementDeploymentPreflightReport preflight,
            SkillManagementEventSink failureSink,
            SkillManagementOperationContext context) {
        SkillManagementDeploymentPreflightReport resolved = deployment(preflight);
        if (resolved.ready()) {
            return;
        }
        SkillManagementPreflightException error = exception(operation, resolved);
        if (failureSink != null) {
            new SkillManagementEventRecorder(failureSink).failure(
                    operation,
                    "",
                    error,
                    context);
        }
        throw error;
    }

    static SkillManagementPreflightException exception(
            SkillManagementEventOperation operation,
            SkillManagementDeploymentPreflightReport preflight) {
        SkillManagementDeploymentPreflightReport resolved = deployment(preflight);
        if (operation == SkillManagementEventOperation.MAINTENANCE) {
            return new SkillManagementMaintenancePreflightException(resolved.validation());
        }
        return new SkillManagementDeploymentPreflightException(resolved);
    }

    private static SkillManagementPreflightException exception(
            SkillManagementEventOperation operation,
            SkillManagementPreflightReport preflight) {
        SkillManagementPreflightReport resolved = neutral(preflight);
        if (operation == SkillManagementEventOperation.MAINTENANCE) {
            return new SkillManagementMaintenancePreflightException(resolved);
        }
        return new SkillManagementPreflightException(operation, resolved);
    }

    private static SkillManagementPreflightReport neutral(SkillManagementPreflightReport preflight) {
        return SkillManagementPreflightReport.orEmpty(preflight);
    }

    private static SkillManagementDeploymentPreflightReport deployment(
            SkillManagementDeploymentPreflightReport preflight) {
        return SkillManagementDeploymentPreflightReport.orEmpty(preflight);
    }
}
