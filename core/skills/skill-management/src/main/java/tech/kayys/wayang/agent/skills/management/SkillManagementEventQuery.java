package tech.kayys.wayang.agent.skills.management;

/**
 * Filters recent skill-management events held by queryable sinks.
 */
public record SkillManagementEventQuery(
        SkillManagementEventOperation operation,
        String skillId,
        Boolean success,
        String operationId,
        String parentOperationId,
        int limit) {

    public static final int DEFAULT_LIMIT = SkillManagementQueryLimits.DEFAULT_LIMIT;
    public static final int MAX_LIMIT = SkillManagementQueryLimits.MAX_LIMIT;

    public SkillManagementEventQuery {
        skillId = skillId == null ? "" : skillId.trim();
        operationId = SkillManagementValueSupport.identifier(operationId);
        parentOperationId = SkillManagementValueSupport.identifier(parentOperationId);
        limit = normalizeLimit(limit);
    }

    public SkillManagementEventQuery(
            SkillManagementEventOperation operation,
            String skillId,
            Boolean success,
            int limit) {
        this(operation, skillId, success, "", "", limit);
    }

    public static SkillManagementEventQuery latest() {
        return new SkillManagementEventQuery(null, "", null, DEFAULT_LIMIT);
    }

    public static SkillManagementEventQuery forSkill(String skillId, int limit) {
        return new SkillManagementEventQuery(null, skillId, null, limit);
    }

    public static SkillManagementEventQuery forOperation(
            SkillManagementEventOperation operation,
            int limit) {
        return new SkillManagementEventQuery(operation, "", null, limit);
    }

    public static SkillManagementEventQuery failures(int limit) {
        return new SkillManagementEventQuery(null, "", Boolean.FALSE, limit);
    }

    public static SkillManagementEventQuery deployments(int limit) {
        return forOperation(SkillManagementEventOperation.DEPLOYMENT, limit);
    }

    public static SkillManagementEventQuery forOperationId(String operationId, int limit) {
        return new SkillManagementEventQuery(null, "", null, operationId, "", limit);
    }

    public static SkillManagementEventQuery forParentOperationId(String parentOperationId, int limit) {
        return new SkillManagementEventQuery(null, "", null, "", parentOperationId, limit);
    }

    public boolean matches(SkillManagementEvent event) {
        if (event == null) {
            return false;
        }
        if (operation != null && event.operation() != operation) {
            return false;
        }
        if (!skillId.isBlank() && !skillId.equals(event.skillId())) {
            return false;
        }
        if (!operationId.isBlank() || !parentOperationId.isBlank()) {
            SkillManagementEventAttributeReader attributes = SkillManagementEventAttributeReader.from(event);
            if (!operationId.isBlank() && !operationId.equals(attributes.operationId())) {
                return false;
            }
            if (!parentOperationId.isBlank() && !parentOperationId.equals(attributes.parentOperationId())) {
                return false;
            }
        }
        return success == null || event.success() == success;
    }

    static int normalizeLimit(int value) {
        return SkillManagementQueryLimits.normalize(value);
    }
}
