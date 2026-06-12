package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared parser for definition/artifact store synchronization policy.
 */
final class SkillStoreSyncConfigSupport {

    private static final List<String> OVERWRITE_KEYS = List.of(
            "overwriteExisting",
            "overwrite-existing",
            "overwrite");
    private static final List<String> DELETE_MISSING_KEYS = List.of(
            "deleteMissingFromTarget",
            "delete-missing-from-target",
            "deleteMissing",
            "delete-missing");

    private SkillStoreSyncConfigSupport() {
    }

    static SkillStoreSyncProfile definitionProfile() {
        return new SkillStoreSyncProfile(
                "definition",
                List.of("updateExisting", "update-existing"),
                List.of("pruneOrphans", "prune-orphans"),
                List.of("inspect", "check", "dryrun", "preview", "plan"));
    }

    static SkillStoreSyncProfile artifactProfile() {
        return new SkillStoreSyncProfile(
                "artifact",
                List.of("replaceExisting", "replace-existing"),
                List.of("prune"),
                List.of("dryrun", "preview", "plan", "inspect"));
    }

    static SkillStoreSyncPolicy fromNormalizedMap(
            Map<String, String> values,
            String prefix,
            SkillStoreSyncPolicy defaults,
            SkillStoreSyncProfile profile) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillStoreSyncPolicy resolvedDefaults = defaults == null
                ? SkillStoreSyncPolicy.bootstrap()
                : defaults;
        SkillStoreSyncPolicy fromMode = SkillStoreConfigKeys.hasMode(scoped)
                ? mode(SkillStoreConfigKeys.mode(scoped, "bootstrap"), profile)
                : resolvedDefaults;
        boolean overwriteExisting = scoped.get(keyAliases(OVERWRITE_KEYS, profile.overwriteAliases()))
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(fromMode.overwriteExisting());
        boolean deleteMissingFromTarget = scoped.get(keyAliases(DELETE_MISSING_KEYS, profile.deleteAliases()))
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(fromMode.deleteMissingFromTarget());
        boolean dryRun = SkillStoreConfigKeys.dryRun(scoped, fromMode.dryRun());
        return new SkillStoreSyncPolicy(overwriteExisting, deleteMissingFromTarget, dryRun);
    }

    private static SkillStoreSyncPolicy mode(String value, SkillStoreSyncProfile profile) {
        String normalized = SkillStoreConfigValues.normalize(value);
        return switch (normalized) {
            case "bootstrap", "copy", "seed", "missingonly", "createmissing" ->
                    SkillStoreSyncPolicy.bootstrap();
            case "mirror", "sync", "repair", "full" -> SkillStoreSyncPolicy.mirror();
            default -> {
                if (profile.dryRunModeAliases().contains(normalized)) {
                    yield SkillStoreSyncPolicy.mirror().asDryRun();
                }
                throw new IllegalArgumentException("Unknown " + profile.name() + " sync mode: " + value);
            }
        };
    }

    private static String[] keyAliases(List<String> shared, List<String> specific) {
        List<String> aliases = new ArrayList<>(shared);
        aliases.addAll(specific);
        return aliases.toArray(String[]::new);
    }

    record SkillStoreSyncProfile(
            String name,
            List<String> overwriteAliases,
            List<String> deleteAliases,
            List<String> dryRunModeAliases) {
    }
}
