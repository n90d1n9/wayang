package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable artifact-store synchronization policy.
 */
public final class SkillArtifactStoreSyncConfigs {

    public static final String PREFIX = "wayang.skills.artifacts.sync.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_ARTIFACTS_SYNC_";

    private SkillArtifactStoreSyncConfigs() {
    }

    public static SkillArtifactStoreSyncOptions fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillArtifactStoreSyncOptions fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillArtifactStoreSyncOptions fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillArtifactStoreSyncOptions fromProperties(Properties properties, String prefix) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix);
    }

    public static SkillArtifactStoreSyncOptions fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillArtifactStoreSyncOptions fromMap(Map<String, ?> values, String prefix) {
        return fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix),
                SkillArtifactStoreSyncOptions.bootstrap());
    }

    public static SkillArtifactStoreSyncOptions fromEnvironment(Map<String, String> environment) {
        return fromMap(SkillStoreConfigValues.fromEnvironment(environment, ENV_PREFIX, PREFIX));
    }

    static SkillArtifactStoreSyncOptions fromNormalizedMap(
            Map<String, String> values,
            String prefix,
            SkillArtifactStoreSyncOptions defaults) {
        return fromPolicy(SkillStoreSyncConfigSupport.fromNormalizedMap(
                values,
                prefix,
                toPolicy(defaults),
                SkillStoreSyncConfigSupport.artifactProfile()));
    }

    private static SkillStoreSyncPolicy toPolicy(SkillArtifactStoreSyncOptions options) {
        return options == null ? null : new SkillStoreSyncPolicy(
                options.overwriteExisting(),
                options.deleteMissingFromTarget(),
                options.dryRun());
    }

    private static SkillArtifactStoreSyncOptions fromPolicy(SkillStoreSyncPolicy policy) {
        return new SkillArtifactStoreSyncOptions(
                policy.overwriteExisting(),
                policy.deleteMissingFromTarget(),
                policy.dryRun());
    }
}
