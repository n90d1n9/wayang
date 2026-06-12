package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;

/**
 * Configuration model for selecting dynamic skill artifact persistence.
 */
public record SkillArtifactStoreConfig(
        Kind kind,
        Path directory,
        String objectPrefix,
        String jdbcTableName,
        boolean initializeJdbcSchema,
        String customStoreName,
        SkillArtifactStoreConfig primary,
        SkillArtifactStoreConfig fallback) {

    public enum Kind {
        MEMORY,
        FILESYSTEM,
        OBJECT_STORAGE,
        JDBC,
        CUSTOM,
        HYBRID,
        MIRRORED
    }

    public SkillArtifactStoreConfig {
        kind = kind == null ? Kind.MEMORY : kind;
        validate(kind, directory, objectPrefix, jdbcTableName, customStoreName, primary, fallback)
                .throwIfInvalid();
    }

    public SkillStoreConfigValidationResult validate() {
        return validate(kind, directory, objectPrefix, jdbcTableName, customStoreName, primary, fallback);
    }

    public static SkillStoreConfigValidationResult validate(
            Kind kind,
            Path directory,
            String objectPrefix,
            String jdbcTableName,
            String customStoreName,
            SkillArtifactStoreConfig primary,
            SkillArtifactStoreConfig fallback) {
        Kind resolvedKind = kind == null ? Kind.MEMORY : kind;
        return SkillStoreConfigValidation.builder()
                .requireDirectoryWhen(
                        resolvedKind == Kind.FILESYSTEM,
                        directory,
                        "Filesystem artifact store requires a directory")
                .requireTextWhen(
                        resolvedKind == Kind.JDBC,
                        jdbcTableName,
                        "JDBC artifact store requires a table name")
                .requireTextWhen(
                        resolvedKind == Kind.CUSTOM,
                        customStoreName,
                        "Custom artifact store requires a store name")
                .requirePairWhen(
                        resolvedKind == Kind.HYBRID || resolvedKind == Kind.MIRRORED,
                        primary,
                        fallback,
                        resolvedKind + " artifact store requires primary and fallback configs")
                .result();
    }

    public static SkillArtifactStoreConfig memory() {
        return new SkillArtifactStoreConfig(Kind.MEMORY, null, null, null, true, null, null, null);
    }

    public static SkillArtifactStoreConfig fileSystem(Path directory) {
        return new SkillArtifactStoreConfig(Kind.FILESYSTEM, directory, null, null, true, null, null, null);
    }

    public static SkillArtifactStoreConfig objectStorage(String objectPrefix) {
        return new SkillArtifactStoreConfig(Kind.OBJECT_STORAGE, null, objectPrefix, null, true, null, null, null);
    }

    public static SkillArtifactStoreConfig jdbc() {
        return jdbc(JdbcSkillArtifactStore.DEFAULT_TABLE_NAME, true);
    }

    public static SkillArtifactStoreConfig jdbc(String tableName, boolean initializeSchema) {
        return new SkillArtifactStoreConfig(Kind.JDBC, null, null, tableName, initializeSchema, null, null, null);
    }

    public static SkillArtifactStoreConfig custom(String storeName) {
        return new SkillArtifactStoreConfig(Kind.CUSTOM, null, null, null, true, storeName, null, null);
    }

    public static SkillArtifactStoreConfig hybrid(
            SkillArtifactStoreConfig primary,
            SkillArtifactStoreConfig fallback) {
        return new SkillArtifactStoreConfig(Kind.HYBRID, null, null, null, true, null, primary, fallback);
    }

    public static SkillArtifactStoreConfig hybridWithFileFallback(
            SkillArtifactStoreConfig primary,
            Path fallbackDirectory) {
        return hybrid(primary, fileSystem(fallbackDirectory));
    }

    public static SkillArtifactStoreConfig mirrored(
            SkillArtifactStoreConfig primary,
            SkillArtifactStoreConfig fallback) {
        return new SkillArtifactStoreConfig(Kind.MIRRORED, null, null, null, true, null, primary, fallback);
    }

    public static SkillArtifactStoreConfig mirroredWithFileFallback(
            SkillArtifactStoreConfig primary,
            Path fallbackDirectory) {
        return mirrored(primary, fileSystem(fallbackDirectory));
    }
}
