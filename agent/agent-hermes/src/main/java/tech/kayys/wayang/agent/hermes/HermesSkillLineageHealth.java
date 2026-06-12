package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operational health summary for a learned-skill lineage catalog.
 */
public record HermesSkillLineageHealth(
        String status,
        boolean attentionRequired,
        String reason,
        List<String> recommendedActions,
        List<String> orphanedRootSkillIds,
        List<String> refinedRootSkillIds,
        int learnedSkillCount,
        int rootCount,
        long orphanedRootCount,
        long refinedRootCount,
        long refinedEntryCount) {

    public HermesSkillLineageHealth {
        status = HermesText.oneLineOr(status, "healthy");
        reason = HermesText.oneLineOr(reason, "skill lineage catalog inspected");
        recommendedActions = HermesText.distinctOneLineList(recommendedActions);
        orphanedRootSkillIds = HermesText.distinctOneLineList(orphanedRootSkillIds);
        refinedRootSkillIds = HermesText.distinctOneLineList(refinedRootSkillIds);
        learnedSkillCount = Math.max(learnedSkillCount, 0);
        rootCount = Math.max(rootCount, 0);
        orphanedRootCount = Math.max(orphanedRootCount, 0);
        refinedRootCount = Math.max(refinedRootCount, 0);
        refinedEntryCount = Math.max(refinedEntryCount, 0);
    }

    public static HermesSkillLineageHealth from(HermesSkillLineageCatalog catalog) {
        HermesSkillLineageCatalog resolved = catalog == null ? HermesSkillLineageCatalog.empty() : catalog;
        List<String> orphanedRootSkillIds = resolved.roots().stream()
                .filter(root -> !root.rootPresent())
                .map(HermesSkillLineageRoot::rootSkillId)
                .toList();
        List<String> refinedRootSkillIds = resolved.roots().stream()
                .filter(HermesSkillLineageRoot::hasRefinements)
                .map(HermesSkillLineageRoot::rootSkillId)
                .toList();
        if (resolved.emptyCatalog()) {
            return new HermesSkillLineageHealth(
                    "empty",
                    false,
                    "no learned skills cataloged yet",
                    List.of("distill-first-successful-complex-workflow"),
                    orphanedRootSkillIds,
                    refinedRootSkillIds,
                    resolved.learnedSkillCount(),
                    resolved.rootCount(),
                    resolved.orphanedRootCount(),
                    resolved.refinedRootCount(),
                    resolved.refinedEntryCount());
        }
        if (!orphanedRootSkillIds.isEmpty()) {
            return new HermesSkillLineageHealth(
                    "attention",
                    true,
                    "learned skill catalog has lineage roots without root entries",
                    List.of("repair-orphaned-lineage-roots", "inspect-learned-skill-storage-consistency"),
                    orphanedRootSkillIds,
                    refinedRootSkillIds,
                    resolved.learnedSkillCount(),
                    resolved.rootCount(),
                    resolved.orphanedRootCount(),
                    resolved.refinedRootCount(),
                    resolved.refinedEntryCount());
        }
        if (!refinedRootSkillIds.isEmpty()) {
            return new HermesSkillLineageHealth(
                    "evolving",
                    false,
                    "learned skill catalog has active refinements",
                    List.of("review-refined-skill-quality"),
                    orphanedRootSkillIds,
                    refinedRootSkillIds,
                    resolved.learnedSkillCount(),
                    resolved.rootCount(),
                    resolved.orphanedRootCount(),
                    resolved.refinedRootCount(),
                    resolved.refinedEntryCount());
        }
        return new HermesSkillLineageHealth(
                "healthy",
                false,
                "learned skill catalog has no lineage gaps",
                List.of(),
                orphanedRootSkillIds,
                refinedRootSkillIds,
                resolved.learnedSkillCount(),
                resolved.rootCount(),
                resolved.orphanedRootCount(),
                resolved.refinedRootCount(),
                resolved.refinedEntryCount());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", status);
        metadata.put("attentionRequired", attentionRequired);
        metadata.put("reason", reason);
        metadata.put("recommendedActions", recommendedActions);
        metadata.put("remediationPlan", remediationPlan().toMetadata());
        metadata.put("orphanedRootSkillIds", orphanedRootSkillIds);
        metadata.put("refinedRootSkillIds", refinedRootSkillIds);
        metadata.put("learnedSkillCount", learnedSkillCount);
        metadata.put("rootCount", rootCount);
        metadata.put("orphanedRootCount", orphanedRootCount);
        metadata.put("refinedRootCount", refinedRootCount);
        metadata.put("refinedEntryCount", refinedEntryCount);
        return Map.copyOf(metadata);
    }

    public HermesSkillLineageRemediationPlan remediationPlan() {
        return HermesSkillLineageRemediationPlan.from(this);
    }
}
