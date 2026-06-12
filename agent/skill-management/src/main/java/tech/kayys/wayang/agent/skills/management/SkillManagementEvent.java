package tech.kayys.wayang.agent.skills.management;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable event emitted by skill-management operations.
 */
public record SkillManagementEvent(
        Instant occurredAt,
        SkillManagementEventOperation operation,
        String skillId,
        boolean success,
        Map<String, String> attributes) {

    public SkillManagementEvent {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        operation = Objects.requireNonNull(operation, "operation");
        skillId = skillId == null ? "" : skillId;
        attributes = sanitize(attributes);
    }

    private static Map<String, String> sanitize(Map<String, String> values) {
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
