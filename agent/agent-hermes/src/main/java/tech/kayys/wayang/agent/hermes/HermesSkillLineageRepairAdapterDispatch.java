package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate dispatch outcome for an adapter-routed batch selection.
 */
public record HermesSkillLineageRepairAdapterDispatch(
        String action,
        String dispatchStatus,
        int batchCount,
        int dispatchedBatchCount,
        int successfulBatchCount,
        int failedBatchCount,
        int unsupportedBatchCount,
        List<HermesSkillLineageRepairAdapterResult> results,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairAdapterDispatch {
        action = HermesSkillLineageRepairAdapterCapabilities.normalize(action, HermesSkillLineageRepairAdapter.PREVIEW);
        results = HermesCollections.copyNonNull(results);
        batchCount = results.size();
        dispatchedBatchCount = (int) results.stream()
                .filter(HermesSkillLineageRepairAdapterResult::dispatched)
                .count();
        successfulBatchCount = (int) results.stream()
                .filter(HermesSkillLineageRepairAdapterResult::successful)
                .count();
        failedBatchCount = (int) results.stream()
                .filter(result -> result.dispatched() && !result.successful())
                .count();
        unsupportedBatchCount = (int) results.stream()
                .filter(result -> !result.dispatched())
                .count();
        dispatchStatus = HermesText.oneLineOr(dispatchStatus, dispatchStatus(
                batchCount,
                dispatchedBatchCount,
                successfulBatchCount,
                failedBatchCount,
                unsupportedBatchCount));
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairAdapterDispatch from(
            String action,
            List<HermesSkillLineageRepairAdapterResult> results,
            Map<String, Object> metadata) {
        return new HermesSkillLineageRepairAdapterDispatch(
                action,
                "",
                0,
                0,
                0,
                0,
                0,
                results,
                metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", action);
        values.put("dispatchStatus", dispatchStatus);
        values.put("batchCount", batchCount);
        values.put("dispatchedBatchCount", dispatchedBatchCount);
        values.put("successfulBatchCount", successfulBatchCount);
        values.put("failedBatchCount", failedBatchCount);
        values.put("unsupportedBatchCount", unsupportedBatchCount);
        values.put("results", results.stream()
                .map(HermesSkillLineageRepairAdapterResult::toMetadata)
                .toList());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static String dispatchStatus(
            int batchCount,
            int dispatchedBatchCount,
            int successfulBatchCount,
            int failedBatchCount,
            int unsupportedBatchCount) {
        if (batchCount == 0) {
            return "empty";
        }
        if (successfulBatchCount == batchCount) {
            return "dispatched";
        }
        if (dispatchedBatchCount == 0 && unsupportedBatchCount == batchCount) {
            return "unsupported";
        }
        if (successfulBatchCount > 0 || dispatchedBatchCount > 0 && failedBatchCount == 0) {
            return "partial";
        }
        return failedBatchCount > 0 ? "failed" : "unsupported";
    }
}
