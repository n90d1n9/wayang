package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable lifecycle state store configuration.
 */
public final class SkillLifecycleStateStoreConfigs {

    public static final String PREFIX = "wayang.skills.lifecycle.store.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_LIFECYCLE_STORE_";

    private SkillLifecycleStateStoreConfigs() {
    }

    public static SkillLifecycleStateStoreConfig fromSystemProperties() {
        return SkillStoreConfigParsing.fromSystemProperties(PREFIX, SkillLifecycleStateStoreConfigs::parse);
    }

    public static SkillLifecycleStateStoreConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillLifecycleStateStoreConfig fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillLifecycleStateStoreConfig fromProperties(Properties properties, String prefix) {
        return SkillStoreConfigParsing.fromProperties(properties, prefix, SkillLifecycleStateStoreConfigs::parse);
    }

    public static SkillLifecycleStateStoreConfig fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillLifecycleStateStoreConfig fromMap(Map<String, ?> values, String prefix) {
        return SkillStoreConfigParsing.fromMap(values, prefix, SkillLifecycleStateStoreConfigs::parse);
    }

    public static SkillLifecycleStateStoreConfig fromEnvironment(Map<String, String> environment) {
        return SkillStoreConfigParsing.fromEnvironment(
                environment,
                ENV_PREFIX,
                PREFIX,
                SkillLifecycleStateStoreConfigs::parse);
    }

    private static SkillLifecycleStateStoreConfig parse(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillLifecycleStateStoreConfig.Kind kind = kind(SkillStoreConfigKeys.storeKind(scoped, "memory"));
        return switch (kind) {
            case MEMORY -> SkillLifecycleStateStoreConfig.memory();
            case FILESYSTEM -> SkillLifecycleStateStoreConfig.fileSystem(Path.of(SkillStoreConfigKeys.directory(
                    scoped,
                    "Filesystem lifecycle store requires a directory")));
            case OBJECT_STORAGE -> SkillLifecycleStateStoreConfig.objectStorage(
                    SkillStoreConfigKeys.objectPrefix(scoped, ObjectStorageSkillLifecycleStateStore.DEFAULT_PREFIX));
            case JDBC -> SkillLifecycleStateStoreConfig.jdbc(
                    SkillStoreConfigKeys.jdbcTableName(scoped, JdbcSkillLifecycleStateStore.DEFAULT_TABLE_NAME),
                    SkillStoreConfigKeys.initializeJdbcSchema(scoped));
            case CUSTOM -> SkillLifecycleStateStoreConfig.custom(SkillStoreConfigKeys.customStoreName(
                    scoped,
                    "Custom lifecycle store requires a store name"));
            case HYBRID -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillLifecycleStateStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Hybrid lifecycle store requires primary and fallback config groups",
                                SkillLifecycleStateStoreConfigs::parse);
                yield SkillLifecycleStateStoreConfig.hybrid(children.primary(), children.fallback());
            }
            case MIRRORED -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillLifecycleStateStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Mirrored lifecycle store requires primary and fallback config groups",
                                SkillLifecycleStateStoreConfigs::parse);
                yield SkillLifecycleStateStoreConfig.mirrored(children.primary(), children.fallback());
            }
        };
    }

    private static SkillLifecycleStateStoreConfig.Kind kind(String value) {
        if (SkillStoreConfigKindAliases.isLifecycleMemory(value)) {
            return SkillLifecycleStateStoreConfig.Kind.MEMORY;
        }
        if (SkillStoreConfigKindAliases.isFilesystem(value)) {
            return SkillLifecycleStateStoreConfig.Kind.FILESYSTEM;
        }
        if (SkillStoreConfigKindAliases.isObjectStorage(value)) {
            return SkillLifecycleStateStoreConfig.Kind.OBJECT_STORAGE;
        }
        if (SkillStoreConfigKindAliases.isJdbc(value)) {
            return SkillLifecycleStateStoreConfig.Kind.JDBC;
        }
        if (SkillStoreConfigKindAliases.isCustom(value)) {
            return SkillLifecycleStateStoreConfig.Kind.CUSTOM;
        }
        if (SkillStoreConfigKindAliases.isHybrid(value)) {
            return SkillLifecycleStateStoreConfig.Kind.HYBRID;
        }
        if (SkillStoreConfigKindAliases.isMirrored(value)) {
            return SkillLifecycleStateStoreConfig.Kind.MIRRORED;
        }
        throw new IllegalArgumentException("Unknown lifecycle store kind: " + value);
    }
}
