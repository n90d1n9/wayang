package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Profile-oriented starter runtime configuration for skill management.
 */
public record SkillManagementRuntimeConfigSample(
        String profile,
        String description,
        List<SkillManagementRuntimeConfigSampleEntry> properties,
        List<SkillManagementRuntimeConfigSampleEntry> environment) {

    public SkillManagementRuntimeConfigSample {
        profile = required(profile, "profile");
        description = description == null ? "" : description.trim();
        properties = copy(properties);
        environment = copy(environment);
    }

    private static String required(String value, String name) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return resolved;
    }

    private static List<SkillManagementRuntimeConfigSampleEntry> copy(
            List<SkillManagementRuntimeConfigSampleEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
