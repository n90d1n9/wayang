package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes journal inspection.
 */
public interface HermesRuntimeJournalPort {

    HermesPortDispatchResult inspect(HermesRuntimeJournalDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.RUNTIME_JOURNAL, this);
    }

    static HermesRuntimeJournalPort service(HermesRuntimeJournalService service) {
        return new HermesRuntimeJournalServicePort(service);
    }

    static HermesRuntimeJournalPort noop() {
        return new HermesRuntimeJournalPort() {
            @Override
            public HermesPortDispatchResult inspect(HermesRuntimeJournalDirective directive) {
                HermesRuntimeJournalDirective resolved = directive == null
                        ? HermesRuntimeJournalDirective.latest(HermesRuntimeEventQuery.DEFAULT_LIMIT)
                        : directive;
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.RUNTIME_JOURNAL,
                        resolved.operation(),
                        resolved.target(),
                        "runtime journal adapter not configured",
                        resolved.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.RUNTIME_JOURNAL);
            }
        };
    }
}
