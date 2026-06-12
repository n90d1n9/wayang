package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Approval and idempotency envelope for dispatching repair batches to adapters.
 */
public record HermesSkillLineageRepairAdapterDispatchRequest(
        String action,
        HermesSkillLineageRepairOperationBatchSelection selection,
        boolean approved,
        String approvalId,
        String idempotencyKey,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairAdapterDispatchRequest {
        action = HermesSkillLineageRepairAdapterCapabilities.normalize(
                action,
                HermesSkillLineageRepairAdapter.PREVIEW);
        selection = selection == null
                ? HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.all(),
                        List.of())
                : selection;
        approvalId = HermesText.oneLineOr(approvalId, "");
        idempotencyKey = HermesText.oneLineOr(idempotencyKey, idempotencyKey(action, selection));
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairAdapterDispatchRequest preview(
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.PREVIEW,
                selection,
                false,
                "",
                "",
                Map.of());
    }

    public static HermesSkillLineageRepairAdapterDispatchRequest apply(
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.APPLY,
                selection,
                false,
                "",
                "",
                Map.of());
    }

    public static HermesSkillLineageRepairAdapterDispatchRequest approvedApply(
            HermesSkillLineageRepairOperationBatchSelection selection,
            String approvalId) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.APPLY,
                selection,
                true,
                approvalId,
                "",
                Map.of());
    }

    public static HermesSkillLineageRepairAdapterDispatchRequest rollback(
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.ROLLBACK,
                selection,
                false,
                "",
                "",
                Map.of());
    }

    public static HermesSkillLineageRepairAdapterDispatchRequest approvedRollback(
            HermesSkillLineageRepairOperationBatchSelection selection,
            String approvalId) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.ROLLBACK,
                selection,
                true,
                approvalId,
                "",
                Map.of());
    }

    public boolean mutationRequested() {
        return HermesSkillLineageRepairAdapter.APPLY.equals(action)
                || HermesSkillLineageRepairAdapter.ROLLBACK.equals(action);
    }

    public boolean approvalSatisfied() {
        return !mutationRequested() || approved && !approvalId.isBlank();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>(toResultMetadata());
        values.put("selection", selection.toMetadata());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    public Map<String, Object> toResultMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", action);
        values.put("approved", approved);
        values.put("approvalId", approvalId);
        values.put("idempotencyKey", idempotencyKey);
        values.put("mutationRequested", mutationRequested());
        values.put("approvalSatisfied", approvalSatisfied());
        values.put("selectionStatus", selection.selectionStatus());
        values.put("batchCount", selection.batchCount());
        values.put("operationCount", selection.operationCount());
        return Map.copyOf(values);
    }

    private static String idempotencyKey(
            String action,
            HermesSkillLineageRepairOperationBatchSelection selection) {
        String batchIds = String.join(
                ",",
                selection.batches().stream()
                        .map(HermesSkillLineageRepairOperationBatch::batchId)
                        .toList());
        String signature = "%s|%s|%s|%s|%s|%d|%d|%s".formatted(
                action,
                selection.query().backendId(),
                selection.query().storageFamily(),
                selection.query().batchStatus(),
                selection.selectionStatus(),
                selection.batchCount(),
                selection.operationCount(),
                batchIds);
        return "repair-dispatch-" + Integer.toUnsignedString(signature.hashCode(), 36);
    }
}
