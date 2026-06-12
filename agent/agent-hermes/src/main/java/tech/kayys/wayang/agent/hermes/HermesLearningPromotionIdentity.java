package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stable identifiers for one learned-skill promotion attempt.
 */
public record HermesLearningPromotionIdentity(
        String promotionId,
        String idempotencyKey,
        String sourceRequestId,
        String revision,
        String lineageRootSkillId) {

    public HermesLearningPromotionIdentity {
        promotionId = HermesText.trimOr(promotionId, "hermes-learning-promotion-unknown");
        idempotencyKey = HermesText.trimOr(idempotencyKey, "learning-promotion-unknown");
        sourceRequestId = HermesText.trimOr(sourceRequestId, "");
        revision = HermesText.trimOr(revision, "");
        lineageRootSkillId = HermesText.trimOr(lineageRootSkillId, "");
    }

    public static HermesLearningPromotionIdentity from(
            HermesLearningDecision decision,
            SkillDefinition skill,
            String reason) {
        String normalizedDecision = decision == null
                ? "skipped"
                : decision.name().toLowerCase(java.util.Locale.ROOT);
        String skillId = skill == null ? "" : HermesText.trimOr(skill.id(), "");
        String revision = metadataText(skill, "hermes.revision");
        String sourceRequestId = firstMetadataText(
                skill,
                "hermes.latestRequestId",
                "hermes.refinementRequestId",
                "hermes.createdRequestId",
                "hermes.requestId");
        String lineageRootSkillId = HermesText.trimOr(
                metadataText(skill, "hermes.lineageRootSkillId"),
                skillId);
        String signature = HermesDirectiveSupport.hashBase(
                normalizedDecision,
                skillId,
                revision,
                sourceRequestId,
                lineageRootSkillId,
                HermesText.oneLineOr(reason, ""));
        String idempotencyKey = "learning-promotion-" + signature;
        String base = String.join(
                "-",
                HermesText.trimOr(skillId, "skill"),
                HermesText.trimOr(revision, "revision"),
                HermesText.trimOr(sourceRequestId, "request"),
                signature);
        return new HermesLearningPromotionIdentity(
                HermesDirectiveSupport.prefixedId("hermes-learning-promotion", base, "promotion"),
                idempotencyKey,
                sourceRequestId,
                revision,
                lineageRootSkillId);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("promotionId", promotionId);
        values.put("idempotencyKey", idempotencyKey);
        values.put("sourceRequestId", sourceRequestId);
        values.put("revision", revision);
        values.put("lineageRootSkillId", lineageRootSkillId);
        return Map.copyOf(values);
    }

    private static String firstMetadataText(SkillDefinition skill, String... keys) {
        if (keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = metadataText(skill, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String metadataText(SkillDefinition skill, String key) {
        if (skill == null || skill.metadata() == null) {
            return "";
        }
        Object value = skill.metadata().get(key);
        return value == null ? "" : HermesText.oneLineOr(String.valueOf(value), "");
    }
}
