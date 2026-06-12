package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One adapter-neutral learned-skill persistence route.
 */
public record HermesSkillPersistenceRoute(
        String role,
        String store,
        String storeType,
        int priority,
        boolean fallback) {

    public HermesSkillPersistenceRoute {
        role = HermesSkillPersistenceRouteRoles.normalize(role);
        store = HermesText.trimOr(store, "none");
        storeType = HermesText.trimOr(storeType, HermesSkillPersistenceStoreClassifier.storeType(store));
    }

    public boolean roleIs(String expectedRole) {
        return role.equals(HermesSkillPersistenceRouteRoles.normalize(expectedRole));
    }

    public HermesSkillPersistenceStoreDescriptor descriptor() {
        return HermesSkillPersistenceStoreDescriptor.from(store, storeType);
    }

    public boolean databaseBacked() {
        return descriptor().databaseBacked();
    }

    public boolean cloudBacked() {
        return descriptor().cloudBacked();
    }

    public boolean fileBacked() {
        return descriptor().fileBacked();
    }

    public boolean skillManagementBacked() {
        return descriptor().skillManagementBacked();
    }

    public Map<String, Object> toMetadata() {
        HermesSkillPersistenceStoreDescriptor descriptor = descriptor();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("role", role);
        metadata.put("store", store);
        metadata.put("storeType", storeType);
        metadata.put("priority", priority);
        metadata.put("fallback", fallback);
        metadata.put("databaseBacked", descriptor.databaseBacked());
        metadata.put("cloudBacked", descriptor.cloudBacked());
        metadata.put("fileBacked", descriptor.fileBacked());
        metadata.put("skillManagementBacked", descriptor.skillManagementBacked());
        metadata.put("storeDescriptor", descriptor.toMetadata());
        return Map.copyOf(metadata);
    }
}
