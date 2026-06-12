package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesLearningAuditDirective;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptQuery;

import java.util.function.BiFunction;

/**
 * Maps named Hermes learning-audit presets to runtime directives.
 */
final class HermesLearningAuditPresetMapper {

    HermesLearningAuditDirective latest(HermesLearningAuditPresetRequest request) {
        return directive(HermesLearningPromotionReceiptQuery.recent(limit(request)), request);
    }

    HermesLearningAuditDirective persisted(HermesLearningAuditPresetRequest request) {
        return directive(HermesLearningPromotionReceiptQuery.persisted(limit(request)), request);
    }

    HermesLearningAuditDirective skill(String skillId, HermesLearningAuditPresetRequest request) {
        return identity("skillId", skillId, request, HermesLearningPromotionReceiptQuery::forSkill);
    }

    HermesLearningAuditDirective status(String status, HermesLearningAuditPresetRequest request) {
        return identity("status", status, request, HermesLearningPromotionReceiptQuery::forStatus);
    }

    HermesLearningAuditDirective outcome(String outcome, HermesLearningAuditPresetRequest request) {
        return identity("outcome", outcome, request, HermesLearningPromotionReceiptQuery::forOutcome);
    }

    HermesLearningAuditDirective receipt(String idempotencyKey, HermesLearningAuditPresetRequest request) {
        return directive(new HermesLearningPromotionReceiptQuery(
                "",
                "",
                "",
                required("idempotencyKey", idempotencyKey),
                false,
                limit(request)), request);
    }

    private HermesLearningAuditDirective identity(
            String parameterName,
            String value,
            HermesLearningAuditPresetRequest request,
            BiFunction<String, Integer, HermesLearningPromotionReceiptQuery> queryFactory) {
        return directive(queryFactory.apply(required(parameterName, value), limit(request)), request);
    }

    private HermesLearningAuditDirective directive(
            HermesLearningPromotionReceiptQuery query,
            HermesLearningAuditPresetRequest request) {
        HermesLearningAuditPresetRequest resolved = request == null
                ? new HermesLearningAuditPresetRequest()
                : request;
        return HermesLearningAuditDirective.inspect(
                query.withCursors(resolved.beforeReceiptId(), resolved.afterReceiptId()));
    }

    private int limit(HermesLearningAuditPresetRequest request) {
        return request == null ? 0 : request.limit();
    }

    private String required(String parameterName, String value) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(parameterName + " is required");
        }
        return resolved;
    }
}
