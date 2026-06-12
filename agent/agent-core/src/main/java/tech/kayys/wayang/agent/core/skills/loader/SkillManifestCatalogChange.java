package tech.kayys.wayang.agent.core.skills.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Diff between two loaded skill manifest snapshots.
 */
public record SkillManifestCatalogChange(
        SkillManifestSnapshot before,
        SkillManifestSnapshot after,
        List<String> added,
        List<String> removed,
        List<String> updated) {

    public SkillManifestCatalogChange {
        before = before == null ? SkillManifestSnapshot.empty() : before;
        after = after == null ? SkillManifestSnapshot.empty() : after;
        added = immutableList(added);
        removed = immutableList(removed);
        updated = immutableList(updated);
    }

    public static SkillManifestCatalogChange between(
            SkillManifestSnapshot before,
            SkillManifestSnapshot after) {
        SkillManifestSnapshot safeBefore = before == null ? SkillManifestSnapshot.empty() : before;
        SkillManifestSnapshot safeAfter = after == null ? SkillManifestSnapshot.empty() : after;

        List<String> added = safeAfter.names().stream()
                .filter(name -> !safeBefore.contains(name))
                .toList();
        List<String> removed = safeBefore.names().stream()
                .filter(name -> !safeAfter.contains(name))
                .toList();
        List<String> updated = safeAfter.names().stream()
                .filter(safeBefore::contains)
                .filter(name -> !Objects.equals(
                        safeBefore.fingerprint(name).orElse(""),
                        safeAfter.fingerprint(name).orElse("")))
                .toList();

        return new SkillManifestCatalogChange(safeBefore, safeAfter, added, removed, updated);
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !updated.isEmpty();
    }

    public List<String> changedNames() {
        List<String> names = new ArrayList<>();
        names.addAll(added);
        names.addAll(updated);
        names.addAll(removed);
        return List.copyOf(names);
    }

    private static List<String> immutableList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
