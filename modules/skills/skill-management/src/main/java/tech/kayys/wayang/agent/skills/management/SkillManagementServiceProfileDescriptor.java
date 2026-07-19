package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * User-facing metadata for a named skill-management persistence profile.
 */
public record SkillManagementServiceProfileDescriptor(
        SkillManagementServiceProfile profile,
        String label,
        List<String> aliases,
        String description) {

    public SkillManagementServiceProfileDescriptor {
        profile = Objects.requireNonNull(profile, "profile");
        label = label == null || label.isBlank() ? profile.label() : label;
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        description = description == null ? "" : description;
    }
}
