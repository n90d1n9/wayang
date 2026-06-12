package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes cron and scheduled work.
 */
public interface HermesAutomationPort {

    HermesPortDispatchResult register(HermesAutomationDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.AUTOMATION, this);
    }

    static HermesAutomationPort noop() {
        return new HermesAutomationPort() {
            @Override
            public HermesPortDispatchResult register(HermesAutomationDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.AUTOMATION,
                        directive.operation(),
                        directive.taskId(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.AUTOMATION);
            }
        };
    }
}
