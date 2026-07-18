package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;

/**
 * Configuration model for selecting the skill definition persistence topology.
 */
public record SkillDefinitionStoreConfig(
        Kind kind,
        Path directory,
        String objectPrefix,
        String customStoreName,
        String jdbcTableName,
        boolean initializeJdbcSchema,
        SkillDefinitionStoreConfig primary,
        SkillDefinitionStoreConfig fallback) {

    public enum Kind {
        REGISTRY,
        FILESYSTEM,
        OBJECT_STORAGE,
        JDBC,
        CUSTOM,
        HYBRID,
        MIRRORED
    }

    public SkillDefinitionStoreConfig {
        kind = kind == null ? Kind.REGISTRY : kind;
        validate(
                kind,
                directory,
                objectPrefix,
                customStoreName,
                jdbcTableName,
                initializeJdbcSchema,
                primary,
                fallback)
                .throwIfInvalid();
    }

    public SkillStoreConfigValidationResult validate() {
        return validate(
                kind,
                directory,
                objectPrefix,
                customStoreName,
                jdbcTableName,
                initializeJdbcSchema,
                primary,
                fallback);
    }

    public static SkillStoreConfigValidationResult validate(
            Kind kind,
            Path directory,
            String objectPrefix,
            String customStoreName,
            String jdbcTableName,
            boolean initializeJdbcSchema,
            SkillDefinitionStoreConfig primary,
            SkillDefinitionStoreConfig fallback) {
        Kind resolvedKind = kind == null ? Kind.REGISTRY : kind;
        return SkillStoreConfigValidation.builder()
                .requireDirectoryWhen(
                        resolvedKind == Kind.FILESYSTEM,
                        directory,
                        "Filesystem skill store requires a directory")
                .requireTextWhen(
                        resolvedKind == Kind.CUSTOM,
                        customStoreName,
                        "Custom skill store requires a store name")
                .requireTextWhen(
                        resolvedKind == Kind.JDBC,
                        jdbcTableName,
                        "JDBC skill store requires a table name")
                .requirePairWhen(
                        resolvedKind == Kind.HYBRID || resolvedKind == Kind.MIRRORED,
                        primary,
                        fallback,
                        resolvedKind + " skill store requires primary and fallback configs")
                .result();
    }

    public static SkillDefinitionStoreConfig registry() {
        return new SkillDefinitionStoreConfig(Kind.REGISTRY, null, null, null, null, false, null, null);
    }

    public static SkillDefinitionStoreConfig fileSystem(Path directory) {
        return new SkillDefinitionStoreConfig(Kind.FILESYSTEM, directory, null, null, null, false, null, null);
    }

    public static SkillDefinitionStoreConfig objectStorage(String objectPrefix) {
        return new SkillDefinitionStoreConfig(Kind.OBJECT_STORAGE, null, objectPrefix, null, null, false, null, null);
    }

    public static SkillDefinitionStoreConfig jdbc() {
        return jdbc(JdbcSkillDefinitionStore.DEFAULT_TABLE_NAME, true);
    }

    public static SkillDefinitionStoreConfig jdbc(String tableName, boolean initializeSchema) {
        return new SkillDefinitionStoreConfig(Kind.JDBC, null, null, null, tableName, initializeSchema, null, null);
    }

    public static SkillDefinitionStoreConfig custom(String storeName) {
        return new SkillDefinitionStoreConfig(Kind.CUSTOM, null, null, storeName, null, false, null, null);
    }

    public static SkillDefinitionStoreConfig hybrid(
            SkillDefinitionStoreConfig primary,
            SkillDefinitionStoreConfig fallback) {
        return new SkillDefinitionStoreConfig(Kind.HYBRID, null, null, null, null, false, primary, fallback);
    }

    public static SkillDefinitionStoreConfig hybridWithFileFallback(
            SkillDefinitionStoreConfig primary,
            Path fallbackDirectory) {
        return hybrid(primary, fileSystem(fallbackDirectory));
    }

    public static SkillDefinitionStoreConfig mirrored(
            SkillDefinitionStoreConfig primary,
            SkillDefinitionStoreConfig fallback) {
        return new SkillDefinitionStoreConfig(Kind.MIRRORED, null, null, null, null, false, primary, fallback);
    }

    public static SkillDefinitionStoreConfig mirroredWithFileFallback(
            SkillDefinitionStoreConfig primary,
            Path fallbackDirectory) {
        return mirrored(primary, fileSystem(fallbackDirectory));
    }
}
