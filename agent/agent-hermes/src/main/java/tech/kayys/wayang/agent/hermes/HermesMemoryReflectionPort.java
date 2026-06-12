package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes long-term memory reflection.
 */
public interface HermesMemoryReflectionPort {

    HermesPortDispatchResult consolidate(HermesMemoryReflectionDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.MEMORY_REFLECTION, this);
    }

    static HermesMemoryReflectionPort noop() {
        return new HermesMemoryReflectionPort() {
            @Override
            public HermesPortDispatchResult consolidate(HermesMemoryReflectionDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.MEMORY_REFLECTION,
                        directive.operation(),
                        directive.subjectId(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.MEMORY_REFLECTION);
            }
        };
    }
}
