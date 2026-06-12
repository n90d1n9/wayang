package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Parses deployable skill-management event history configuration.
 */
public final class SkillManagementEventStoreConfigs {

    public static final String PREFIX = "wayang.skills.events.store.";
    public static final String ENV_PREFIX = "WAYANG_SKILLS_EVENTS_STORE_";

    private SkillManagementEventStoreConfigs() {
    }

    public static SkillManagementEventStoreConfig fromSystemProperties() {
        return SkillStoreConfigParsing.fromSystemProperties(PREFIX, SkillManagementEventStoreConfigs::parse);
    }

    public static SkillManagementEventStoreConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillManagementEventStoreConfig fromProperties(Properties properties) {
        return fromProperties(properties, PREFIX);
    }

    public static SkillManagementEventStoreConfig fromProperties(Properties properties, String prefix) {
        return SkillStoreConfigParsing.fromProperties(properties, prefix, SkillManagementEventStoreConfigs::parse);
    }

    public static SkillManagementEventStoreConfig fromMap(Map<String, ?> values) {
        return fromMap(values, PREFIX);
    }

    public static SkillManagementEventStoreConfig fromMap(Map<String, ?> values, String prefix) {
        return SkillStoreConfigParsing.fromMap(values, prefix, SkillManagementEventStoreConfigs::parse);
    }

    public static SkillManagementEventStoreConfig fromEnvironment(Map<String, String> environment) {
        return SkillStoreConfigParsing.fromEnvironment(
                environment,
                ENV_PREFIX,
                PREFIX,
                SkillManagementEventStoreConfigs::parse);
    }

    private static SkillManagementEventStoreConfig parse(Map<String, String> values, String prefix) {
        SkillStoreConfigValues.ScopedValues scoped = new SkillStoreConfigValues.ScopedValues(values, prefix);
        SkillManagementEventStoreConfig.Kind kind = kind(SkillStoreConfigKeys.storeKind(scoped, "none"));
        int maxEvents = SkillStoreConfigKeys.eventMaxEvents(
                scoped,
                InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS,
                "Invalid event store max event count");
        return switch (kind) {
            case NONE -> SkillManagementEventStoreConfig.none();
            case MEMORY -> SkillManagementEventStoreConfig.memory(maxEvents);
            case FILESYSTEM -> SkillManagementEventStoreConfig.fileSystem(Path.of(SkillStoreConfigKeys.directory(
                    scoped,
                    "Filesystem event store requires a directory")), maxEvents);
            case OBJECT_STORAGE -> SkillManagementEventStoreConfig.objectStorage(
                    SkillStoreConfigKeys.objectPrefix(scoped, ObjectStorageSkillManagementEventStore.DEFAULT_PREFIX),
                    maxEvents);
            case JDBC -> SkillManagementEventStoreConfig.jdbc(
                    SkillStoreConfigKeys.jdbcTableName(scoped, JdbcSkillManagementEventStore.DEFAULT_TABLE_NAME),
                    SkillStoreConfigKeys.initializeJdbcSchema(scoped),
                    maxEvents);
            case CUSTOM -> SkillManagementEventStoreConfig.custom(SkillStoreConfigKeys.customStoreName(
                    scoped,
                    "Custom event store requires a store name"));
            case HYBRID -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillManagementEventStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Hybrid event store requires primary and fallback config groups",
                                SkillManagementEventStoreConfigs::parse);
                yield SkillManagementEventStoreConfig.hybrid(children.primary(), children.fallback());
            }
            case MIRRORED -> {
                SkillStoreConfigParsing.PrimaryFallback<SkillManagementEventStoreConfig> children =
                        SkillStoreConfigParsing.primaryFallback(
                                values,
                                prefix,
                                scoped,
                                "Mirrored event store requires primary and fallback config groups",
                                SkillManagementEventStoreConfigs::parse);
                yield SkillManagementEventStoreConfig.mirrored(children.primary(), children.fallback());
            }
        };
    }

    private static SkillManagementEventStoreConfig.Kind kind(String value) {
        if (SkillStoreConfigKindAliases.isEventNone(value)) {
            return SkillManagementEventStoreConfig.Kind.NONE;
        }
        if (SkillStoreConfigKindAliases.isEventMemory(value)) {
            return SkillManagementEventStoreConfig.Kind.MEMORY;
        }
        if (SkillStoreConfigKindAliases.isFilesystem(value)) {
            return SkillManagementEventStoreConfig.Kind.FILESYSTEM;
        }
        if (SkillStoreConfigKindAliases.isObjectStorage(value)) {
            return SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE;
        }
        if (SkillStoreConfigKindAliases.isJdbc(value)) {
            return SkillManagementEventStoreConfig.Kind.JDBC;
        }
        if (SkillStoreConfigKindAliases.isCustom(value)) {
            return SkillManagementEventStoreConfig.Kind.CUSTOM;
        }
        if (SkillStoreConfigKindAliases.isEventMirrored(value)) {
            return SkillManagementEventStoreConfig.Kind.MIRRORED;
        }
        if (SkillStoreConfigKindAliases.isEventHybrid(value)) {
            return SkillManagementEventStoreConfig.Kind.HYBRID;
        }
        throw new IllegalArgumentException("Unknown skill-management event store kind: " + value);
    }
}
