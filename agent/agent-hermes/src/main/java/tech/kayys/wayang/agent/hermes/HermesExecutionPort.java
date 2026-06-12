package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes execution backends.
 */
public interface HermesExecutionPort {

    HermesPortDispatchResult dispatch(HermesExecutionDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.EXECUTION, this);
    }

    static HermesExecutionPort noop() {
        return new HermesExecutionPort() {
            @Override
            public HermesPortDispatchResult dispatch(HermesExecutionDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.EXECUTION,
                        directive.operation(),
                        directive.backend(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.EXECUTION);
            }
        };
    }
}
