package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Request handed to learned-skill indexing adapters after persistence completes.
 */
public record HermesLearningIndexingRequest(
        HermesLearningPromotion promotion,
        HermesLearningResult result,
        HermesLearningSignal signal) {

    public HermesLearningIndexingRequest {
        result = result == null ? HermesLearningResult.skipped("learning result missing") : result;
        promotion = promotion == null
                ? HermesLearningPromotion.skipped(HermesLearningPlan.skipped(result.reason()))
                : promotion;
    }

    public String skillId() {
        return HermesText.trimOr(promotion.skillId(), result.skillId());
    }

    public Optional<SkillDefinition> skillDefinition() {
        return result.skillDefinition().or(promotion::skillDefinition);
    }

    public String requestId() {
        return signal == null ? "" : HermesText.trimOr(signal.requestId(), "");
    }

    public boolean persistedSkill() {
        Object persisted = result.metadataView().promotionReceipt().get("persisted");
        if (persisted instanceof Boolean bool) {
            return bool;
        }
        return promotion.status().equals(HermesLearningPromotion.STATUS_APPROVED)
                && (result.decision() == HermesLearningDecision.CREATED
                || result.decision() == HermesLearningDecision.UPDATED);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("skillId", skillId());
        values.put("requestId", requestId());
        values.put("decision", result.decision().name().toLowerCase(java.util.Locale.ROOT));
        values.put("promotionStatus", promotion.status());
        values.put("persistedSkill", persistedSkill());
        skillDefinition().ifPresent(skill -> {
            values.put("skillName", HermesText.oneLineOr(skill.name(), ""));
            values.put("skillCategory", HermesText.oneLineOr(skill.category(), ""));
        });
        return Map.copyOf(values);
    }
}
