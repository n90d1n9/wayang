package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes gateway delivery channels.
 */
public interface HermesGatewayPort {

    HermesPortDispatchResult deliver(HermesGatewayDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.GATEWAY, this);
    }

    static HermesGatewayPort noop() {
        return new HermesGatewayPort() {
            @Override
            public HermesPortDispatchResult deliver(HermesGatewayDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.GATEWAY,
                        directive.operation(),
                        directive.destinationId(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.GATEWAY);
            }
        };
    }
}
