package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral instruction for inspecting learned-skill lineage.
 */
public record HermesSkillLineageDirective(
        boolean active,
        String operation,
        String skillId,
        String target,
        String reason,
        String repairAction,
        boolean repairApproved,
        String repairApprovalId,
        String repairIdempotencyKey,
        String repairBackendId,
        String repairStorageFamily,
        boolean repairAdapterReadyOnly) {

    public static final String INSPECT = "inspect";
    public static final String CATALOG = "catalog";
    public static final String REPAIR_PREVIEW = "repair-preview";
    public static final String REPAIR_APPLY = "repair-apply";
    public static final String REPAIR_ROLLBACK = "repair-rollback";

    public HermesSkillLineageDirective {
        skillId = HermesDirectiveSupport.clean(skillId, "");
        String requestedOperation = operation(operation);
        active = active && (globalOperation(requestedOperation) || !skillId.isBlank());
        operation = active ? requestedOperation : "none";
        target = HermesDirectiveSupport.clean(target, target(active, operation, skillId));
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? reason(operation) : "skill lineage inspection inactive");
        repairAction = HermesDirectiveSupport.clean(repairAction, repairAction(operation));
        repairApprovalId = HermesDirectiveSupport.clean(repairApprovalId, "");
        repairIdempotencyKey = HermesDirectiveSupport.clean(repairIdempotencyKey, "");
        repairBackendId = HermesDirectiveSupport.clean(repairBackendId, "");
        repairStorageFamily = HermesDirectiveSupport.clean(repairStorageFamily, "");
    }

    public HermesSkillLineageDirective(
            boolean active,
            String operation,
            String skillId,
            String target,
            String reason) {
        this(
                active,
                operation,
                skillId,
                target,
                reason,
                "",
                false,
                "",
                "",
                "",
                "",
                true);
    }

    public static HermesSkillLineageDirective inspect(String skillId) {
        return new HermesSkillLineageDirective(
                true,
                INSPECT,
                skillId,
                "",
                "skill lineage inspection requested");
    }

    public static HermesSkillLineageDirective catalog() {
        return new HermesSkillLineageDirective(
                true,
                CATALOG,
                "",
                "",
                "skill lineage catalog requested");
    }

    public static HermesSkillLineageDirective repairPreview() {
        return new HermesSkillLineageDirective(
                true,
                REPAIR_PREVIEW,
                "",
                "",
                "skill lineage repair preview requested");
    }

    public static HermesSkillLineageDirective repairApply(
            String approvalId,
            String idempotencyKey) {
        return new HermesSkillLineageDirective(
                true,
                REPAIR_APPLY,
                "",
                "",
                "skill lineage repair apply requested",
                HermesSkillLineageRepairAdapter.APPLY,
                !HermesDirectiveSupport.clean(approvalId, "").isBlank(),
                approvalId,
                idempotencyKey,
                "",
                "",
                true);
    }

    public static HermesSkillLineageDirective repairRollback(
            String approvalId,
            String idempotencyKey) {
        return new HermesSkillLineageDirective(
                true,
                REPAIR_ROLLBACK,
                "",
                "",
                "skill lineage repair rollback requested",
                HermesSkillLineageRepairAdapter.ROLLBACK,
                !HermesDirectiveSupport.clean(approvalId, "").isBlank(),
                approvalId,
                idempotencyKey,
                "",
                "",
                true);
    }

    public static HermesSkillLineageDirective from(HermesSkillLineagePlan plan) {
        HermesSkillLineagePlan resolved = plan == null
                ? new HermesSkillLineageResolver(HermesAgentModeConfig.defaults()).defaultPlan()
                : plan;
        return new HermesSkillLineageDirective(
                resolved.active(),
                resolved.operation(),
                resolved.skillId(),
                resolved.target(),
                resolved.reason(),
                resolved.repairAction(),
                resolved.repairApproved(),
                resolved.repairApprovalId(),
                resolved.repairIdempotencyKey(),
                resolved.repairBackendId(),
                resolved.repairStorageFamily(),
                resolved.repairAdapterReadyOnly());
    }

    public static HermesSkillLineageDirective none() {
        return new HermesSkillLineageDirective(
                false,
                "none",
                "",
                "",
                "skill lineage inspection inactive");
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("skillId", skillId);
        metadata.put("target", target);
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

    private static String target(boolean active, String operation, String skillId) {
        if (!active) {
            return "";
        }
        return switch (operation) {
            case CATALOG -> "learned-skills";
            case REPAIR_PREVIEW -> "learned-skills:repair-preview";
            case REPAIR_APPLY -> "learned-skills:repair-apply";
            case REPAIR_ROLLBACK -> "learned-skills:repair-rollback";
            default -> "skill:" + skillId;
        };
    }

    private static String reason(String operation) {
        return switch (operation) {
            case CATALOG -> "skill lineage catalog requested";
            case REPAIR_PREVIEW -> "skill lineage repair preview requested";
            case REPAIR_APPLY -> "skill lineage repair apply requested";
            case REPAIR_ROLLBACK -> "skill lineage repair rollback requested";
            default -> "skill lineage inspection requested";
        };
    }

    static boolean globalOperation(String operation) {
        return CATALOG.equals(operation)
                || REPAIR_PREVIEW.equals(operation)
                || REPAIR_APPLY.equals(operation)
                || REPAIR_ROLLBACK.equals(operation);
    }

    static String operation(String value) {
        String normalized = HermesRequestValues.normalize(value);
        return switch (normalized) {
            case "catalog", "list", "library", "summary" -> CATALOG;
            case "repairpreview", "previewrepair", "repairadapterpreview", "adapterpreview",
                    "dispatchpreview", "previewdispatch" -> REPAIR_PREVIEW;
            case "repairapply", "applyrepair", "repairadapterapply", "adapterapply",
                    "dispatchapply", "applydispatch", "apply" -> REPAIR_APPLY;
            case "repairrollback", "rollbackrepair", "repairadapterrollback", "adapterrollback",
                    "dispatchrollback", "rollbackdispatch", "rollback" -> REPAIR_ROLLBACK;
            case "none", "off", "disabled", "false" -> "none";
            default -> INSPECT;
        };
    }

    private static String repairAction(String operation) {
        return switch (operation) {
            case REPAIR_PREVIEW -> HermesSkillLineageRepairAdapter.PREVIEW;
            case REPAIR_APPLY -> HermesSkillLineageRepairAdapter.APPLY;
            case REPAIR_ROLLBACK -> HermesSkillLineageRepairAdapter.ROLLBACK;
            default -> "";
        };
    }
}
