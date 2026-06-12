package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Result of one Hermes learning-loop pass.
 */
public record HermesLearningResult(
        HermesLearningDecision decision,
        String skillId,
        String reason,
        SkillDefinition skill,
        Map<String, Object> metadata) {

    public HermesLearningResult(
            HermesLearningDecision decision,
            String skillId,
            String reason,
            SkillDefinition skill) {
        this(decision, skillId, reason, skill, Map.of());
    }

    public HermesLearningResult {
        if (decision == null) {
            throw new IllegalArgumentException("learning decision is required");
        }
        skillId = HermesText.trimOr(skillId, skill == null ? "" : skill.id());
        reason = HermesText.oneLineOr(reason, "");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Optional<SkillDefinition> skillDefinition() {
        return Optional.ofNullable(skill);
    }

    public HermesLearningResultMetadata metadataView() {
        return new HermesLearningResultMetadata(metadata);
    }

    public static HermesLearningResult created(SkillDefinition skill) {
        return new HermesLearningResult(HermesLearningDecision.CREATED, skill.id(), "learned new procedural skill", skill);
    }

    public static HermesLearningResult updated(SkillDefinition skill) {
        return new HermesLearningResult(HermesLearningDecision.UPDATED, skill.id(), "refined existing procedural skill", skill);
    }

    public static HermesLearningResult skipped(String reason) {
        return new HermesLearningResult(HermesLearningDecision.SKIPPED, null, reason, null);
    }

    public HermesLearningResult withPromotion(HermesLearningPromotion promotion) {
        if (promotion == null) {
            return this;
        }
        return withMetadata(Map.of(HermesLearningMetadataKeys.PROMOTION, promotion.toMetadata()));
    }

    public HermesLearningResult withPromotionReceipt(HermesLearningPromotionReceipt receipt) {
        if (receipt == null) {
            return this;
        }
        return withMetadata(Map.of(HermesLearningMetadataKeys.PROMOTION_RECEIPT, receipt.toMetadata()));
    }

    public HermesLearningResult withLifecycleReport(HermesLearningLifecycleReport lifecycleReport) {
        if (lifecycleReport == null || lifecycleReport.emptyReport()) {
            return this;
        }
        return withMetadata(Map.of(HermesLearningMetadataKeys.LIFECYCLE, lifecycleReport.toMetadata()));
    }

    public HermesLearningResult withSkillIndexingReceipt(HermesLearningIndexingReceipt receipt) {
        if (receipt == null) {
            return this;
        }
        return withMetadata(Map.of(HermesLearningMetadataKeys.SKILL_INDEXING_RECEIPT, receipt.toMetadata()));
    }

    public HermesLearningResult withMetadata(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(metadata);
        merged.putAll(values);
        return new HermesLearningResult(decision, skillId, reason, skill, merged);
    }
}
