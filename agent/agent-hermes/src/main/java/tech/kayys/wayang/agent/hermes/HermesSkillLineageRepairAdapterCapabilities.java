package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Declared routing and execution capabilities for a lineage repair adapter.
 */
public record HermesSkillLineageRepairAdapterCapabilities(
        String adapterId,
        List<String> backendIds,
        List<String> storageFamilies,
        boolean previewSupported,
        boolean applySupported,
        boolean rollbackSupported,
        boolean mutationSupported,
        boolean approvalRequired,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairAdapterCapabilities {
        adapterId = normalize(adapterId, "repair-adapter");
        backendIds = normalizeList(backendIds);
        storageFamilies = normalizeList(storageFamilies);
        previewSupported = previewSupported || applySupported || rollbackSupported;
        mutationSupported = mutationSupported || applySupported || rollbackSupported;
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairAdapterCapabilities previewOnly(
            String adapterId,
            List<String> backendIds,
            List<String> storageFamilies) {
        return new HermesSkillLineageRepairAdapterCapabilities(
                adapterId,
                backendIds,
                storageFamilies,
                true,
                false,
                false,
                false,
                false,
                Map.of());
    }

    public static HermesSkillLineageRepairAdapterCapabilities mutating(
            String adapterId,
            List<String> backendIds,
            List<String> storageFamilies,
            boolean approvalRequired) {
        return new HermesSkillLineageRepairAdapterCapabilities(
                adapterId,
                backendIds,
                storageFamilies,
                true,
                true,
                true,
                true,
                approvalRequired,
                Map.of());
    }

    public boolean supportsAction(String action) {
        return switch (normalize(action, HermesSkillLineageRepairAdapter.PREVIEW)) {
            case HermesSkillLineageRepairAdapter.PREVIEW -> previewSupported;
            case HermesSkillLineageRepairAdapter.APPLY -> applySupported;
            case HermesSkillLineageRepairAdapter.ROLLBACK -> rollbackSupported;
            default -> false;
        };
    }

    public boolean supportsBatch(HermesSkillLineageRepairOperationBatch batch) {
        return batch != null
                && (matchesBackend(batch.backendId()) || matchesStorageFamily(batch.storageFamily()));
    }

    public boolean matchesBackend(String backendId) {
        return backendIds.contains(normalize(backendId, ""));
    }

    public boolean matchesStorageFamily(String storageFamily) {
        return storageFamilies.contains(normalize(storageFamily, ""));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("adapterId", adapterId);
        values.put("backendIds", backendIds);
        values.put("storageFamilies", storageFamilies);
        values.put("previewSupported", previewSupported);
        values.put("applySupported", applySupported);
        values.put("rollbackSupported", rollbackSupported);
        values.put("mutationSupported", mutationSupported);
        values.put("approvalRequired", approvalRequired);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    static String normalize(String value, String fallback) {
        String resolved = value == null || value.isBlank() ? fallback : HermesText.oneLine(value);
        return resolved.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static List<String> normalizeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> normalize(value, ""))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }
}
