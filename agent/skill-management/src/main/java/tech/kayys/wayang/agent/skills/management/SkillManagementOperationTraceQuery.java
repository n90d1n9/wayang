package tech.kayys.wayang.agent.skills.management;

/**
 * Query options for assembling operation-trace pages.
 */
public record SkillManagementOperationTraceQuery(
        int operationLimit,
        int childEventLimit,
        SkillManagementOperationTraceStatus status) {

    public SkillManagementOperationTraceQuery {
        operationLimit = SkillManagementQueryLimits.normalize(operationLimit);
        childEventLimit = SkillManagementQueryLimits.normalize(childEventLimit);
    }

    public static SkillManagementOperationTraceQuery deployments(
            int operationLimit,
            int childEventLimit) {
        return new SkillManagementOperationTraceQuery(operationLimit, childEventLimit, null);
    }

    public static SkillManagementOperationTraceQuery deploymentsByStatus(
            int operationLimit,
            int childEventLimit,
            SkillManagementOperationTraceStatus status) {
        return new SkillManagementOperationTraceQuery(operationLimit, childEventLimit, status);
    }

    public static SkillManagementOperationTraceQuery deploymentsByStatusName(
            int operationLimit,
            int childEventLimit,
            String status) {
        if (SkillManagementValueSupport.identifier(status).isBlank()) {
            return deployments(operationLimit, childEventLimit);
        }
        return deploymentsByStatus(
                operationLimit,
                childEventLimit,
                SkillManagementOperationTraceStatus.require(status));
    }

    boolean matches(SkillManagementAdminOperationTrace trace) {
        if (trace == null) {
            return false;
        }
        return status == null || status.name().equals(trace.status());
    }

    String statusFilter() {
        return status == null ? "" : status.name();
    }
}
