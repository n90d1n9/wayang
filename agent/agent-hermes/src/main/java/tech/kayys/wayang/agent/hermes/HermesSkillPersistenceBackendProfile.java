package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter-facing backend profile for one learned-skill persistence route.
 */
public record HermesSkillPersistenceBackendProfile(
        String backendId,
        String routeRole,
        String storageFamily,
        String store,
        String storeType,
        int priority,
        boolean fallback,
        List<String> aliases,
        HermesSkillPersistenceStoreDescriptor storeDescriptor) {

    public HermesSkillPersistenceBackendProfile {
        storeDescriptor = storeDescriptor == null
                ? HermesSkillPersistenceStoreDescriptor.from(store, storeType)
                : storeDescriptor;
        backendId = normalizeBackendId(HermesText.oneLineOr(backendId, backendIdFor(storeDescriptor)));
        routeRole = HermesSkillPersistenceRouteRoles.normalize(routeRole);
        storageFamily = normalizeBackendId(HermesText.oneLineOr(storageFamily, familyFor(storeDescriptor)));
        store = HermesText.oneLineOr(store, storeDescriptor.store());
        storeType = HermesText.oneLineOr(storeType, storeDescriptor.storeType());
        aliases = aliases == null
                ? aliasesFor(storeDescriptor)
                : aliases.stream()
                        .filter(alias -> alias != null && !alias.isBlank())
                        .map(HermesSkillPersistenceBackendProfile::normalizeBackendId)
                        .distinct()
                        .toList();
    }

    public static HermesSkillPersistenceBackendProfile from(HermesSkillPersistenceRoute route) {
        HermesSkillPersistenceRoute effective = route == null
                ? new HermesSkillPersistenceRoute(
                        HermesSkillPersistenceRouteRoles.CUSTOM,
                        "none",
                        "none",
                        0,
                        false)
                : route;
        HermesSkillPersistenceStoreDescriptor descriptor = effective.descriptor();
        return new HermesSkillPersistenceBackendProfile(
                backendIdFor(descriptor),
                effective.role(),
                familyFor(descriptor),
                effective.store(),
                effective.storeType(),
                effective.priority(),
                effective.fallback(),
                aliasesFor(descriptor),
                descriptor);
    }

    public boolean roleIs(String role) {
        return routeRole.equals(HermesSkillPersistenceRouteRoles.normalize(role));
    }

    public boolean storageFamilyIs(String family) {
        return storageFamily.equals(normalizeBackendId(family));
    }

    public HermesSkillPersistenceBackendCapabilities capabilities() {
        return HermesSkillPersistenceBackendCapabilities.from(this);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backendId", backendId);
        values.put("routeRole", routeRole);
        values.put("storageFamily", storageFamily);
        values.put("store", store);
        values.put("storeType", storeType);
        values.put("priority", priority);
        values.put("fallback", fallback);
        values.put("aliases", aliases);
        values.put("capabilities", capabilities().toMetadata());
        values.put("storeDescriptor", storeDescriptor.toMetadata());
        return Map.copyOf(values);
    }

    static String backendIdFor(HermesSkillPersistenceStoreDescriptor descriptor) {
        HermesSkillPersistenceStoreDescriptor effective = descriptor == null
                ? HermesSkillPersistenceStoreDescriptor.from("none", "none")
                : descriptor;
        if (!effective.canonicalCloudStore().isBlank()) {
            return effective.canonicalCloudStore();
        }
        return switch (effective.storeType()) {
            case "database" -> "database";
            case "file" -> "file-system";
            case "skill-management" -> "skill-management";
            case "object-storage" -> "object-storage";
            case "hybrid" -> "hybrid";
            case "none" -> "none";
            default -> normalizeBackendId(effective.store());
        };
    }

    static String familyFor(HermesSkillPersistenceStoreDescriptor descriptor) {
        HermesSkillPersistenceStoreDescriptor effective = descriptor == null
                ? HermesSkillPersistenceStoreDescriptor.from("none", "none")
                : descriptor;
        return switch (effective.storeType()) {
            case "database" -> "database";
            case "file" -> "file-system";
            case "object-storage" -> "object-storage";
            case "skill-management" -> "skill-management";
            case "hybrid" -> "hybrid";
            case "none" -> "none";
            default -> "custom";
        };
    }

    static List<String> aliasesFor(HermesSkillPersistenceStoreDescriptor descriptor) {
        HermesSkillPersistenceStoreDescriptor effective = descriptor == null
                ? HermesSkillPersistenceStoreDescriptor.from("none", "none")
                : descriptor;
        return switch (effective.storeType()) {
            case "database" -> List.of("database", "jdbc", "postgres", "postgresql", "mysql", "mariadb");
            case "file" -> List.of("file-system", "file", "files", "filesystem", "local-file", "local-files");
            case "object-storage" -> List.of(
                    "object-storage",
                    "s3",
                    "s3-compatible",
                    "minio",
                    "rustfs",
                    "gcs",
                    "azure-blob");
            case "skill-management" -> List.of(
                    "skill-management",
                    "skill-management.definition-store",
                    "skill-management.artifact-store");
            case "hybrid" -> List.of("hybrid", "database", "object-storage", "file-system");
            default -> List.of();
        };
    }

    static String normalizeBackendId(String value) {
        String normalized = HermesText.oneLineOr(value, "none")
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        return normalized.isBlank() ? "none" : normalized;
    }
}
