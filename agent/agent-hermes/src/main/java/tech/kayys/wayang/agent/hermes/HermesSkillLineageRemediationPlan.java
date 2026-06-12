package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only remediation plan derived from learned-skill lineage health.
 */
public record HermesSkillLineageRemediationPlan(
        boolean required,
        String strategy,
        int actionCount,
        int requiredActionCount,
        int advisoryActionCount,
        List<HermesSkillLineageRemediationAction> actions) {

    public HermesSkillLineageRemediationPlan {
        actions = HermesCollections.copyNonNull(actions);
        actionCount = actions.size();
        requiredActionCount = (int) actions.stream()
                .filter(HermesSkillLineageRemediationAction::required)
                .count();
        advisoryActionCount = actionCount - requiredActionCount;
        required = required || requiredActionCount > 0;
        strategy = HermesText.oneLineOr(strategy, required ? "repair-lineage" : actionCount > 0 ? "lineage-advisory" : "none");
    }

    public static HermesSkillLineageRemediationPlan none() {
        return new HermesSkillLineageRemediationPlan(false, "none", 0, 0, 0, List.of());
    }

    public static HermesSkillLineageRemediationPlan from(HermesSkillLineageHealth health) {
        if (health == null) {
            return none();
        }
        if ("attention".equals(health.status())) {
            return new HermesSkillLineageRemediationPlan(
                    true,
                    "repair-lineage-roots",
                    0,
                    0,
                    0,
                    attentionActions(health));
        }
        if ("evolving".equals(health.status())) {
            return new HermesSkillLineageRemediationPlan(
                    false,
                    "review-refined-skill-quality",
                    0,
                    0,
                    0,
                    health.refinedRootSkillIds().stream()
                            .map(root -> new HermesSkillLineageRemediationAction(
                                    "review-refined-skill-quality",
                                    "info",
                                    false,
                                    false,
                                    "lineage-root",
                                    root,
                                    "refined learned skill root should be periodically reviewed",
                                    Map.of("rootSkillId", root)))
                            .toList());
        }
        if ("empty".equals(health.status())) {
            return new HermesSkillLineageRemediationPlan(
                    false,
                    "bootstrap-learned-skill-library",
                    0,
                    0,
                    0,
                    List.of(new HermesSkillLineageRemediationAction(
                            "distill-first-successful-complex-workflow",
                            "info",
                            false,
                            false,
                            "catalog",
                            "learned-skills",
                            "no learned skills have been cataloged yet",
                            Map.of("learnedSkillCount", health.learnedSkillCount()))));
        }
        return none();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("required", required);
        values.put("strategy", strategy);
        values.put("actionCount", actionCount);
        values.put("requiredActionCount", requiredActionCount);
        values.put("advisoryActionCount", advisoryActionCount);
        values.put("actions", actions.stream()
                .map(HermesSkillLineageRemediationAction::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static List<HermesSkillLineageRemediationAction> attentionActions(HermesSkillLineageHealth health) {
        List<HermesSkillLineageRemediationAction> actions = new java.util.ArrayList<>();
        for (String rootSkillId : health.orphanedRootSkillIds()) {
            actions.add(new HermesSkillLineageRemediationAction(
                    "repair-orphaned-lineage-root",
                    "critical",
                    true,
                    false,
                    "lineage-root",
                    rootSkillId,
                    "lineage root is referenced by refined skills but the root entry is missing",
                    Map.of("rootSkillId", rootSkillId)));
        }
        actions.add(new HermesSkillLineageRemediationAction(
                "inspect-learned-skill-storage-consistency",
                "warning",
                true,
                false,
                "catalog",
                "learned-skills",
                "verify file, database, and object-storage indexes agree for learned skills",
                Map.of(
                        "orphanedRootCount", health.orphanedRootCount(),
                        "learnedSkillCount", health.learnedSkillCount())));
        return List.copyOf(actions);
    }
}
