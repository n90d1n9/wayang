package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes parallel sub-agent delegation.
 */
public interface HermesDelegationPort {

    HermesPortDispatchResult spawn(HermesDelegationDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.DELEGATION, this);
    }

    static HermesDelegationPort noop() {
        return new HermesDelegationPort() {
            @Override
            public HermesPortDispatchResult spawn(HermesDelegationDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.DELEGATION,
                        directive.operation(),
                        directive.groupId(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.DELEGATION);
            }
        };
    }
}
