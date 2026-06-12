package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authorization decision for a lineage repair dispatch request.
 */
public record HermesSkillLineageRepairApprovalDecision(
        boolean mutationRequested,
        boolean approved,
        String status,
        String reason,
        HermesSkillLineageRepairApproval approval,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairApprovalDecision {
        status = HermesText.oneLineOr(status, approved ? "approved" : "rejected");
        reason = HermesText.oneLineOr(reason, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairApprovalDecision notRequired(
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        return new HermesSkillLineageRepairApprovalDecision(
                false,
                true,
                "not-required",
                "repair dispatch does not request mutation",
                null,
                requestMetadata(request));
    }

    public static HermesSkillLineageRepairApprovalDecision approved(
            HermesSkillLineageRepairApproval approval,
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        return new HermesSkillLineageRepairApprovalDecision(
                true,
                true,
                "approved",
                approval == null ? "lineage repair mutation approved" : approval.reason(),
                approval,
                requestMetadata(request));
    }

    public static HermesSkillLineageRepairApprovalDecision rejected(
            String status,
            String reason,
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        return new HermesSkillLineageRepairApprovalDecision(
                request != null && request.mutationRequested(),
                false,
                status,
                reason,
                null,
                requestMetadata(request));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mutationRequested", mutationRequested);
        values.put("approved", approved);
        values.put("status", status);
        values.put("reason", reason);
        values.put("approval", approval == null ? Map.of() : approval.toMetadata());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static Map<String, Object> requestMetadata(
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        return request == null ? Map.of() : request.toResultMetadata();
    }
}
