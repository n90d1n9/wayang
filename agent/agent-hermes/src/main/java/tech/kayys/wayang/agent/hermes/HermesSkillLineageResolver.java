package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves learned-skill lineage inspection intent for Hermes requests.
 */
public final class HermesSkillLineageResolver {

    private static final Pattern SKILL_ID_ASSIGNMENT = Pattern.compile(
            "\\b(?:skill(?:\\s+id)?|skill-id)\\s*[:=]\\s*([A-Za-z0-9][A-Za-z0-9._:-]{1,127})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LINEAGE_FOR_SKILL = Pattern.compile(
            "\\blineage\\s+(?:for|of)\\s+(?:skill\\s+)?([A-Za-z0-9][A-Za-z0-9._:-]{1,127})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INSPECT_SKILL_LINEAGE = Pattern.compile(
            "\\binspect\\s+(?:skill\\s+)?([A-Za-z0-9][A-Za-z0-9._:-]{1,127})\\s+lineage\\b",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> LINEAGE_KEYS = List.of(
            "hermes.skill.lineage",
            "hermes.skillLineage",
            "skill.lineage",
            "skillLineage",
            "lineage.inspect",
            "inspectSkillLineage");

    private static final List<String> CATALOG_KEYS = List.of(
            "hermes.skill.catalog",
            "hermes.skillLineage.catalog",
            "skill.catalog",
            "skillLineage.catalog",
            "learnedSkillCatalog",
            "catalogSkills");

    private static final List<String> OPERATION_KEYS = List.of(
            "hermes.skill.lineage.operation",
            "hermes.skillLineage.operation",
            "skill.lineage.operation",
            "skillLineage.operation",
            "skillLineageOperation");

    private static final List<String> SKILL_ID_KEYS = List.of(
            "hermes.skill.id",
            "hermes.skillId",
            "hermes.skillLineage.skillId",
            "skill.id",
            "skillId",
            "skill.lineage.skillId",
            "skillLineage.skillId",
            "skillLineageSkillId");

    private static final List<String> REPAIR_APPROVED_KEYS = List.of(
            "hermes.skill.lineage.repair.approved",
            "hermes.skillLineage.repair.approved",
            "skill.lineage.repair.approved",
            "skillLineage.repair.approved",
            "skillLineageRepairApproved");

    private static final List<String> REPAIR_APPROVAL_ID_KEYS = List.of(
            "hermes.skill.lineage.repair.approvalId",
            "hermes.skillLineage.repair.approvalId",
            "skill.lineage.repair.approvalId",
            "skillLineage.repair.approvalId",
            "skillLineageRepairApprovalId",
            "repairApprovalId");

    private static final List<String> REPAIR_IDEMPOTENCY_KEY_KEYS = List.of(
            "hermes.skill.lineage.repair.idempotencyKey",
            "hermes.skillLineage.repair.idempotencyKey",
            "skill.lineage.repair.idempotencyKey",
            "skillLineage.repair.idempotencyKey",
            "skillLineageRepairIdempotencyKey",
            "repairIdempotencyKey");

    private static final List<String> REPAIR_BACKEND_ID_KEYS = List.of(
            "hermes.skill.lineage.repair.backendId",
            "hermes.skillLineage.repair.backendId",
            "skill.lineage.repair.backendId",
            "skillLineage.repair.backendId",
            "skillLineageRepairBackendId",
            "repairBackendId");

    private static final List<String> REPAIR_STORAGE_FAMILY_KEYS = List.of(
            "hermes.skill.lineage.repair.storageFamily",
            "hermes.skillLineage.repair.storageFamily",
            "skill.lineage.repair.storageFamily",
            "skillLineage.repair.storageFamily",
            "skillLineageRepairStorageFamily",
            "repairStorageFamily");

    private static final List<String> REPAIR_ADAPTER_READY_ONLY_KEYS = List.of(
            "hermes.skill.lineage.repair.adapterReadyOnly",
            "hermes.skillLineage.repair.adapterReadyOnly",
            "skill.lineage.repair.adapterReadyOnly",
            "skillLineage.repair.adapterReadyOnly",
            "skillLineageRepairAdapterReadyOnly");

    private final HermesAgentModeConfig config;

    public HermesSkillLineageResolver(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesSkillLineagePlan resolve(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        Optional<Boolean> explicitLineage = values.firstBoolean(LINEAGE_KEYS, "skill lineage");
        Optional<Boolean> explicitCatalog = values.firstBoolean(CATALOG_KEYS, "skill lineage catalog");
        Optional<String> explicitOperation = values.firstText(OPERATION_KEYS).map(HermesSkillLineageResolver::operation);
        Optional<String> explicitSkillId = values.firstText(SKILL_ID_KEYS)
                .map(HermesSkillLineageResolver::cleanSkillId)
                .filter(value -> !value.isBlank());
        boolean repairApproved = values.firstBoolean(
                REPAIR_APPROVED_KEYS,
                "skill lineage repair approved").orElse(false);
        String repairApprovalId = values.firstText(REPAIR_APPROVAL_ID_KEYS)
                .map(HermesText::oneLine)
                .orElse("");
        String repairIdempotencyKey = values.firstText(REPAIR_IDEMPOTENCY_KEY_KEYS)
                .map(HermesText::oneLine)
                .orElse("");
        String repairBackendId = values.firstText(REPAIR_BACKEND_ID_KEYS)
                .map(HermesText::oneLine)
                .orElse("");
        String repairStorageFamily = values.firstText(REPAIR_STORAGE_FAMILY_KEYS)
                .map(HermesText::oneLine)
                .orElse("");
        boolean repairAdapterReadyOnly = values.firstBoolean(
                REPAIR_ADAPTER_READY_ONLY_KEYS,
                "skill lineage repair adapter-ready only").orElse(true);
        Optional<String> promptSkillId = promptSkillId(prompt);
        Optional<String> skillId = explicitSkillId.or(() -> promptSkillId);

        boolean promptCatalog = suggestsCatalog(prompt);
        boolean promptRepairPreview = suggestsRepairPreview(prompt);
        boolean promptInspect = suggestsInspection(prompt);
        boolean catalogRequested = explicitCatalog.orElse(false)
                || explicitOperation.filter("catalog"::equals).isPresent()
                || promptCatalog;
        boolean repairPreviewRequested = explicitOperation
                .filter(HermesSkillLineageDirective.REPAIR_PREVIEW::equals)
                .isPresent()
                || promptRepairPreview;
        boolean repairMutationRequested = explicitOperation
                .filter(operation -> HermesSkillLineageDirective.REPAIR_APPLY.equals(operation)
                        || HermesSkillLineageDirective.REPAIR_ROLLBACK.equals(operation))
                .isPresent();
        boolean inspectRequested = explicitLineage.orElse(false)
                || explicitOperation.filter(HermesSkillLineageDirective.INSPECT::equals).isPresent()
                || skillId.isPresent()
                || promptInspect;
        boolean disabledByRequest = explicitLineage.isPresent() && !explicitLineage.orElseThrow()
                && !catalogRequested
                && !repairPreviewRequested
                && !repairMutationRequested
                && explicitOperation.isEmpty()
                && skillId.isEmpty();
        boolean requested = !disabledByRequest
                && (catalogRequested || repairPreviewRequested || repairMutationRequested || inspectRequested);
        String operation = operation(
                explicitOperation,
                requested,
                catalogRequested,
                repairPreviewRequested,
                skillId);
        String source = source(
                explicitLineage,
                explicitCatalog,
                explicitOperation,
                explicitSkillId,
                promptCatalog || promptRepairPreview || promptInspect || promptSkillId.isPresent());
        boolean inspect = requested && !"none".equals(operation);

        if (!config.skillLearningEnabled()) {
            return new HermesSkillLineagePlan(
                    false,
                    requested,
                    false,
                    operation,
                    skillId.orElse(""),
                    "",
                    source,
                    "skill lineage disabled because skill learning is disabled",
                    "",
                    false,
                    "",
                    "",
                    "",
                    "",
                    true);
        }

        if (disabledByRequest || !requested) {
            return new HermesSkillLineagePlan(
                    true,
                    false,
                    false,
                    "none",
                    "",
                    "",
                    disabledByRequest ? "explicit" : "none",
                    disabledByRequest ? "skill lineage disabled for request" : "no skill lineage requested",
                    "",
                    false,
                    "",
                    "",
                    "",
                    "",
                    true);
        }

        return new HermesSkillLineagePlan(
                true,
                true,
                inspect,
                operation,
                skillId.orElse(""),
                "",
                source,
                reason(operation),
                "",
                repairApproved,
                repairApprovalId,
                repairIdempotencyKey,
                repairBackendId,
                repairStorageFamily,
                repairAdapterReadyOnly);
    }

    public HermesSkillLineagePlan defaultPlan() {
        return resolve(null);
    }

    private static String operation(
            Optional<String> explicitOperation,
            boolean requested,
            boolean catalogRequested,
            boolean repairPreviewRequested,
            Optional<String> skillId) {
        if (!requested) {
            return "none";
        }
        if (explicitOperation.isPresent()) {
            return explicitOperation.orElseThrow();
        }
        if (repairPreviewRequested) {
            return HermesSkillLineageDirective.REPAIR_PREVIEW;
        }
        if (catalogRequested || skillId.isEmpty()) {
            return HermesSkillLineageDirective.CATALOG;
        }
        return HermesSkillLineageDirective.INSPECT;
    }

    private static String operation(String value) {
        return HermesSkillLineageDirective.operation(value);
    }

    private static String source(
            Optional<Boolean> explicitLineage,
            Optional<Boolean> explicitCatalog,
            Optional<String> explicitOperation,
            Optional<String> skillId,
            boolean promptSignal) {
        if (explicitLineage.isPresent() || explicitCatalog.isPresent()
                || explicitOperation.isPresent() || skillId.isPresent()) {
            return "explicit";
        }
        return promptSignal ? "prompt" : "none";
    }

    private static Optional<String> promptSkillId(String prompt) {
        for (Pattern pattern : List.of(SKILL_ID_ASSIGNMENT, LINEAGE_FOR_SKILL, INSPECT_SKILL_LINEAGE)) {
            Matcher matcher = pattern.matcher(HermesText.oneLine(prompt));
            if (matcher.find()) {
                String skillId = cleanSkillId(matcher.group(1));
                if (!skillId.isBlank()) {
                    return Optional.of(skillId);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean suggestsCatalog(String prompt) {
        return HermesRequestValues.containsAny(
                HermesRequestValues.normalizeText(prompt),
                "learned skill catalog",
                "learned skills",
                "skill catalog",
                "lineage catalog",
                "show skill lineage",
                "list skill lineage",
                "list learned skills",
                "show learned skills",
                "skill library",
                "learned skill library");
    }

    private static boolean suggestsInspection(String prompt) {
        return HermesRequestValues.containsAny(
                HermesRequestValues.normalizeText(prompt),
                "skill lineage",
                "inspect lineage",
                "lineage for",
                "lineage of",
                "skill history",
                "revision history");
    }

    private static boolean suggestsRepairPreview(String prompt) {
        return HermesRequestValues.containsAny(
                HermesRequestValues.normalizeText(prompt),
                "skill lineage repair preview",
                "lineage repair preview",
                "preview skill lineage repair",
                "preview lineage repair",
                "preview repair adapters",
                "repair adapter preview");
    }

    private static String reason(String operation) {
        return switch (operation) {
            case HermesSkillLineageDirective.CATALOG -> "skill lineage catalog requested";
            case HermesSkillLineageDirective.REPAIR_PREVIEW -> "skill lineage repair preview requested";
            case HermesSkillLineageDirective.REPAIR_APPLY -> "skill lineage repair apply requested";
            case HermesSkillLineageDirective.REPAIR_ROLLBACK -> "skill lineage repair rollback requested";
            default -> "skill lineage inspection requested";
        };
    }

    private static String cleanSkillId(String value) {
        String clean = HermesText.oneLine(value).replaceAll("[,.;:]+$", "");
        String normalized = HermesRequestValues.normalize(clean);
        if (List.of("for", "of", "skill", "lineage", "catalog", "history", "library").contains(normalized)) {
            return "";
        }
        return clean;
    }
}
