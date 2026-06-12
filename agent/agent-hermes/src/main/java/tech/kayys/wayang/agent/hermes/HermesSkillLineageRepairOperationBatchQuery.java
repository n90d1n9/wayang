package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter-facing filter for routed repair-operation batches.
 */
public record HermesSkillLineageRepairOperationBatchQuery(
        String backendId,
        String storageFamily,
        String batchStatus,
        boolean adapterReadyOnly) {

    public HermesSkillLineageRepairOperationBatchQuery {
        backendId = normalize(backendId);
        storageFamily = normalize(storageFamily);
        batchStatus = normalize(batchStatus);
    }

    public static HermesSkillLineageRepairOperationBatchQuery all() {
        return new HermesSkillLineageRepairOperationBatchQuery("", "", "", false);
    }

    public static HermesSkillLineageRepairOperationBatchQuery adapterReady() {
        return new HermesSkillLineageRepairOperationBatchQuery("", "", "", true);
    }

    public static HermesSkillLineageRepairOperationBatchQuery forBackend(String backendId) {
        return new HermesSkillLineageRepairOperationBatchQuery(backendId, "", "", false);
    }

    public static HermesSkillLineageRepairOperationBatchQuery forStorageFamily(String storageFamily) {
        return new HermesSkillLineageRepairOperationBatchQuery("", storageFamily, "", false);
    }

    public static HermesSkillLineageRepairOperationBatchQuery adapterReadyForBackend(String backendId) {
        return new HermesSkillLineageRepairOperationBatchQuery(backendId, "", "", true);
    }

    public static HermesSkillLineageRepairOperationBatchQuery forStatus(String batchStatus) {
        return new HermesSkillLineageRepairOperationBatchQuery("", "", batchStatus, false);
    }

    public boolean matches(HermesSkillLineageRepairOperationBatch batch) {
        if (batch == null) {
            return false;
        }
        if (!backendId.isBlank() && !backendId.equals(normalize(batch.backendId()))) {
            return false;
        }
        if (!storageFamily.isBlank() && !storageFamily.equals(normalize(batch.storageFamily()))) {
            return false;
        }
        if (!batchStatus.isBlank() && !batchStatus.equals(normalize(batch.batchStatus()))) {
            return false;
        }
        return !adapterReadyOnly || batch.adapterReady();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backendId", backendId);
        values.put("storageFamily", storageFamily);
        values.put("batchStatus", batchStatus);
        values.put("adapterReadyOnly", adapterReadyOnly);
        return Map.copyOf(values);
    }

    private static String normalize(String value) {
        return value == null ? "" : HermesText.oneLine(value)
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }
}
