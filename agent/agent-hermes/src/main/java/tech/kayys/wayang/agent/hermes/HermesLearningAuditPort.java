package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes learned-skill audit inspection.
 */
public interface HermesLearningAuditPort {

    HermesPortDispatchResult inspect(HermesLearningAuditDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.LEARNING_AUDIT, this);
    }

    static HermesLearningAuditPort service(HermesLearningAuditService service) {
        return service == null ? noop() : new HermesLearningAuditServicePort(service);
    }

    static HermesLearningAuditPort service(HermesLearningPromotionReceiptLedger ledger) {
        return ledger == null ? noop() : service(new HermesLearningAuditService(ledger));
    }

    static HermesLearningAuditPort noop() {
        return new HermesLearningAuditPort() {
            @Override
            public HermesPortDispatchResult inspect(HermesLearningAuditDirective directive) {
                HermesLearningAuditDirective resolved = directive == null
                        ? HermesLearningAuditDirective.latest(HermesLearningPromotionReceiptQuery.DEFAULT_LIMIT)
                        : directive;
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.LEARNING_AUDIT,
                        resolved.operation(),
                        resolved.target(),
                        "learning audit adapter not configured",
                        resolved.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.LEARNING_AUDIT);
            }
        };
    }
}
