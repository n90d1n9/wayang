package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable lifecycle state reconciliation policy.
 */
public final class SkillLifecycleStateReconcileConfigs {

    public static final String PREFIX = "wayang.skills.lifecycle.reconcile.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_LIFECYCLE_RECONCILE_";

    private SkillLifecycleStateReconcileConfigs() {
    }

    public static SkillLifecycleStateReconcileOptions fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillLifecycleStateReconcileOptions fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillLifecycleStateReconcileOptions fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillLifecycleStateReconcileOptions fromProperties(Properties properties, String prefix) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix);
    }

    public static SkillLifecycleStateReconcileOptions fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillLifecycleStateReconcileOptions fromMap(Map<String, ?> values, String prefix) {
        return fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    public static SkillLifecycleStateReconcileOptions fromEnvironment(Map<String, String> environment) {
        return fromMap(SkillStoreConfigValues.fromEnvironment(environment, ENV_PREFIX, PREFIX));
    }

    static SkillLifecycleStateReconcileOptions fromNormalizedMap(
            Map<String, String> values,
            String prefix,
            SkillLifecycleStateReconcileOptions defaults) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillLifecycleStateReconcileOptions resolvedDefaults = defaults == null
                ? SkillLifecycleStateReconcileOptions.inspectOnly()
                : defaults;
        SkillLifecycleStateReconcileOptions fromMode = SkillStoreConfigKeys.hasMode(scoped)
                ? mode(SkillStoreConfigKeys.mode(scoped, "inspect-only"))
                : resolvedDefaults;
        boolean createMissing = scoped.get(
                        "createMissingStates",
                        "create-missing-states",
                        "createMissing",
                        "create-missing",
                        "initializeMissing",
                        "initialize-missing")
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(fromMode.createMissingStates());
        boolean removeOrphans = scoped.get(
                        "removeOrphanedStates",
                        "remove-orphaned-states",
                        "removeOrphans",
                        "remove-orphans",
                        "pruneOrphans",
                        "prune-orphans")
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(fromMode.removeOrphanedStates());
        return new SkillLifecycleStateReconcileOptions(createMissing, removeOrphans);
    }

    private static SkillLifecycleStateReconcileOptions mode(String value) {
        String normalized = SkillStoreConfigValues.normalize(value);
        return switch (normalized) {
            case "inspectonly", "inspect", "check", "dryrun", "none", "disabled", "off" ->
                    SkillLifecycleStateReconcileOptions.inspectOnly();
            case "createmissing", "initializemissing", "bootstrap", "initialize", "create" ->
                    SkillLifecycleStateReconcileOptions.createMissing();
            case "sync", "repair", "prune", "full", "createmissingandremoveorphans",
                    "initializemissingandremoveorphans" ->
                    SkillLifecycleStateReconcileOptions.createMissingAndRemoveOrphans();
            default -> throw new IllegalArgumentException("Unknown lifecycle reconcile mode: " + value);
        };
    }
}
