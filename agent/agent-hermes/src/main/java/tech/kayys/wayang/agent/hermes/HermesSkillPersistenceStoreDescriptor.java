package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral capability description for a learned-skill persistence store.
 */
public record HermesSkillPersistenceStoreDescriptor(
        String store,
        String storeType,
        String canonicalCloudStore,
        boolean databaseBacked,
        boolean cloudBacked,
        boolean fileBacked,
        boolean skillManagementBacked,
        boolean hybrid,
        boolean custom) {

    public HermesSkillPersistenceStoreDescriptor {
        store = HermesText.trimOr(store, "none");
        storeType = HermesText.trimOr(storeType, HermesSkillPersistenceStoreClassifier.storeType(store));
        canonicalCloudStore = HermesText.trimOr(canonicalCloudStore, "");
    }

    public static HermesSkillPersistenceStoreDescriptor from(String store) {
        return from(store, HermesSkillPersistenceStoreClassifier.storeType(store));
    }

    public static HermesSkillPersistenceStoreDescriptor from(String store, String storeType) {
        String type = HermesText.trimOr(storeType, HermesSkillPersistenceStoreClassifier.storeType(store));
        boolean hybrid = "hybrid".equals(type);
        return new HermesSkillPersistenceStoreDescriptor(
                store,
                type,
                HermesSkillPersistenceStoreClassifier.canonicalCloudStore(store),
                hybrid || "database".equals(type),
                hybrid || "object-storage".equals(type),
                hybrid || "file".equals(type),
                "skill-management".equals(type),
                hybrid,
                "custom".equals(type));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("store", store);
        metadata.put("storeType", storeType);
        metadata.put("canonicalCloudStore", canonicalCloudStore);
        metadata.put("databaseBacked", databaseBacked);
        metadata.put("cloudBacked", cloudBacked);
        metadata.put("fileBacked", fileBacked);
        metadata.put("skillManagementBacked", skillManagementBacked);
        metadata.put("hybrid", hybrid);
        metadata.put("custom", custom);
        return Map.copyOf(metadata);
    }
}
