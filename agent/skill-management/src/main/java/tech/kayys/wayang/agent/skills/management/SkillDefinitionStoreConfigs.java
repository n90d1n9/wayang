package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable skill store configuration into the internal store config model.
 */
public final class SkillDefinitionStoreConfigs {

    public static final String PREFIX = "wayang.skills.store.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_STORE_";

    private SkillDefinitionStoreConfigs() {
    }

    public static SkillDefinitionStoreConfig fromSystemProperties() {
        return SkillStoreConfigParsing.fromSystemProperties(PREFIX, SkillDefinitionStoreConfigs::parse);
    }

    public static SkillDefinitionStoreConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillDefinitionStoreConfig fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillDefinitionStoreConfig fromProperties(Properties properties, String prefix) {
        return SkillStoreConfigParsing.fromProperties(properties, prefix, SkillDefinitionStoreConfigs::parse);
    }

    public static SkillDefinitionStoreConfig fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillDefinitionStoreConfig fromMap(Map<String, ?> values, String prefix) {
        return SkillStoreConfigParsing.fromMap(values, prefix, SkillDefinitionStoreConfigs::parse);
    }

    public static SkillDefinitionStoreConfig fromEnvironment(Map<String, String> environment) {
        return SkillStoreConfigParsing.fromEnvironment(
                environment,
                ENV_PREFIX,
                PREFIX,
                SkillDefinitionStoreConfigs::parse);
    }

    private static SkillDefinitionStoreConfig parse(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillDefinitionStoreConfig.Kind kind = kind(SkillStoreConfigKeys.storeKind(scoped, "registry"));
        return switch (kind) {
            case REGISTRY -> SkillDefinitionStoreConfig.registry();
            case FILESYSTEM -> SkillDefinitionStoreConfig.fileSystem(Path.of(SkillStoreConfigKeys.directory(
                    scoped,
                    "Filesystem skill store requires a directory")));
            case OBJECT_STORAGE -> SkillDefinitionStoreConfig.objectStorage(
                    SkillStoreConfigKeys.objectPrefix(scoped, null));
            case JDBC -> SkillDefinitionStoreConfig.jdbc(
                    SkillStoreConfigKeys.jdbcTableName(scoped, JdbcSkillDefinitionStore.DEFAULT_TABLE_NAME),
                    SkillStoreConfigKeys.initializeJdbcSchema(scoped));
            case CUSTOM -> SkillDefinitionStoreConfig.custom(SkillStoreConfigKeys.customStoreName(
                    scoped,
                    "Custom skill store requires a store name"));
            case HYBRID -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillDefinitionStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Hybrid skill store requires primary and fallback config groups",
                                SkillDefinitionStoreConfigs::parse);
                yield SkillDefinitionStoreConfig.hybrid(children.primary(), children.fallback());
            }
            case MIRRORED -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillDefinitionStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Mirrored skill store requires primary and fallback config groups",
                                SkillDefinitionStoreConfigs::parse);
                yield SkillDefinitionStoreConfig.mirrored(children.primary(), children.fallback());
            }
        };
    }

    private static SkillDefinitionStoreConfig.Kind kind(String value) {
        if (SkillStoreConfigKindAliases.isDefinitionRegistry(value)) {
            return SkillDefinitionStoreConfig.Kind.REGISTRY;
        }
        if (SkillStoreConfigKindAliases.isFilesystem(value)) {
            return SkillDefinitionStoreConfig.Kind.FILESYSTEM;
        }
        if (SkillStoreConfigKindAliases.isObjectStorage(value)) {
            return SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE;
        }
        if (SkillStoreConfigKindAliases.isJdbc(value)) {
            return SkillDefinitionStoreConfig.Kind.JDBC;
        }
        if (SkillStoreConfigKindAliases.isCustom(value)) {
            return SkillDefinitionStoreConfig.Kind.CUSTOM;
        }
        if (SkillStoreConfigKindAliases.isHybrid(value)) {
            return SkillDefinitionStoreConfig.Kind.HYBRID;
        }
        if (SkillStoreConfigKindAliases.isMirrored(value)) {
            return SkillDefinitionStoreConfig.Kind.MIRRORED;
        }
        throw new IllegalArgumentException("Unknown skill store kind: " + value);
    }
}
