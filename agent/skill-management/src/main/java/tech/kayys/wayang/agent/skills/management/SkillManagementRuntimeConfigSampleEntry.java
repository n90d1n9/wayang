package tech.kayys.wayang.agent.skills.management;

/**
 * One rendered runtime configuration sample entry.
 */
public record SkillManagementRuntimeConfigSampleEntry(
        String key,
        String value,
        String description) {

    public SkillManagementRuntimeConfigSampleEntry {
        key = required(key, "key");
        value = value == null ? "" : value.trim();
        description = description == null ? "" : description.trim();
    }

    private static String required(String value, String name) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return resolved;
    }
}
