package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;

/**
 * Configuration model for selecting skill-management event history storage.
 */
public record SkillManagementEventStoreConfig(
        Kind kind,
        Path directory,
        String objectPrefix,
        int maxEvents,
        String customStoreName,
        String jdbcTableName,
        boolean initializeJdbcSchema,
        SkillManagementEventStoreConfig primary,
        SkillManagementEventStoreConfig fallback) {

    public enum Kind {
        NONE,
        MEMORY,
        FILESYSTEM,
        OBJECT_STORAGE,
        JDBC,
        CUSTOM,
        HYBRID,
        MIRRORED
    }

    public SkillManagementEventStoreConfig {
        kind = kind == null ? Kind.NONE : kind;
        maxEvents = SkillManagementEventRetention.normalizeCapacityOrDefault(
                maxEvents,
                InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
        validate(
                kind,
                directory,
                objectPrefix,
                maxEvents,
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
                maxEvents,
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
            int maxEvents,
            String customStoreName,
            String jdbcTableName,
            boolean initializeJdbcSchema,
            SkillManagementEventStoreConfig primary,
            SkillManagementEventStoreConfig fallback) {
        Kind resolvedKind = kind == null ? Kind.NONE : kind;
        return SkillStoreConfigValidation.builder()
                .requireDirectoryWhen(
                        resolvedKind == Kind.FILESYSTEM,
                        directory,
                        "Filesystem event store requires a directory")
                .requireTextWhen(
                        resolvedKind == Kind.OBJECT_STORAGE,
                        objectPrefix,
                        "Object-storage event store requires an object prefix")
                .requireTextWhen(
                        resolvedKind == Kind.JDBC,
                        jdbcTableName,
                        "JDBC event store requires a table name")
                .requireTextWhen(
                        resolvedKind == Kind.CUSTOM,
                        customStoreName,
                        "Custom event store requires a store name")
                .requirePairWhen(
                        resolvedKind == Kind.HYBRID || resolvedKind == Kind.MIRRORED,
                        primary,
                        fallback,
                        resolvedKind + " event store requires primary and fallback configs")
                .result();
    }

    public static SkillManagementEventStoreConfig none() {
        return new SkillManagementEventStoreConfig(Kind.NONE, null, null, 0, null, null, false, null, null);
    }

    public static SkillManagementEventStoreConfig memory() {
        return memory(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public static SkillManagementEventStoreConfig memory(int maxEvents) {
        return new SkillManagementEventStoreConfig(Kind.MEMORY, null, null, maxEvents, null, null, false, null, null);
    }

    public static SkillManagementEventStoreConfig fileSystem(Path directory) {
        return fileSystem(directory, InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public static SkillManagementEventStoreConfig fileSystem(Path directory, int maxEvents) {
        return new SkillManagementEventStoreConfig(
                Kind.FILESYSTEM,
                directory,
                null,
                maxEvents,
                null,
                null,
                false,
                null,
                null);
    }

    public static SkillManagementEventStoreConfig objectStorage(String objectPrefix) {
        return objectStorage(objectPrefix, InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public static SkillManagementEventStoreConfig objectStorage(String objectPrefix, int maxEvents) {
        return new SkillManagementEventStoreConfig(
                Kind.OBJECT_STORAGE,
                null,
                objectPrefix,
                maxEvents,
                null,
                null,
                false,
                null,
                null);
    }

    public static SkillManagementEventStoreConfig jdbc() {
        return jdbc(JdbcSkillManagementEventStore.DEFAULT_TABLE_NAME, true);
    }

    public static SkillManagementEventStoreConfig jdbc(String tableName, boolean initializeSchema) {
        return jdbc(tableName, initializeSchema, InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public static SkillManagementEventStoreConfig jdbc(
            String tableName,
            boolean initializeSchema,
            int maxEvents) {
        return new SkillManagementEventStoreConfig(
                Kind.JDBC,
                null,
                null,
                maxEvents,
                null,
                tableName,
                initializeSchema,
                null,
                null);
    }

    public static SkillManagementEventStoreConfig custom(String storeName) {
        return new SkillManagementEventStoreConfig(Kind.CUSTOM, null, null, 0, storeName, null, false, null, null);
    }

    public static SkillManagementEventStoreConfig hybrid(
            SkillManagementEventStoreConfig primary,
            SkillManagementEventStoreConfig fallback) {
        return new SkillManagementEventStoreConfig(
                Kind.HYBRID,
                null,
                null,
                0,
                null,
                null,
                false,
                primary,
                fallback);
    }

    public static SkillManagementEventStoreConfig mirrored(
            SkillManagementEventStoreConfig primary,
            SkillManagementEventStoreConfig fallback) {
        return new SkillManagementEventStoreConfig(
                Kind.MIRRORED,
                null,
                null,
                0,
                null,
                null,
                false,
                primary,
                fallback);
    }

    public static SkillManagementEventStoreConfig mirroredWithFileFallback(
            SkillManagementEventStoreConfig primary,
            Path fallbackDirectory) {
        return mirrored(primary, fileSystem(fallbackDirectory));
    }
}
