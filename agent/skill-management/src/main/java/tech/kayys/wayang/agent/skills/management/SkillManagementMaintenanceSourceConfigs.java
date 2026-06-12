package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Parses optional source stores for configured skill-management maintenance.
 */
public final class SkillManagementMaintenanceSourceConfigs {

    public static final String PREFIX = "wayang.skills.maintenance.source.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_MAINTENANCE_SOURCE_";

    private SkillManagementMaintenanceSourceConfigs() {
    }

    public static SkillManagementMaintenanceSourceConfig fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillManagementMaintenanceSourceConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillManagementMaintenanceSourceConfig fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillManagementMaintenanceSourceConfig fromProperties(Properties properties, String prefix) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties), prefix);
    }

    public static SkillStoreConfigValidationResult validateProperties(Properties properties) {
        return validate(() -> fromProperties(properties));
    }

    public static SkillManagementMaintenanceSourceConfig fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillManagementMaintenanceSourceConfig fromMap(Map<String, ?> values, String prefix) {
        return parse(
                SkillStoreConfigValues.flattenAndNormalize(values),
                SkillStoreConfigValues.normalizePrefix(prefix));
    }

    public static SkillStoreConfigValidationResult validateMap(Map<String, ?> values) {
        return validate(() -> fromMap(values));
    }

    public static SkillManagementMaintenanceSourceConfig fromEnvironment(Map<String, String> environment) {
        return fromMap(SkillStoreConfigValues.fromEnvironment(environment, ENV_PREFIX, PREFIX));
    }

    public static SkillStoreConfigValidationResult validateEnvironment(Map<String, String> environment) {
        return validate(() -> fromEnvironment(environment));
    }

    private static SkillManagementMaintenanceSourceConfig parse(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        String definitionStorePrefix = definitionStoreChildPrefix(scoped);
        String artifactStorePrefix = artifactStoreChildPrefix(scoped);
        return SkillManagementMaintenanceSourceConfig.of(
                definitionStorePrefix == null
                        ? null
                        : SkillDefinitionStoreConfigs.fromMap(values, prefix + definitionStorePrefix),
                artifactStorePrefix == null
                        ? null
                        : SkillArtifactStoreConfigs.fromMap(values, prefix + artifactStorePrefix));
    }

    private static String definitionStoreChildPrefix(SkillStoreConfigValues.ScopedValues scoped) {
        if (scoped.hasChild("definitions.store")) {
            return "definitions.store.";
        }
        if (scoped.hasChild("definition.store")) {
            return "definition.store.";
        }
        if (scoped.hasChild("skills.store")) {
            return "skills.store.";
        }
        if (scoped.hasChild("skill.store")) {
            return "skill.store.";
        }
        if (scoped.hasChild("store")) {
            return "store.";
        }
        return null;
    }

    private static String artifactStoreChildPrefix(SkillStoreConfigValues.ScopedValues scoped) {
        if (scoped.hasChild("artifacts.store")) {
            return "artifacts.store.";
        }
        if (scoped.hasChild("artifact.store")) {
            return "artifact.store.";
        }
        return null;
    }

    private static SkillStoreConfigValidationResult validate(
            Supplier<SkillManagementMaintenanceSourceConfig> parser) {
        try {
            return parser.get().validate();
        } catch (IllegalArgumentException error) {
            return SkillStoreConfigValidationResult.error(error.getMessage());
        }
    }
}
