package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Operator-facing runtime configuration hint.
 */
public record SkillManagementRuntimeConfigHint(
        String name,
        String description,
        List<String> properties,
        List<String> environment,
        String defaultValue,
        List<String> notes) {

    public SkillManagementRuntimeConfigHint {
        name = required(name, "name");
        description = text(description);
        properties = copy(properties);
        environment = copy(environment);
        defaultValue = text(defaultValue);
        notes = copy(notes);
    }

    public boolean hasDefaultValue() {
        return !defaultValue.isBlank();
    }

    private static String required(String value, String name) {
        String resolved = text(value);
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return resolved;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> copy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
