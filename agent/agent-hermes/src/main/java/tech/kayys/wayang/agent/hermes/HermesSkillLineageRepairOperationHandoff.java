package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Machine-readable dry-run handoff for future lineage repair adapters.
 */
public record HermesSkillLineageRepairOperationHandoff(
        int schemaVersion,
        String schema,
        String handoffId,
        boolean dryRun,
        String executionStatus,
        String handoffStatus,
        String policyMode,
        boolean mutationAllowed,
        boolean mutationSupported,
        boolean approvalRequired,
        boolean adapterReady,
        int operationCount,
        int mutationReadyOperationCount,
        int previewOnlyOperationCount,
        int unsupportedOperationCount,
        List<HermesSkillLineageRepairOperation> operations,
        int batchCount,
        int adapterReadyBatchCount,
        int previewOnlyBatchCount,
        int unsupportedBatchCount,
        List<HermesSkillLineageRepairOperationBatch> batches,
        Map<String, Object> metadata) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CURRENT_SCHEMA = "hermes.skill-lineage.repair.operation-handoff.v1";

    public HermesSkillLineageRepairOperationHandoff {
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        schema = HermesText.oneLineOr(schema, CURRENT_SCHEMA);
        executionStatus = HermesText.oneLineOr(executionStatus, "dry-run-noop");
        policyMode = HermesText.oneLineOr(policyMode, HermesSkillLineageRemediationPolicy.DRY_RUN);
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
        batches = batches == null || batches.isEmpty()
                ? batches(operations, dryRun, mutationAllowed, approvalRequired)
                : HermesCollections.copyNonNull(batches);
        batchCount = batches.size();
        adapterReadyBatchCount = (int) batches.stream()
                .filter(HermesSkillLineageRepairOperationBatch::adapterReady)
                .count();
        previewOnlyBatchCount = (int) batches.stream()
                .filter(batch -> "preview-only".equals(batch.batchStatus()))
                .count();
        unsupportedBatchCount = (int) batches.stream()
                .filter(batch -> "unsupported".equals(batch.batchStatus()))
                .count();
        adapterReady = adapterReady || adapterReady(
                operationCount,
                mutationReadyOperationCount,
                mutationAllowed,
                mutationSupported);
        handoffStatus = HermesText.oneLineOr(handoffStatus, handoffStatus(
                operationCount,
                mutationReadyOperationCount,
                previewOnlyOperationCount,
                unsupportedOperationCount,
                mutationAllowed,
                approvalRequired));
        handoffId = HermesText.oneLineOr(handoffId, handoffId(
                policyMode,
                handoffStatus,
                operationCount,
                mutationReadyOperationCount));
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairOperationHandoff from(
            HermesSkillLineageRemediationExecution execution) {
        HermesSkillLineageRemediationExecution resolved = execution == null
                ? HermesSkillLineageRemediationExecution.dryRun(
                        HermesSkillLineageRemediationPlan.none(),
                        HermesSkillStoreConsistencyReport.empty())
                : execution;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("source", "skill-lineage-remediation-dry-run");
        values.put("strategy", resolved.strategy());
        values.put("repairIntentCount", resolved.repairIntentPlan().intentCount());
        values.put("backendCount", resolved.repairBackendPlan().backendCount());
        return new HermesSkillLineageRepairOperationHandoff(
                CURRENT_SCHEMA_VERSION,
                CURRENT_SCHEMA,
                "",
                resolved.dryRun(),
                resolved.status(),
                "",
                resolved.policy().mode(),
                resolved.mutationAllowed(),
                resolved.mutationSupported(),
                resolved.approvalRequired(),
                false,
                0,
                0,
                0,
                0,
                resolved.repairBackendPlan().operations(),
                0,
                0,
                0,
                0,
                List.of(),
                values);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schemaVersion", schemaVersion);
        values.put("schema", schema);
        values.put("handoffId", handoffId);
        values.put("dryRun", dryRun);
        values.put("executionStatus", executionStatus);
        values.put("handoffStatus", handoffStatus);
        values.put("policyMode", policyMode);
        values.put("mutationAllowed", mutationAllowed);
        values.put("mutationSupported", mutationSupported);
        values.put("approvalRequired", approvalRequired);
        values.put("adapterReady", adapterReady);
        values.put("operationCount", operationCount);
        values.put("mutationReadyOperationCount", mutationReadyOperationCount);
        values.put("previewOnlyOperationCount", previewOnlyOperationCount);
        values.put("unsupportedOperationCount", unsupportedOperationCount);
        values.put("batchCount", batchCount);
        values.put("adapterReadyBatchCount", adapterReadyBatchCount);
        values.put("previewOnlyBatchCount", previewOnlyBatchCount);
        values.put("unsupportedBatchCount", unsupportedBatchCount);
        values.put("operations", operations.stream()
                .map(HermesSkillLineageRepairOperation::toMetadata)
                .toList());
        values.put("batches", batches.stream()
                .map(HermesSkillLineageRepairOperationBatch::toMetadata)
                .toList());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    public HermesSkillLineageRepairOperationBatchSelection selectBatches(
            HermesSkillLineageRepairOperationBatchQuery query) {
        HermesSkillLineageRepairOperationBatchQuery resolved =
                query == null ? HermesSkillLineageRepairOperationBatchQuery.all() : query;
        return HermesSkillLineageRepairOperationBatchSelection.from(
                resolved,
                batches.stream()
                        .filter(resolved::matches)
                        .toList());
    }

    private static List<HermesSkillLineageRepairOperationBatch> batches(
            List<HermesSkillLineageRepairOperation> operations,
            boolean dryRun,
            boolean mutationAllowed,
            boolean approvalRequired) {
        Map<String, List<HermesSkillLineageRepairOperation>> grouped = new LinkedHashMap<>();
        Map<String, String> backends = new LinkedHashMap<>();
        Map<String, String> storageFamilies = new LinkedHashMap<>();
        for (HermesSkillLineageRepairOperation operation : operations) {
            String backendId = HermesText.oneLineOr(operation.backendId(), "no-backend");
            String storageFamily = HermesText.oneLineOr(operation.storageFamily(), "none");
            String key = backendId + "::" + storageFamily;
            grouped.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(operation);
            backends.putIfAbsent(key, backendId);
            storageFamilies.putIfAbsent(key, storageFamily);
        }
        return grouped.entrySet().stream()
                .map(entry -> HermesSkillLineageRepairOperationBatch.from(
                        backends.get(entry.getKey()),
                        storageFamilies.get(entry.getKey()),
                        dryRun,
                        mutationAllowed,
                        approvalRequired,
                        entry.getValue()))
                .toList();
    }

    private static boolean adapterReady(
            int operationCount,
            int mutationReadyOperationCount,
            boolean mutationAllowed,
            boolean mutationSupported) {
        return operationCount > 0
                && mutationAllowed
                && mutationSupported
                && mutationReadyOperationCount == operationCount;
    }

    private static String handoffStatus(
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

    private static String handoffId(
            String policyMode,
            String handoffStatus,
            int operationCount,
            int mutationReadyOperationCount) {
        return "handoff-%s-%s-%d-%d".formatted(
                HermesText.oneLineOr(policyMode, HermesSkillLineageRemediationPolicy.DRY_RUN),
                HermesText.oneLineOr(handoffStatus, "empty"),
                Math.max(operationCount, 0),
                Math.max(mutationReadyOperationCount, 0));
    }
}
