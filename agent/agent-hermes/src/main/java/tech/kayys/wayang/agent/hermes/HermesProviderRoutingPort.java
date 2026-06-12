package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes provider/model routing.
 */
public interface HermesProviderRoutingPort {

    HermesPortDispatchResult route(HermesProviderRoutingDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.PROVIDER_ROUTING, this);
    }

    static HermesProviderRoutingPort noop() {
        return new HermesProviderRoutingPort() {
            @Override
            public HermesPortDispatchResult route(HermesProviderRoutingDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.PROVIDER_ROUTING,
                        directive.operation(),
                        directive.selectedProvider(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.PROVIDER_ROUTING);
            }
        };
    }
}
