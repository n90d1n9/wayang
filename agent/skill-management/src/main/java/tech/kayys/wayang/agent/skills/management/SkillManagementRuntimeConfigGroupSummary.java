package tech.kayys.wayang.agent.skills.management;

/**
 * Compact summary of one runtime config hint group.
 */
public record SkillManagementRuntimeConfigGroupSummary(
        String name,
        String label,
        int hintCount) {

    public SkillManagementRuntimeConfigGroupSummary {
        name = required(name, "name");
        label = required(label, "label");
        hintCount = Math.max(0, hintCount);
    }

    static SkillManagementRuntimeConfigGroupSummary from(SkillManagementRuntimeConfigGroup group) {
        return new SkillManagementRuntimeConfigGroupSummary(
                group.name(),
                group.label(),
                group.hints().size());
    }

    private static String required(String value, String name) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return resolved;
    }
}
