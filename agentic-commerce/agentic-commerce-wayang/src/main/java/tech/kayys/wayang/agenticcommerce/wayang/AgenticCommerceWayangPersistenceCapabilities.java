package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized capability summary for persistence stores.
 */
public record AgenticCommerceWayangPersistenceCapabilities(
        String storageKind,
        boolean durable,
        boolean ephemeral,
        boolean localFile,
        boolean objectStore,
        boolean cloudStorage,
        boolean hybrid,
        boolean mirrored,
        boolean fallbackReadable,
        List<String> storageKinds,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceCapabilities {
        storageKind = normalize(storageKind);
        storageKinds = normalizeKinds(storageKinds, storageKind);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceCapabilities from(
            AgenticCommerceWayangPersistenceStore store) {
        AgenticCommerceWayangPersistenceStore resolved = Objects.requireNonNull(store, "store");
        Map<String, Object> status;
        try {
            status = new LinkedHashMap<>(resolved.toMap());
        } catch (RuntimeException exception) {
            status = new LinkedHashMap<>();
            status.put("statusReadable", false);
        }
        status.putIfAbsent("storageKind", resolved.storageKind());
        return fromStatus(status);
    }

    public static AgenticCommerceWayangPersistenceCapabilities fromStatus(Map<?, ?> storeStatus) {
        Map<String, Object> status = AgenticCommerceWayangMaps.copy(storeStatus);
        String storageKind = normalize(AgenticCommerceWayangMaps.firstText(status, "storageKind", "kind", "type"));
        String primaryStorageKind = normalize(AgenticCommerceWayangMaps.firstText(status, "primaryStorageKind"));
        String fallbackStorageKind = normalize(AgenticCommerceWayangMaps.firstText(status, "fallbackStorageKind"));
        List<String> storageKinds = storageKinds(status, storageKind, primaryStorageKind, fallbackStorageKind);
        boolean hybrid = isKind(storageKind, HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                || !primaryStorageKind.isBlank()
                || !fallbackStorageKind.isBlank();
        boolean localFile = containsKind(storageKinds, FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        boolean objectStore = containsKind(storageKinds, ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                || status.containsKey("objectStore");
        boolean cloudStorage = objectStore;
        boolean memory = containsKind(storageKinds, InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                || containsKind(storageKinds, "memory")
                || containsKind(storageKinds, "runtime");
        boolean explicitEphemeral = AgenticCommerceWayangMaps.firstBoolean(status, "ephemeral", "transient")
                .orElse(false);
        boolean ephemeral = explicitEphemeral || (!hybrid && memory);
        boolean durable = !ephemeral && (localFile || objectStore || storageKinds.stream()
                .anyMatch(AgenticCommerceWayangPersistenceCapabilities::durableUnknown));
        boolean mirrored = AgenticCommerceWayangMaps.firstBoolean(
                status,
                "mirrorWritesToFallback",
                "mirrorWrites",
                "writeFallback").orElse(false);
        boolean fallbackReadable = hybrid && !fallbackStorageKind.isBlank();
        return new AgenticCommerceWayangPersistenceCapabilities(
                storageKind,
                durable,
                ephemeral,
                localFile,
                objectStore,
                cloudStorage,
                hybrid,
                mirrored,
                fallbackReadable,
                storageKinds,
                attributes(status, primaryStorageKind, fallbackStorageKind));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind);
        values.put("durable", durable);
        values.put("ephemeral", ephemeral);
        values.put("localFile", localFile);
        values.put("objectStore", objectStore);
        values.put("cloudStorage", cloudStorage);
        values.put("hybrid", hybrid);
        values.put("mirrored", mirrored);
        values.put("fallbackReadable", fallbackReadable);
        values.put("storageKinds", storageKinds);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<String, Object> attributes(
            Map<String, Object> status,
            String primaryStorageKind,
            String fallbackStorageKind) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusReadable", status.isEmpty() || !Boolean.FALSE.equals(status.get("statusReadable")));
        AgenticCommerceWayangMaps.putText(values, "primaryStorageKind", primaryStorageKind);
        AgenticCommerceWayangMaps.putText(values, "fallbackStorageKind", fallbackStorageKind);
        copyKnown(values, status, "documentCount");
        AgenticCommerceWayangPersistenceDocuments.ALL.forEach(
                document -> copyKnown(values, status, document.availabilityStatusKey()));
        return Map.copyOf(values);
    }

    private static void copyKnown(Map<String, Object> values, Map<String, Object> status, String key) {
        if (status.containsKey(key)) {
            values.put(key, status.get(key));
        }
    }

    private static List<String> storageKinds(
            Map<String, Object> status,
            String storageKind,
            String primaryStorageKind,
            String fallbackStorageKind) {
        List<String> kinds = new ArrayList<>();
        addKind(kinds, storageKind);
        addKind(kinds, primaryStorageKind);
        addKind(kinds, fallbackStorageKind);
        nestedKind(status, "primary").ifPresent(kind -> addKind(kinds, kind));
        nestedKind(status, "fallback").ifPresent(kind -> addKind(kinds, kind));
        return List.copyOf(kinds);
    }

    private static java.util.Optional<String> nestedKind(Map<String, Object> status, String key) {
        Object nested = status.get(key);
        if (nested instanceof Map<?, ?> map) {
            return java.util.Optional.of(normalize(AgenticCommerceWayangMaps.firstText(map, "storageKind")));
        }
        return java.util.Optional.empty();
    }

    private static List<String> normalizeKinds(List<String> storageKinds, String storageKind) {
        List<String> kinds = new ArrayList<>();
        addKind(kinds, storageKind);
        if (storageKinds != null) {
            for (String kind : storageKinds) {
                addKind(kinds, kind);
            }
        }
        return List.copyOf(kinds);
    }

    private static void addKind(List<String> kinds, String value) {
        String kind = normalize(value);
        if (!kind.isBlank() && !kinds.contains(kind)) {
            kinds.add(kind);
        }
    }

    private static boolean containsKind(List<String> kinds, String kind) {
        String normalized = normalize(kind);
        return kinds.stream().anyMatch(value -> isKind(value, normalized));
    }

    private static boolean isKind(String value, String kind) {
        return normalize(value).equals(normalize(kind));
    }

    private static boolean durableUnknown(String storageKind) {
        String kind = normalize(storageKind);
        return !kind.isBlank()
                && !isKind(kind, InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                && !isKind(kind, "memory")
                && !isKind(kind, "runtime")
                && !isKind(kind, HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    private static String normalize(String value) {
        return AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
    }
}
