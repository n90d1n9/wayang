package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable artifact store configuration.
 */
public final class SkillArtifactStoreConfigs {

    public static final String PREFIX = "wayang.skills.artifacts.store.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_ARTIFACTS_STORE_";

    private SkillArtifactStoreConfigs() {
    }

    public static SkillArtifactStoreConfig fromSystemProperties() {
        return SkillStoreConfigParsing.fromSystemProperties(PREFIX, SkillArtifactStoreConfigs::parse);
    }

    public static SkillArtifactStoreConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillArtifactStoreConfig fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillArtifactStoreConfig fromProperties(Properties properties, String prefix) {
        return SkillStoreConfigParsing.fromProperties(properties, prefix, SkillArtifactStoreConfigs::parse);
    }

    public static SkillArtifactStoreConfig fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillArtifactStoreConfig fromMap(Map<String, ?> values, String prefix) {
        return SkillStoreConfigParsing.fromMap(values, prefix, SkillArtifactStoreConfigs::parse);
    }

    public static SkillArtifactStoreConfig fromEnvironment(Map<String, String> environment) {
        return SkillStoreConfigParsing.fromEnvironment(
                environment,
                ENV_PREFIX,
                PREFIX,
                SkillArtifactStoreConfigs::parse);
    }

    private static SkillArtifactStoreConfig parse(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillArtifactStoreConfig.Kind kind = kind(SkillStoreConfigKeys.storeKind(scoped, "memory"));
        return switch (kind) {
            case MEMORY -> SkillArtifactStoreConfig.memory();
            case FILESYSTEM -> SkillArtifactStoreConfig.fileSystem(Path.of(SkillStoreConfigKeys.directory(
                    scoped,
                    "Filesystem artifact store requires a directory")));
            case OBJECT_STORAGE -> SkillArtifactStoreConfig.objectStorage(
                    SkillStoreConfigKeys.objectPrefix(scoped, ObjectStorageSkillArtifactStore.DEFAULT_PREFIX));
            case JDBC -> SkillArtifactStoreConfig.jdbc(
                    SkillStoreConfigKeys.jdbcTableName(scoped, JdbcSkillArtifactStore.DEFAULT_TABLE_NAME),
                    SkillStoreConfigKeys.initializeJdbcSchema(scoped));
            case CUSTOM -> SkillArtifactStoreConfig.custom(SkillStoreConfigKeys.customStoreName(
                    scoped,
                    "Custom artifact store requires a store name"));
            case HYBRID -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillArtifactStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Hybrid artifact store requires primary and fallback config groups",
                                SkillArtifactStoreConfigs::parse);
                yield SkillArtifactStoreConfig.hybrid(children.primary(), children.fallback());
            }
            case MIRRORED -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillArtifactStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Mirrored artifact store requires primary and fallback config groups",
                                SkillArtifactStoreConfigs::parse);
                yield SkillArtifactStoreConfig.mirrored(children.primary(), children.fallback());
            }
        };
    }

    private static SkillArtifactStoreConfig.Kind kind(String value) {
        if (SkillStoreConfigKindAliases.isArtifactMemory(value)) {
            return SkillArtifactStoreConfig.Kind.MEMORY;
        }
        if (SkillStoreConfigKindAliases.isFilesystem(value)) {
            return SkillArtifactStoreConfig.Kind.FILESYSTEM;
        }
        if (SkillStoreConfigKindAliases.isObjectStorage(value)) {
            return SkillArtifactStoreConfig.Kind.OBJECT_STORAGE;
        }
        if (SkillStoreConfigKindAliases.isJdbc(value)) {
            return SkillArtifactStoreConfig.Kind.JDBC;
        }
        if (SkillStoreConfigKindAliases.isCustom(value)) {
            return SkillArtifactStoreConfig.Kind.CUSTOM;
        }
        if (SkillStoreConfigKindAliases.isHybrid(value)) {
            return SkillArtifactStoreConfig.Kind.HYBRID;
        }
        if (SkillStoreConfigKindAliases.isMirrored(value)) {
            return SkillArtifactStoreConfig.Kind.MIRRORED;
        }
        throw new IllegalArgumentException("Unknown artifact store kind: " + value);
    }
}
