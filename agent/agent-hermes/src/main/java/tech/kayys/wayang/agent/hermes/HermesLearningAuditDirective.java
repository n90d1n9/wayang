package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral instruction for inspecting Hermes learned-skill audit receipts.
 */
public record HermesLearningAuditDirective(
        boolean active,
        String operation,
        String target,
        HermesLearningPromotionReceiptQuery query,
        String reason) {

    public HermesLearningAuditDirective {
        query = query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        operation = HermesDirectiveSupport.clean(operation, active ? "inspect" : "none");
        target = HermesDirectiveSupport.clean(target, target(query));
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "learning audit inspection requested" : "learning audit inspection inactive");
    }

    public static HermesLearningAuditDirective inspect(HermesLearningPromotionReceiptQuery query) {
        HermesLearningPromotionReceiptQuery resolved =
                query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        return new HermesLearningAuditDirective(
                true,
                "inspect",
                target(resolved),
                resolved,
                "learning audit inspection requested");
    }

    public static HermesLearningAuditDirective latest(int limit) {
        return inspect(HermesLearningPromotionReceiptQuery.recent(limit));
    }

    public static HermesLearningAuditDirective skill(String skillId, int limit) {
        return inspect(HermesLearningPromotionReceiptQuery.forSkill(skillId, limit));
    }

    public static HermesLearningAuditDirective status(String status, int limit) {
        return inspect(HermesLearningPromotionReceiptQuery.forStatus(status, limit));
    }

    public static HermesLearningAuditDirective outcome(String outcome, int limit) {
        return inspect(HermesLearningPromotionReceiptQuery.forOutcome(outcome, limit));
    }

    public static HermesLearningAuditDirective persisted(int limit) {
        return inspect(HermesLearningPromotionReceiptQuery.persisted(limit));
    }

    public static HermesLearningAuditDirective none() {
        return new HermesLearningAuditDirective(
                false,
                "none",
                "",
                HermesLearningPromotionReceiptQuery.recent(),
                "learning audit inspection inactive");
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("active", active);
        values.put("operation", operation);
        values.put("target", target);
        values.put("reason", reason);
        values.put("query", query.toMetadata());
        return Map.copyOf(values);
    }

    private static String target(HermesLearningPromotionReceiptQuery query) {
        if (!query.beforeReceiptId().isBlank()) {
            return "before-receipt:" + query.beforeReceiptId();
        }
        if (!query.afterReceiptId().isBlank()) {
            return "after-receipt:" + query.afterReceiptId();
        }
        if (!query.idempotencyKey().isBlank()) {
            return "receipt:" + query.idempotencyKey();
        }
        if (!query.skillId().isBlank()) {
            return "skill:" + query.skillId();
        }
        if (!query.status().isBlank()) {
            return "status:" + query.status();
        }
        if (!query.outcome().isBlank()) {
            return "outcome:" + query.outcome();
        }
        if (query.persistedOnly()) {
            return "persisted";
        }
        return "latest";
    }
}
