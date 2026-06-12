package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesLearningAuditDirective;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptQuery;

/**
 * Maps learning-audit request parameters to Hermes runtime directives.
 */
final class HermesLearningAuditDirectiveMapper {

    HermesLearningAuditDirective directive(HermesLearningAuditRequest request) {
        HermesLearningAuditRequest resolved = request == null
                ? new HermesLearningAuditRequest()
                : request;
        return HermesLearningAuditDirective.inspect(new HermesLearningPromotionReceiptQuery(
                resolved.skillId(),
                resolved.status(),
                resolved.outcome(),
                resolved.idempotencyKey(),
                resolved.persistedOnly(),
                resolved.beforeReceiptId(),
                resolved.afterReceiptId(),
                resolved.limit()));
    }
}
