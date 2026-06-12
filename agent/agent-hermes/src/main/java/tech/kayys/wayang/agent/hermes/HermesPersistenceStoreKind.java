package tech.kayys.wayang.agent.hermes;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Canonical Hermes persistence stores shared by config parsing and resolvers.
 */
enum HermesPersistenceStoreKind {
    NOOP("noop", false, false, false, false, false),
    IN_MEMORY("in-memory", false, false, false, false, true),
    FILE_SYSTEM("file-system", true, true, false, false, true),
    OBJECT_STORAGE("object-storage", true, false, true, false, true),
    DATABASE("database", true, false, false, true, true),
    HYBRID("hybrid", true, true, true, true, true);

    private static final Set<HermesPersistenceStoreKind> RUNTIME_EVENT_JOURNAL_STORES =
            EnumSet.of(FILE_SYSTEM, OBJECT_STORAGE, DATABASE, HYBRID);
    private static final Set<HermesPersistenceStoreKind> REPAIR_STORES =
            EnumSet.allOf(HermesPersistenceStoreKind.class);

    private final String configValue;
    private final boolean durable;
    private final boolean fileFallback;
    private final boolean objectStorageCapable;
    private final boolean databaseCapable;
    private final boolean replaySupported;

    HermesPersistenceStoreKind(
            String configValue,
            boolean durable,
            boolean fileFallback,
            boolean objectStorageCapable,
            boolean databaseCapable,
            boolean replaySupported) {
        this.configValue = configValue;
        this.durable = durable;
        this.fileFallback = fileFallback;
        this.objectStorageCapable = objectStorageCapable;
        this.databaseCapable = databaseCapable;
        this.replaySupported = replaySupported;
    }

    static HermesPersistenceStoreKind runtimeEventJournal(String value) {
        return parse(
                value,
                FILE_SYSTEM,
                RUNTIME_EVENT_JOURNAL_STORES,
                "runtimeEventJournalStore must be file-system, object-storage, database, or hybrid");
    }

    static HermesPersistenceStoreKind repairStore(String value, String configName) {
        String effectiveName = configName == null || configName.isBlank() ? "persistenceStore" : configName;
        return parse(
                value,
                NOOP,
                REPAIR_STORES,
                effectiveName
                        + " must be noop, in-memory, file-system, object-storage, database, or hybrid");
    }

    String configValue() {
        return configValue;
    }

    boolean durable() {
        return durable;
    }

    boolean fileFallback() {
        return fileFallback;
    }

    boolean objectStorageCapable() {
        return objectStorageCapable;
    }

    boolean databaseCapable() {
        return databaseCapable;
    }

    boolean replaySupported() {
        return replaySupported;
    }

    private static HermesPersistenceStoreKind parse(
            String value,
            HermesPersistenceStoreKind fallback,
            Set<HermesPersistenceStoreKind> allowedStores,
            String errorMessage) {
        String normalized = normalize(value, fallback);
        for (HermesPersistenceStoreKind kind : values()) {
            if (kind.configValue.equals(normalized)) {
                if (allowedStores.contains(kind)) {
                    return kind;
                }
                throw new IllegalArgumentException(errorMessage);
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private static String normalize(String value, HermesPersistenceStoreKind fallback) {
        String normalized = value == null || value.isBlank()
                ? fallback.configValue
                : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if ("jdbc".equals(normalized) || "db".equals(normalized) || "sql".equals(normalized)) {
            return DATABASE.configValue;
        }
        return normalized;
    }
}
