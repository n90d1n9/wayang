package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Skill lifecycle delta consumed by integration refresh hooks.
 */
public record SkillIntegrationRefreshRequest(
        List<String> added,
        List<String> updated,
        List<String> removed) {

    public SkillIntegrationRefreshRequest {
        added = copyNames(added);
        updated = copyNames(updated);
        removed = copyNames(removed);
    }

    public static SkillIntegrationRefreshRequest empty() {
        return new SkillIntegrationRefreshRequest(List.of(), List.of(), List.of());
    }

    public static SkillIntegrationRefreshRequest from(SkillManifestCatalogChange change) {
        if (change == null) {
            return empty();
        }
        return new SkillIntegrationRefreshRequest(change.added(), change.updated(), change.removed());
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !updated.isEmpty() || !removed.isEmpty();
    }

    public List<String> upsertedNames() {
        List<String> names = new ArrayList<>();
        names.addAll(added);
        names.addAll(updated);
        return List.copyOf(names);
    }

    public List<String> changedNames() {
        List<String> names = new ArrayList<>(upsertedNames());
        names.addAll(removed);
        return List.copyOf(names);
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
