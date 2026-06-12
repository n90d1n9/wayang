package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advisory learned-skill lineage inspection contract for Hermes requests.
 */
public record HermesSkillLineagePlan(
        boolean lineageEnabled,
        boolean requested,
        boolean inspect,
        String operation,
        String skillId,
        String target,
        String source,
        String reason,
        String repairAction,
        boolean repairApproved,
        String repairApprovalId,
        String repairIdempotencyKey,
        String repairBackendId,
        String repairStorageFamily,
        boolean repairAdapterReadyOnly) {

    public HermesSkillLineagePlan {
        skillId = HermesText.oneLineOr(skillId, "");
        operation = HermesText.oneLineOr(operation, inspect ? "inspect" : "none");
        inspect = inspect && (HermesSkillLineageDirective.globalOperation(operation) || !skillId.isBlank());
        if (!requested && !inspect) {
            operation = "none";
        }
        target = HermesText.oneLineOr(target, target(operation, skillId, inspect));
        source = HermesText.oneLineOr(source, "none");
        reason = HermesText.oneLineOr(reason, inspect ? "skill lineage inspection requested" : "no skill lineage requested");
        repairAction = HermesText.oneLineOr(repairAction, repairAction(operation));
        repairApprovalId = HermesText.oneLineOr(repairApprovalId, "");
        repairIdempotencyKey = HermesText.oneLineOr(repairIdempotencyKey, "");
        repairBackendId = HermesText.oneLineOr(repairBackendId, "");
        repairStorageFamily = HermesText.oneLineOr(repairStorageFamily, "");
    }

    public HermesSkillLineagePlan(
            boolean lineageEnabled,
            boolean requested,
            boolean inspect,
            String operation,
            String skillId,
            String target,
            String source,
            String reason) {
        this(
                lineageEnabled,
                requested,
                inspect,
                operation,
                skillId,
                target,
                source,
                reason,
                "",
                false,
                "",
                "",
                "",
                "",
                true);
    }

    public boolean active() {
        return lineageEnabled && inspect;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("lineageEnabled", lineageEnabled);
        metadata.put("requested", requested);
        metadata.put("inspect", inspect);
        metadata.put("active", active());
        metadata.put("operation", operation);
        metadata.put("skillId", skillId);
        metadata.put("target", target);
        metadata.put("source", source);
        metadata.put("reason", reason);
        metadata.put("repairAction", repairAction);
        metadata.put("repairApproved", repairApproved);
        metadata.put("repairApprovalId", repairApprovalId);
        metadata.put("repairIdempotencyKey", repairIdempotencyKey);
        metadata.put("repairBackendId", repairBackendId);
        metadata.put("repairStorageFamily", repairStorageFamily);
        metadata.put("repairAdapterReadyOnly", repairAdapterReadyOnly);
        return Map.copyOf(metadata);
    }

    private static String target(String operation, String skillId, boolean inspect) {
        if (!inspect) {
            return "";
        }
        return switch (operation) {
            case HermesSkillLineageDirective.CATALOG -> "learned-skills";
            case HermesSkillLineageDirective.REPAIR_PREVIEW -> "learned-skills:repair-preview";
            case HermesSkillLineageDirective.REPAIR_APPLY -> "learned-skills:repair-apply";
            case HermesSkillLineageDirective.REPAIR_ROLLBACK -> "learned-skills:repair-rollback";
            default -> "skill:" + skillId;
        };
    }

    private static String repairAction(String operation) {
        return switch (operation) {
            case HermesSkillLineageDirective.REPAIR_PREVIEW -> HermesSkillLineageRepairAdapter.PREVIEW;
            case HermesSkillLineageDirective.REPAIR_APPLY -> HermesSkillLineageRepairAdapter.APPLY;
            case HermesSkillLineageDirective.REPAIR_ROLLBACK -> HermesSkillLineageRepairAdapter.ROLLBACK;
            default -> "";
        };
    }
}
