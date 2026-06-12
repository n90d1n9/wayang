package tech.kayys.wayang.agent.skills.management;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stable admin-facing projection of a skill-management event.
 */
public record SkillManagementAdminEvent(
        String occurredAt,
        String operation,
        String skillId,
        String operationId,
        String parentOperationId,
        boolean success,
        Map<String, String> attributes) {

    public SkillManagementAdminEvent {
        occurredAt = SkillManagementAdminValueSupport.text(occurredAt);
        operation = SkillManagementAdminValueSupport.unknownIfBlank(operation);
        skillId = SkillManagementAdminValueSupport.text(skillId);
        operationId = SkillManagementAdminValueSupport.identifier(operationId);
        parentOperationId = SkillManagementAdminValueSupport.identifier(parentOperationId);
        attributes = copy(attributes);
    }

    private static Map<String, String> copy(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, String> copy = new TreeMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(key, value);
            }
        });
        return Collections.unmodifiableMap(copy);
    }
}
