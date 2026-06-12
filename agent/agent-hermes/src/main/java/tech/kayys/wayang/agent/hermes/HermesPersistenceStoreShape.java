package tech.kayys.wayang.agent.hermes;

import java.util.Map;

/**
 * Shared capability flags for configured Hermes persistence stores.
 */
record HermesPersistenceStoreShape(HermesPersistenceStoreKind store) {

    HermesPersistenceStoreShape {
        store = store == null ? HermesPersistenceStoreKind.NOOP : store;
    }

    static HermesPersistenceStoreShape of(String store) {
        return of(HermesPersistenceStoreKind.repairStore(store, "persistenceStore"));
    }

    static HermesPersistenceStoreShape of(HermesPersistenceStoreKind store) {
        return new HermesPersistenceStoreShape(store);
    }

    boolean durable() {
        return store.durable();
    }

    boolean fileFallback() {
        return store.fileFallback();
    }

    boolean objectStorageCapable() {
        return store.objectStorageCapable();
    }

    boolean databaseCapable() {
        return store.databaseCapable();
    }

    boolean replaySupported() {
        return store.replaySupported();
    }

    void putStorageMetadata(Map<String, Object> values) {
        values.put("durable", durable());
        values.put("fileFallback", fileFallback());
        values.put("objectStorageCapable", objectStorageCapable());
        values.put("databaseCapable", databaseCapable());
    }

    void putRuntimeJournalMetadata(Map<String, Object> values, boolean enabled) {
        values.put("durable", enabled && durable());
        values.put("fileFallback", fileFallback());
        values.put("objectStorageCapable", objectStorageCapable());
        values.put("databaseCapable", databaseCapable());
        values.put("queryable", enabled);
    }
}
