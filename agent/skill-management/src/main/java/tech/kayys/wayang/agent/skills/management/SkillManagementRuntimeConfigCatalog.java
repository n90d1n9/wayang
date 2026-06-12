package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Discoverable runtime configuration surface for skill management.
 */
public record SkillManagementRuntimeConfigCatalog(
        List<SkillManagementRuntimeConfigGroup> groups) {

    public SkillManagementRuntimeConfigCatalog {
        groups = groups == null ? List.of() : groups.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public SkillManagementRuntimeConfigGroup group(String name) {
        String expected = name == null ? "" : name.trim();
        return groups.stream()
                .filter(group -> group.name().equals(expected))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown runtime config hint group: " + name));
    }

    public SkillManagementRuntimeConfigCatalog selectGroup(String name) {
        return new SkillManagementRuntimeConfigCatalog(List.of(group(name)));
    }

    public List<SkillManagementRuntimeConfigGroupSummary> groupSummaries() {
        return groups.stream()
                .map(SkillManagementRuntimeConfigGroupSummary::from)
                .toList();
    }

    public int hintCount() {
        return groups.stream()
                .mapToInt(group -> group.hints().size())
                .sum();
    }
}
