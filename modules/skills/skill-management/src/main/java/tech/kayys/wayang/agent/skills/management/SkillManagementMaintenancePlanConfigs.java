package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Parses deployable skill-management maintenance workflow policy.
 */
public final class SkillManagementMaintenancePlanConfigs {

    public static final String PREFIX = "wayang.skills.maintenance.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_MAINTENANCE_";

    private SkillManagementMaintenancePlanConfigs() {
    }

    public static SkillManagementMaintenancePlan fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillManagementMaintenancePlan fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillManagementMaintenancePlan fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillManagementMaintenancePlan fromProperties(Properties properties, String prefix) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix);
    }

    public static SkillStoreConfigValidationResult validateProperties(Properties properties) {
        return validate(() -> fromProperties(properties));
    }

    public static SkillManagementMaintenancePlan fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillManagementMaintenancePlan fromMap(Map<String, ?> values, String prefix) {
        return parse(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix));
    }

    public static SkillStoreConfigValidationResult validateMap(Map<String, ?> values) {
        return validate(() -> fromMap(values));
    }

    public static SkillManagementMaintenancePlan fromEnvironment(Map<String, String> environment) {
        return fromMap(SkillStoreConfigValues.fromEnvironment(environment, ENV_PREFIX, PREFIX));
    }

    public static SkillStoreConfigValidationResult validateEnvironment(Map<String, String> environment) {
        return validate(() -> fromEnvironment(environment));
    }

    private static SkillManagementMaintenancePlan parse(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillManagementMaintenancePlan fromMode = mode(SkillStoreConfigKeys.mode(scoped, "bootstrap"));
        SkillDefinitionStoreSyncOptions syncOptions = scoped.hasChild("definition.sync")
                ? definitionSyncOptions(values, prefix + "definition.sync.", fromMode.definitionSyncOptions())
                : fromMode.definitionSyncOptions();
        SkillArtifactStoreSyncOptions artifactSyncOptions = artifactSyncOptions(
                values,
                prefix,
                fromMode.artifactSyncOptions());
        SkillLifecycleStateReconcileOptions reconcileOptions = scoped.hasChild("lifecycle.reconcile")
                ? lifecycleReconcileOptions(
                        values,
                        prefix + "lifecycle.reconcile.",
                        fromMode.lifecycleStateReconcileOptions())
                : fromMode.lifecycleStateReconcileOptions();
        SkillManagementEventPrunePolicy eventPrunePolicy = eventPrunePolicy(
                values,
                prefix,
                fromMode.eventPrunePolicy());
        SkillManagementMaintenancePlan plan =
                new SkillManagementMaintenancePlan(syncOptions, artifactSyncOptions, reconcileOptions,
                        eventPrunePolicy);
        return SkillStoreConfigKeys.dryRun(scoped, false)
                ? plan.asDryRun()
                : plan;
    }

    private static SkillDefinitionStoreSyncOptions definitionSyncOptions(
            Map<String, String> values,
            String prefix,
            SkillDefinitionStoreSyncOptions defaults) {
        return SkillDefinitionStoreSyncConfigs.fromNormalizedMap(values, prefix, defaults);
    }

    private static SkillArtifactStoreSyncOptions artifactSyncOptions(
            Map<String, String> values,
            String prefix,
            SkillArtifactStoreSyncOptions defaults) {
        String childPrefix = artifactSyncChildPrefix(new SkillStoreConfigValues.ScopedValues(values, prefix));
        return childPrefix == null
                ? defaults
                : SkillArtifactStoreSyncConfigs.fromNormalizedMap(values, prefix + childPrefix, defaults);
    }

    private static SkillLifecycleStateReconcileOptions lifecycleReconcileOptions(
            Map<String, String> values,
            String prefix,
            SkillLifecycleStateReconcileOptions defaults) {
        return SkillLifecycleStateReconcileConfigs.fromNormalizedMap(values, prefix, defaults);
    }

    private static SkillManagementEventPrunePolicy eventPrunePolicy(
            Map<String, String> values,
            String prefix,
            SkillManagementEventPrunePolicy defaults) {
        SkillStoreConfigValues.ScopedValues root = new SkillStoreConfigValues.ScopedValues(values, prefix);
        boolean hasRootEventPrunePolicy = root.get(
                "pruneEvents",
                "prune-events",
                "compactEvents",
                "compact-events",
                "eventPrune",
                "event-prune",
                "eventHistoryPrune",
                "event-history-prune").isPresent();
        String childPrefix = eventPruneChildPrefix(root);
        if (!hasRootEventPrunePolicy && childPrefix == null) {
            return defaults;
        }
        if (childPrefix == null) {
            boolean enabled = root.get(
                            "pruneEvents",
                            "prune-events",
                            "compactEvents",
                            "compact-events",
                            "eventPrune",
                            "event-prune",
                            "eventHistoryPrune",
                            "event-history-prune")
                    .map(SkillStoreConfigValues::booleanValue)
                    .orElse(defaults.enabled());
            return enabled
                    ? new SkillManagementEventPrunePolicy(true, defaults.options())
                    : SkillManagementEventPrunePolicy.disabled();
        }
        String eventPrunePrefix = prefix + childPrefix;
        SkillManagementEventPrunePolicy baseDefaults =
                new SkillManagementEventPrunePolicy(true, defaults.options());
        return SkillManagementEventPrunePolicyConfigs.fromNormalizedMap(
                values,
                eventPrunePrefix,
                baseDefaults);
    }

    private static String eventPruneChildPrefix(SkillStoreConfigValues.ScopedValues scoped) {
        if (scoped.hasChild("events.prune")) {
            return "events.prune.";
        }
        if (scoped.hasChild("event.prune")) {
            return "event.prune.";
        }
        if (scoped.hasChild("eventHistory.prune")) {
            return "eventHistory.prune.";
        }
        if (scoped.hasChild("event-history.prune")) {
            return "event-history.prune.";
        }
        return null;
    }

    private static String artifactSyncChildPrefix(SkillStoreConfigValues.ScopedValues scoped) {
        if (scoped.hasChild("artifacts.sync")) {
            return "artifacts.sync.";
        }
        if (scoped.hasChild("artifact.sync")) {
            return "artifact.sync.";
        }
        return null;
    }

    private static SkillManagementMaintenancePlan mode(String value) {
        String normalized = SkillStoreConfigValues.normalize(value);
        return switch (normalized) {
            case "bootstrap", "copy", "seed", "missingonly" -> SkillManagementMaintenancePlan.bootstrap();
            case "mirror", "sync", "repair", "full" -> SkillManagementMaintenancePlan.mirrorAndRepair();
            case "inspect", "check", "dryrun", "preview", "plan" -> SkillManagementMaintenancePlan.inspectOnly();
            default -> throw new IllegalArgumentException("Unknown skill maintenance mode: " + value);
        };
    }

    private static SkillStoreConfigValidationResult validate(Supplier<SkillManagementMaintenancePlan> parser) {
        try {
            parser.get();
            return SkillStoreConfigValidationResult.valid();
        } catch (IllegalArgumentException error) {
            return SkillStoreConfigValidationResult.error(error.getMessage());
        }
    }
}
