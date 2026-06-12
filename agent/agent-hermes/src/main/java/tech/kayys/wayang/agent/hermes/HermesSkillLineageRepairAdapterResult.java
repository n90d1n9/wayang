package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result returned by a concrete lineage repair adapter for one routed batch.
 */
public record HermesSkillLineageRepairAdapterResult(
        String adapterId,
        String action,
        String batchId,
        String backendId,
        String storageFamily,
        int operationCount,
        boolean active,
        boolean dispatched,
        boolean successful,
        boolean mutationAttempted,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairAdapterResult {
        adapterId = HermesSkillLineageRepairAdapterCapabilities.normalize(adapterId, "repair-adapter");
        action = HermesSkillLineageRepairAdapterCapabilities.normalize(action, HermesSkillLineageRepairAdapter.PREVIEW);
        batchId = HermesText.oneLineOr(batchId, "no-batch");
        backendId = HermesSkillLineageRepairAdapterCapabilities.normalize(backendId, "no-backend");
        storageFamily = HermesSkillLineageRepairAdapterCapabilities.normalize(storageFamily, "none");
        operationCount = Math.max(operationCount, 0);
        status = HermesSkillLineageRepairAdapterCapabilities.normalize(status, successful ? "ok" : "failed");
        reason = HermesText.oneLineOr(reason, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairAdapterResult dispatched(
            String adapterId,
            String action,
            HermesSkillLineageRepairOperationBatch batch,
            String status,
            String reason,
            Map<String, Object> metadata) {
        return new HermesSkillLineageRepairAdapterResult(
                adapterId,
                action,
                batchId(batch),
                backendId(batch),
                storageFamily(batch),
                operationCount(batch),
                true,
                true,
                true,
                mutationAttempted(action),
                status,
                reason,
                metadata);
    }

    public static HermesSkillLineageRepairAdapterResult unavailable(
            String adapterId,
            String action,
            HermesSkillLineageRepairOperationBatch batch,
            String reason,
            Map<String, Object> metadata) {
        return new HermesSkillLineageRepairAdapterResult(
                adapterId,
                action,
                batchId(batch),
                backendId(batch),
                storageFamily(batch),
                operationCount(batch),
                true,
                false,
                false,
                false,
                "unavailable",
                reason,
                metadata);
    }

    public static HermesSkillLineageRepairAdapterResult failed(
            String adapterId,
            String action,
            HermesSkillLineageRepairOperationBatch batch,
            String reason,
            Map<String, Object> metadata) {
        return new HermesSkillLineageRepairAdapterResult(
                adapterId,
                action,
                batchId(batch),
                backendId(batch),
                storageFamily(batch),
                operationCount(batch),
                true,
                true,
                false,
                mutationAttempted(action),
                "failed",
                reason,
                metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("adapterId", adapterId);
        values.put("action", action);
        values.put("batchId", batchId);
        values.put("backendId", backendId);
        values.put("storageFamily", storageFamily);
        values.put("operationCount", operationCount);
        values.put("active", active);
        values.put("dispatched", dispatched);
        values.put("successful", successful);
        values.put("mutationAttempted", mutationAttempted);
        values.put("status", status);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static boolean mutationAttempted(String action) {
        String normalized = HermesSkillLineageRepairAdapterCapabilities.normalize(
                action,
                HermesSkillLineageRepairAdapter.PREVIEW);
        return HermesSkillLineageRepairAdapter.APPLY.equals(normalized)
                || HermesSkillLineageRepairAdapter.ROLLBACK.equals(normalized);
    }

    private static String batchId(HermesSkillLineageRepairOperationBatch batch) {
        return batch == null ? "no-batch" : batch.batchId();
    }

    private static String backendId(HermesSkillLineageRepairOperationBatch batch) {
        return batch == null ? "no-backend" : batch.backendId();
    }

    private static String storageFamily(HermesSkillLineageRepairOperationBatch batch) {
        return batch == null ? "none" : batch.storageFamily();
    }

    private static int operationCount(HermesSkillLineageRepairOperationBatch batch) {
        return batch == null ? 0 : batch.operationCount();
    }
}
