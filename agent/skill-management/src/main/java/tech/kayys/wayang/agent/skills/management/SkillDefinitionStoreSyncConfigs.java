package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable definition-store synchronization policy.
 */
public final class SkillDefinitionStoreSyncConfigs {

    public static final String PREFIX = "wayang.skills.definition.sync.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_DEFINITION_SYNC_";

    private SkillDefinitionStoreSyncConfigs() {
    }

    public static SkillDefinitionStoreSyncOptions fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillDefinitionStoreSyncOptions fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillDefinitionStoreSyncOptions fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillDefinitionStoreSyncOptions fromProperties(Properties properties, String prefix) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix);
    }

    public static SkillDefinitionStoreSyncOptions fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillDefinitionStoreSyncOptions fromMap(Map<String, ?> values, String prefix) {
        return fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix),
                SkillDefinitionStoreSyncOptions.bootstrap());
    }

    public static SkillDefinitionStoreSyncOptions fromEnvironment(Map<String, String> environment) {
        return fromMap(SkillStoreConfigValues.fromEnvironment(environment, ENV_PREFIX, PREFIX));
    }

    static SkillDefinitionStoreSyncOptions fromNormalizedMap(
            Map<String, String> values,
            String prefix,
            SkillDefinitionStoreSyncOptions defaults) {
        return fromPolicy(SkillStoreSyncConfigSupport.fromNormalizedMap(
                values,
                prefix,
                toPolicy(defaults),
                SkillStoreSyncConfigSupport.definitionProfile()));
    }

    private static SkillStoreSyncPolicy toPolicy(SkillDefinitionStoreSyncOptions options) {
        return options == null ? null : new SkillStoreSyncPolicy(
                options.overwriteExisting(),
                options.deleteMissingFromTarget(),
                options.dryRun());
    }

    private static SkillDefinitionStoreSyncOptions fromPolicy(SkillStoreSyncPolicy policy) {
        return new SkillDefinitionStoreSyncOptions(
                policy.overwriteExisting(),
                policy.deleteMissingFromTarget(),
                policy.dryRun());
    }
}
