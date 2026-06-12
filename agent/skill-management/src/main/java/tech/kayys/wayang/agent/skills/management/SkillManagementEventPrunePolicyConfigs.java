package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable skill-management event-history prune policy.
 */
public final class SkillManagementEventPrunePolicyConfigs {

    public static final String PREFIX = "wayang.skills.events.prune.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_EVENTS_PRUNE_";

    private SkillManagementEventPrunePolicyConfigs() {
    }

    public static SkillManagementEventPrunePolicy fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillManagementEventPrunePolicy fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillManagementEventPrunePolicy fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillManagementEventPrunePolicy fromProperties(Properties properties, String prefix) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix);
    }

    public static SkillManagementEventPrunePolicy fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillManagementEventPrunePolicy fromMap(Map<String, ?> values, String prefix) {
        return fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix),
                SkillManagementEventPrunePolicy.disabled());
    }

    public static SkillManagementEventPrunePolicy fromEnvironment(Map<String, String> environment) {
        return fromMap(SkillStoreConfigValues.fromEnvironment(environment, ENV_PREFIX, PREFIX));
    }

    static SkillManagementEventPrunePolicy fromNormalizedMap(
            Map<String, String> values,
            String prefix,
            SkillManagementEventPrunePolicy defaults) {
        String resolvedPrefix = SkillStoreConfigValues.normalizePrefix(prefix);
        SkillManagementEventPrunePolicy resolvedDefaults = defaults == null
                ? SkillManagementEventPrunePolicy.disabled()
                : defaults;
        if (!hasConfig(values, resolvedPrefix)) {
            return resolvedDefaults;
        }

        SkillStoreConfigValues.ScopedValues scoped =
                new SkillStoreConfigValues.ScopedValues(values, resolvedPrefix);
        boolean enabled = resolvedDefaults.enabled();
        boolean dryRun = resolvedDefaults.options().dryRun();
        if (SkillStoreConfigKeys.hasMode(scoped)) {
            String mode = SkillStoreConfigKeys.mode(scoped, "");
            String normalized = SkillStoreConfigValues.normalize(mode);
            switch (normalized) {
                case "", "keep", "keeplatest", "prune", "compact", "enabled", "on" -> enabled = true;
                case "dryrun", "preview", "plan", "check" -> {
                    enabled = true;
                    dryRun = true;
                }
                case "none", "skip", "off", "disabled" -> enabled = false;
                default -> throw new IllegalArgumentException(
                        "Unknown skill-management event prune mode: " + mode);
            }
        }

        enabled = scoped.get(
                        "enabled",
                        "enable",
                        "active",
                        "pruneEvents",
                        "prune-events",
                        "compactEvents",
                        "compact-events",
                        "eventPrune",
                        "event-prune",
                        "eventHistoryPrune",
                        "event-history-prune")
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(enabled);
        int keepLatestEvents = SkillStoreConfigKeys.keepLatestEvents(
                scoped,
                resolvedDefaults.options().keepLatestEvents(),
                "Invalid skill-management event prune retention");
        dryRun = SkillStoreConfigKeys.dryRun(scoped, dryRun);

        if (!enabled) {
            return SkillManagementEventPrunePolicy.disabled();
        }
        return dryRun
                ? SkillManagementEventPrunePolicy.dryRun(keepLatestEvents)
                : SkillManagementEventPrunePolicy.keepLatest(keepLatestEvents);
    }

    static boolean hasConfig(Map<String, String> values, String prefix) {
        String normalizedPrefix = SkillStoreConfigValues.normalizePrefix(prefix);
        String normalized = SkillStoreConfigValues.normalize(normalizedPrefix);
        return values.keySet().stream().anyMatch(key -> key.startsWith(normalized));
    }
}
