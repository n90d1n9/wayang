package tech.kayys.wayang.agent.hermes;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Runtime adapter boundary for Hermes diagnostics inspection.
 */
public interface HermesRuntimeDiagnosticsPort {

    HermesPortDispatchResult inspect(HermesRuntimeDiagnosticsDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.RUNTIME_DIAGNOSTICS, this);
    }

    static HermesRuntimeDiagnosticsPort service(HermesRuntimeDiagnostics diagnostics) {
        return new HermesRuntimeDiagnosticsServicePort(diagnostics);
    }

    static HermesRuntimeDiagnosticsPort service(
            HermesRuntimeDiagnostics diagnostics,
            Supplier<Map<String, Object>> metadataOverlay) {
        return new HermesRuntimeDiagnosticsServicePort(diagnostics, metadataOverlay);
    }

    static HermesRuntimeDiagnosticsPort noop() {
        return new HermesRuntimeDiagnosticsPort() {
            @Override
            public HermesPortDispatchResult inspect(HermesRuntimeDiagnosticsDirective directive) {
                HermesRuntimeDiagnosticsDirective resolved = directive == null
                        ? HermesRuntimeDiagnosticsDirective.summary()
                        : directive;
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.RUNTIME_DIAGNOSTICS,
                        resolved.operation(),
                        resolved.target(),
                        "runtime diagnostics adapter not configured",
                        resolved.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.RUNTIME_DIAGNOSTICS);
            }
        };
    }
}
