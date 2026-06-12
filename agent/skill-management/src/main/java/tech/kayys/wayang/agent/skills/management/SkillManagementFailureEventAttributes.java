package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.ERROR;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.ERROR_TYPE;

/**
 * Event-attribute projection for operation context and runtime failures.
 */
final class SkillManagementFailureEventAttributes {

    private SkillManagementFailureEventAttributes() {
    }

    static Map<String, String> withContext(
            Map<String, String> attributes,
            SkillManagementOperationContext context) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        if (attributes != null) {
            copy.putAll(attributes);
        }
        putContext(copy, context);
        return Map.copyOf(copy);
    }

    static Map<String, String> failure(RuntimeException error, Map<String, String> attributes) {
        return failure(error, attributes, null);
    }

    static Map<String, String> failure(
            RuntimeException error,
            Map<String, String> attributes,
            SkillManagementOperationContext context) {
        Objects.requireNonNull(error, "error");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        copy.putAll(SkillManagementPreflightEventAttributes.failure(error));
        if (attributes != null) {
            copy.putAll(attributes);
        }
        putContext(copy, context);
        copy.put(ERROR_TYPE, error.getClass().getSimpleName());
        putIfPresent(copy, ERROR, error.getMessage());
        return Map.copyOf(copy);
    }

    private static void putIfPresent(
            LinkedHashMap<String, String> attributes,
            String key,
            String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    private static void putContext(
            LinkedHashMap<String, String> attributes,
            SkillManagementOperationContext context) {
        if (context != null) {
            attributes.putAll(context.attributes());
        }
    }
}
