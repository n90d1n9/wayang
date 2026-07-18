package tech.kayys.wayang.agent.core.skills.integration;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Result of synchronizing filesystem manifests into the runtime skill registry.
 */
public record SkillManifestRegistrySyncResult(
        List<String> registered,
        List<String> removed,
        List<String> skipped) {

    public SkillManifestRegistrySyncResult {
        registered = copyNames(registered);
        removed = copyNames(removed);
        skipped = copyNames(skipped);
    }

    public static SkillManifestRegistrySyncResult empty() {
        return new SkillManifestRegistrySyncResult(List.of(), List.of(), List.of());
    }

    public boolean hasWork() {
        return !registered.isEmpty() || !removed.isEmpty();
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
