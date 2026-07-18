package tech.kayys.wayang.agent.core.skills.integration;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Per-integration effect of applying a skill lifecycle refresh request.
 */
public record SkillIntegrationRefreshImpact(
        String integrationKey,
        List<String> refreshed,
        List<String> removed,
        List<String> skipped) {

    public SkillIntegrationRefreshImpact {
        if (integrationKey == null || integrationKey.isBlank()) {
            throw new IllegalArgumentException("Integration key must not be blank");
        }
        integrationKey = integrationKey.trim();
        refreshed = copyNames(refreshed);
        removed = copyNames(removed);
        skipped = copyNames(skipped);
    }

    public static SkillIntegrationRefreshImpact of(
            String integrationKey,
            List<String> refreshed,
            List<String> removed,
            List<String> skipped) {
        return new SkillIntegrationRefreshImpact(integrationKey, refreshed, removed, skipped);
    }

    public static SkillIntegrationRefreshImpact skipped(String integrationKey, List<String> skillIds) {
        return new SkillIntegrationRefreshImpact(integrationKey, List.of(), List.of(), skillIds);
    }

    public boolean hasWork() {
        return !refreshed.isEmpty() || !removed.isEmpty();
    }

    public boolean hasSkipped() {
        return !skipped.isEmpty();
    }

    private static List<String> copyNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }
}
