package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.OPERATION_ID;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.PARENT_OPERATION_ID;

/**
 * Correlates maintenance/deployment events that belong to the same workflow.
 */
record SkillManagementOperationContext(
        String operationId,
        String parentOperationId) {

    SkillManagementOperationContext {
        operationId = normalize(operationId);
        parentOperationId = parentOperationId == null || parentOperationId.isBlank()
                ? ""
                : parentOperationId;
    }

    static SkillManagementOperationContext root() {
        return new SkillManagementOperationContext(newId(), "");
    }

    static SkillManagementOperationContext of(String operationId) {
        return new SkillManagementOperationContext(operationId, "");
    }

    SkillManagementOperationContext child() {
        return new SkillManagementOperationContext(newId(), operationId);
    }

    Map<String, String> attributes() {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        attributes.put(OPERATION_ID, operationId);
        if (!parentOperationId.isBlank()) {
            attributes.put(PARENT_OPERATION_ID, parentOperationId);
        }
        return Map.copyOf(attributes);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? newId() : value;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
