package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;

/**
 * Configuration model for selecting lifecycle state persistence.
 */
public record SkillLifecycleStateStoreConfig(
        Kind kind,
        Path directory,
        String objectPrefix,
        String jdbcTableName,
        boolean initializeJdbcSchema,
        String customStoreName,
        SkillLifecycleStateStoreConfig primary,
        SkillLifecycleStateStoreConfig fallback) {

    public enum Kind {
        MEMORY,
        FILESYSTEM,
        OBJECT_STORAGE,
        JDBC,
        CUSTOM,
        HYBRID,
        MIRRORED
    }

    public SkillLifecycleStateStoreConfig(
            Kind kind,
            Path directory,
            String jdbcTableName,
            boolean initializeJdbcSchema) {
        this(kind, directory, null, jdbcTableName, initializeJdbcSchema, null, null, null);
    }

    public SkillLifecycleStateStoreConfig(
            Kind kind,
            Path directory,
            String jdbcTableName,
            boolean initializeJdbcSchema,
            String customStoreName) {
        this(kind, directory, null, jdbcTableName, initializeJdbcSchema, customStoreName, null, null);
    }

    public SkillLifecycleStateStoreConfig {
        kind = kind == null ? Kind.MEMORY : kind;
        validate(
                kind,
                directory,
                objectPrefix,
                jdbcTableName,
                initializeJdbcSchema,
                customStoreName,
                primary,
                fallback)
                .throwIfInvalid();
    }

    public SkillStoreConfigValidationResult validate() {
        return validate(
                kind,
                directory,
                objectPrefix,
                jdbcTableName,
                initializeJdbcSchema,
                customStoreName,
                primary,
                fallback);
    }

    public static SkillStoreConfigValidationResult validate(
            Kind kind,
            Path directory,
            String objectPrefix,
            String jdbcTableName,
            boolean initializeJdbcSchema,
            String customStoreName,
            SkillLifecycleStateStoreConfig primary,
            SkillLifecycleStateStoreConfig fallback) {
        Kind resolvedKind = kind == null ? Kind.MEMORY : kind;
        return SkillStoreConfigValidation.builder()
                .requireDirectoryWhen(
                        resolvedKind == Kind.FILESYSTEM,
                        directory,
                        "Filesystem lifecycle store requires a directory")
                .requireTextWhen(
                        resolvedKind == Kind.JDBC,
                        jdbcTableName,
                        "JDBC lifecycle store requires a table name")
                .requireTextWhen(
                        resolvedKind == Kind.CUSTOM,
                        customStoreName,
                        "Custom lifecycle store requires a store name")
                .requirePairWhen(
                        resolvedKind == Kind.HYBRID || resolvedKind == Kind.MIRRORED,
                        primary,
                        fallback,
                        resolvedKind + " lifecycle store requires primary and fallback configs")
                .result();
    }

    public static SkillLifecycleStateStoreConfig memory() {
        return new SkillLifecycleStateStoreConfig(Kind.MEMORY, null, null, null, false, null, null, null);
    }

    public static SkillLifecycleStateStoreConfig fileSystem(Path directory) {
        return new SkillLifecycleStateStoreConfig(Kind.FILESYSTEM, directory, null, null, false, null, null, null);
    }

    public static SkillLifecycleStateStoreConfig objectStorage(String objectPrefix) {
        return new SkillLifecycleStateStoreConfig(Kind.OBJECT_STORAGE, null, objectPrefix, null, false, null, null, null);
    }

    public static SkillLifecycleStateStoreConfig jdbc() {
        return jdbc(JdbcSkillLifecycleStateStore.DEFAULT_TABLE_NAME, true);
    }

    public static SkillLifecycleStateStoreConfig jdbc(String tableName, boolean initializeSchema) {
        return new SkillLifecycleStateStoreConfig(Kind.JDBC, null, null, tableName, initializeSchema, null, null, null);
    }

    public static SkillLifecycleStateStoreConfig custom(String storeName) {
        return new SkillLifecycleStateStoreConfig(Kind.CUSTOM, null, null, null, false, storeName, null, null);
    }

    public static SkillLifecycleStateStoreConfig hybrid(
            SkillLifecycleStateStoreConfig primary,
            SkillLifecycleStateStoreConfig fallback) {
        return new SkillLifecycleStateStoreConfig(Kind.HYBRID, null, null, null, false, null, primary, fallback);
    }

    public static SkillLifecycleStateStoreConfig hybridWithFileFallback(
            SkillLifecycleStateStoreConfig primary,
            Path fallbackDirectory) {
        return hybrid(primary, fileSystem(fallbackDirectory));
    }

    public static SkillLifecycleStateStoreConfig mirrored(
            SkillLifecycleStateStoreConfig primary,
            SkillLifecycleStateStoreConfig fallback) {
        return new SkillLifecycleStateStoreConfig(Kind.MIRRORED, null, null, null, false, null, primary, fallback);
    }

    public static SkillLifecycleStateStoreConfig mirroredWithFileFallback(
            SkillLifecycleStateStoreConfig primary,
            Path fallbackDirectory) {
        return mirrored(primary, fileSystem(fallbackDirectory));
    }
}
