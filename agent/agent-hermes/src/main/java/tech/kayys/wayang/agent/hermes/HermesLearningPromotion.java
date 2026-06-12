package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Final promotion decision for a planned learned-skill persistence mutation.
 */
public record HermesLearningPromotion(
        HermesLearningDecision decision,
        String status,
        String skillId,
        String reason,
        SkillDefinition skill,
        HermesLearningPromotionIdentity identity,
        Map<String, Object> metadata) {

    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_SKIPPED = "skipped";

    public HermesLearningPromotion {
        if (decision == null) {
            throw new IllegalArgumentException("learning promotion decision is required");
        }
        status = HermesText.oneLineOr(status, STATUS_SKIPPED);
        reason = HermesText.oneLineOr(reason, "");
        skillId = HermesText.trimOr(skillId, skill == null ? "" : skill.id());
        identity = identity == null
                ? HermesLearningPromotionIdentity.from(decision, skill, reason)
                : identity;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Optional<SkillDefinition> skillDefinition() {
        return Optional.ofNullable(skill);
    }

    public boolean persistsSkill() {
        return decision == HermesLearningDecision.CREATED || decision == HermesLearningDecision.UPDATED;
    }

    public static HermesLearningPromotion approved(HermesLearningPlan plan) {
        HermesLearningPlan resolved = requirePlan(plan);
        return new HermesLearningPromotion(
                resolved.decision(),
                STATUS_APPROVED,
                skillId(resolved),
                resolved.reason(),
                resolved.skillDefinition().orElseThrow(),
                identity(resolved),
                planMetadata(resolved));
    }

    public static HermesLearningPromotion rejected(HermesLearningPlan plan, String reason) {
        HermesLearningPlan resolved = requirePlan(plan);
        return new HermesLearningPromotion(
                HermesLearningDecision.SKIPPED,
                STATUS_REJECTED,
                skillId(resolved),
                reason,
                resolved.skillDefinition().orElse(null),
                identity(resolved),
                planMetadata(resolved));
    }

    public static HermesLearningPromotion skipped(HermesLearningPlan plan) {
        HermesLearningPlan resolved = requirePlan(plan);
        return new HermesLearningPromotion(
                HermesLearningDecision.SKIPPED,
                STATUS_SKIPPED,
                skillId(resolved),
                resolved.reason(),
                resolved.skillDefinition().orElse(null),
                identity(resolved),
                planMetadata(resolved));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>(metadata);
        values.putAll(identity.toMetadata());
        values.put("decision", decision.name().toLowerCase(java.util.Locale.ROOT));
        values.put("status", status);
        values.put("skillId", skillId);
        values.put("reason", reason);
        values.put("persistsSkill", persistsSkill());
        skillDefinition().ifPresent(skillDefinition -> {
            values.put("skillName", HermesText.oneLineOr(skillDefinition.name(), ""));
            values.put("skillCategory", HermesText.oneLineOr(skillDefinition.category(), ""));
            values.put("revision", revision(skillDefinition));
        });
        return Map.copyOf(values);
    }

    private static HermesLearningPlan requirePlan(HermesLearningPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("learning plan is required");
        }
        return plan;
    }

    private static String skillId(HermesLearningPlan plan) {
        return plan.skillDefinition()
                .map(SkillDefinition::id)
                .orElse("");
    }

    private static Map<String, Object> planMetadata(HermesLearningPlan plan) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("planDecision", plan.decision().name().toLowerCase(java.util.Locale.ROOT));
        values.put("planReason", plan.reason());
        values.put("plannedPersistence", plan.persistsSkill());
        values.put(HermesLearningMetadataKeys.PLANNING_LIFECYCLE, plan.lifecycleReport().toMetadata());
        return values;
    }

    private static HermesLearningPromotionIdentity identity(HermesLearningPlan plan) {
        return HermesLearningPromotionIdentity.from(
                plan.decision(),
                plan.skillDefinition().orElse(null),
                plan.reason());
    }

    private static String revision(SkillDefinition skill) {
        Object revision = skill.metadata().get("hermes.revision");
        return revision == null ? "" : HermesText.oneLineOr(String.valueOf(revision), "");
    }
}
