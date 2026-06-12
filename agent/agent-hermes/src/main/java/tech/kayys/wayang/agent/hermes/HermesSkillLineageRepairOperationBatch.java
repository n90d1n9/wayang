package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend-routed subset of repair operations for a future adapter.
 */
public record HermesSkillLineageRepairOperationBatch(
        String batchId,
        String backendId,
        String storageFamily,
        boolean dryRun,
        boolean mutationAllowed,
        boolean mutationSupported,
        boolean approvalRequired,
        boolean adapterReady,
        String batchStatus,
        int operationCount,
        int mutationReadyOperationCount,
        int previewOnlyOperationCount,
        int unsupportedOperationCount,
        List<HermesSkillLineageRepairOperation> operations,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairOperationBatch {
        backendId = HermesText.oneLineOr(backendId, "no-backend");
        storageFamily = HermesText.oneLineOr(storageFamily, "none");
        operations = HermesCollections.copyNonNull(operations);
        operationCount = operations.size();
        mutationReadyOperationCount = (int) operations.stream()
                .filter(HermesSkillLineageRepairOperation::mutationReady)
                .count();
        previewOnlyOperationCount = (int) operations.stream()
                .filter(operation -> "preview-only".equals(operation.status()))
                .count();
        unsupportedOperationCount = (int) operations.stream()
                .filter(operation -> "unsupported".equals(operation.status()))
                .count();
        mutationSupported = mutationSupported || operationCount > 0
                && mutationReadyOperationCount == operationCount;
        adapterReady = adapterReady || operationCount > 0
                && mutationAllowed
                && mutationSupported;
        batchStatus = HermesText.oneLineOr(batchStatus, batchStatus(
                operationCount,
                mutationReadyOperationCount,
                previewOnlyOperationCount,
                unsupportedOperationCount,
                mutationAllowed,
                approvalRequired));
        batchId = HermesText.oneLineOr(batchId, batchId(
                backendId,
                storageFamily,
                batchStatus,
                operationCount,
                mutationReadyOperationCount));
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairOperationBatch from(
            String backendId,
            String storageFamily,
            boolean dryRun,
            boolean mutationAllowed,
            boolean approvalRequired,
            List<HermesSkillLineageRepairOperation> operations) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("source", "skill-lineage-repair-operation-handoff");
        return new HermesSkillLineageRepairOperationBatch(
                "",
                backendId,
                storageFamily,
                dryRun,
                mutationAllowed,
                false,
                approvalRequired,
                false,
                "",
                0,
                0,
                0,
                0,
                operations,
                values);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("batchId", batchId);
        values.put("backendId", backendId);
        values.put("storageFamily", storageFamily);
        values.put("dryRun", dryRun);
        values.put("mutationAllowed", mutationAllowed);
        values.put("mutationSupported", mutationSupported);
        values.put("approvalRequired", approvalRequired);
        values.put("adapterReady", adapterReady);
        values.put("batchStatus", batchStatus);
        values.put("operationCount", operationCount);
        values.put("mutationReadyOperationCount", mutationReadyOperationCount);
        values.put("previewOnlyOperationCount", previewOnlyOperationCount);
        values.put("unsupportedOperationCount", unsupportedOperationCount);
        values.put("operations", operations.stream()
                .map(HermesSkillLineageRepairOperation::toMetadata)
                .toList());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static String batchStatus(
            int operationCount,
            int mutationReadyOperationCount,
            int previewOnlyOperationCount,
            int unsupportedOperationCount,
            boolean mutationAllowed,
            boolean approvalRequired) {
        if (operationCount == 0) {
            return "empty";
        }
        if (!mutationAllowed) {
            return "policy-blocked";
        }
        if (unsupportedOperationCount > 0) {
            return "unsupported";
        }
        if (mutationReadyOperationCount == operationCount) {
            return approvalRequired ? "awaiting-approval" : "adapter-ready";
        }
        return previewOnlyOperationCount > 0 ? "preview-only" : "unsupported";
    }

    private static String batchId(
            String backendId,
            String storageFamily,
            String batchStatus,
            int operationCount,
            int mutationReadyOperationCount) {
        return "batch-%s-%s-%s-%d-%d".formatted(
                HermesText.oneLineOr(backendId, "no-backend"),
                HermesText.oneLineOr(storageFamily, "none"),
                HermesText.oneLineOr(batchStatus, "empty"),
                Math.max(operationCount, 0),
                Math.max(mutationReadyOperationCount, 0));
    }
}
