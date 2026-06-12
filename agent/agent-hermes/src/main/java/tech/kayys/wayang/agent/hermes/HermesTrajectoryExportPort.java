package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for Hermes trajectory and audit export.
 */
public interface HermesTrajectoryExportPort {

    HermesPortDispatchResult export(HermesTrajectoryExportDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.TRAJECTORY_EXPORT, this);
    }

    static HermesTrajectoryExportPort noop() {
        return new HermesTrajectoryExportPort() {
            @Override
            public HermesPortDispatchResult export(HermesTrajectoryExportDirective directive) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.TRAJECTORY_EXPORT,
                        directive.operation(),
                        directive.exportId(),
                        directive.reason(),
                        directive.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.TRAJECTORY_EXPORT);
            }
        };
    }
}
