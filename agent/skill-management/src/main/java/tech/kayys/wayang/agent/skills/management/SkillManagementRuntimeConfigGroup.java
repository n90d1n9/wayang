package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Group of related runtime configuration hints.
 */
public record SkillManagementRuntimeConfigGroup(
        String name,
        String label,
        List<SkillManagementRuntimeConfigHint> hints) {

    public SkillManagementRuntimeConfigGroup {
        name = required(name, "name");
        label = required(label, "label");
        hints = hints == null ? List.of() : hints.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static String required(String value, String name) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return resolved;
    }
}
