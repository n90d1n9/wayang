package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of selecting repair-operation batches for an adapter.
 */
public record HermesSkillLineageRepairOperationBatchSelection(
        HermesSkillLineageRepairOperationBatchQuery query,
        String selectionStatus,
        int batchCount,
        int operationCount,
        int adapterReadyBatchCount,
        int previewOnlyBatchCount,
        int unsupportedBatchCount,
        List<HermesSkillLineageRepairOperationBatch> batches) {

    public HermesSkillLineageRepairOperationBatchSelection {
        query = query == null ? HermesSkillLineageRepairOperationBatchQuery.all() : query;
        batches = HermesCollections.copyNonNull(batches);
        batchCount = batches.size();
        operationCount = batches.stream()
                .mapToInt(HermesSkillLineageRepairOperationBatch::operationCount)
                .sum();
        adapterReadyBatchCount = (int) batches.stream()
                .filter(HermesSkillLineageRepairOperationBatch::adapterReady)
                .count();
        previewOnlyBatchCount = (int) batches.stream()
                .filter(batch -> "preview-only".equals(batch.batchStatus()))
                .count();
        unsupportedBatchCount = (int) batches.stream()
                .filter(batch -> "unsupported".equals(batch.batchStatus()))
                .count();
        selectionStatus = HermesText.oneLineOr(selectionStatus, selectionStatus(
                batchCount,
                adapterReadyBatchCount,
                previewOnlyBatchCount,
                unsupportedBatchCount));
    }

    public static HermesSkillLineageRepairOperationBatchSelection from(
            HermesSkillLineageRepairOperationBatchQuery query,
            List<HermesSkillLineageRepairOperationBatch> batches) {
        return new HermesSkillLineageRepairOperationBatchSelection(
                query,
                "",
                0,
                0,
                0,
                0,
                0,
                batches);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("query", query.toMetadata());
        values.put("selectionStatus", selectionStatus);
        values.put("batchCount", batchCount);
        values.put("operationCount", operationCount);
        values.put("adapterReadyBatchCount", adapterReadyBatchCount);
        values.put("previewOnlyBatchCount", previewOnlyBatchCount);
        values.put("unsupportedBatchCount", unsupportedBatchCount);
        values.put("batches", batches.stream()
                .map(HermesSkillLineageRepairOperationBatch::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static String selectionStatus(
            int batchCount,
            int adapterReadyBatchCount,
            int previewOnlyBatchCount,
            int unsupportedBatchCount) {
        if (batchCount == 0) {
            return "empty";
        }
        if (adapterReadyBatchCount == batchCount) {
            return "adapter-ready";
        }
        if (previewOnlyBatchCount == batchCount) {
            return "preview-only";
        }
        if (unsupportedBatchCount == batchCount) {
            return "unsupported";
        }
        return "mixed";
    }
}
