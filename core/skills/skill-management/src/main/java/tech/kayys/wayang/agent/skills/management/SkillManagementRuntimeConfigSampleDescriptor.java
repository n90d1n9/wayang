package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Discoverable runtime config sample that can be rendered by tools or CLI.
 */
public record SkillManagementRuntimeConfigSampleDescriptor(
        String name,
        String profile,
        String objectStorageProvider,
        String description,
        List<String> aliases) {

    public SkillManagementRuntimeConfigSampleDescriptor {
        name = required(name, "name");
        profile = required(profile, "profile");
        objectStorageProvider = objectStorageProvider == null ? "" : objectStorageProvider.trim();
        description = description == null ? "" : description.trim();
        aliases = aliases == null
                ? List.of()
                : aliases.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(alias -> !alias.isBlank())
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
