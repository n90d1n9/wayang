package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Runtime diagnostics port backed by the current Hermes diagnostics snapshot.
 */
public final class HermesRuntimeDiagnosticsServicePort implements HermesRuntimeDiagnosticsPort {

    private final HermesRuntimeDiagnostics diagnostics;
    private final Supplier<Map<String, Object>> metadataOverlay;

    public HermesRuntimeDiagnosticsServicePort(HermesRuntimeDiagnostics diagnostics) {
        this(diagnostics, null);
    }

    public HermesRuntimeDiagnosticsServicePort(
            HermesRuntimeDiagnostics diagnostics,
            Supplier<Map<String, Object>> metadataOverlay) {
        this.diagnostics = diagnostics == null
                ? HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop())
                : diagnostics;
        this.metadataOverlay = metadataOverlay == null ? Map::of : metadataOverlay;
    }

    @Override
    public HermesPortDispatchResult inspect(HermesRuntimeDiagnosticsDirective directive) {
        HermesRuntimeDiagnosticsDirective resolved = directive == null
                ? HermesRuntimeDiagnosticsDirective.summary()
                : directive;
        Map<String, Object> overlay = overlayMetadata();
        Map<String, Object> metadata = new LinkedHashMap<>(resolved.toMetadata());
        metadata.put("ready", diagnostics.ready());
        metadata.put("runtimePortsReady", diagnostics.runtimePortsReady());
        metadata.put("skillPersistenceReady", diagnostics.skillPersistenceReady());
        metadata.put("learningAuditConfigured", diagnostics.learningAuditConfigured());
        metadata.put("learningAuditReady", diagnostics.learningAuditReady());
        metadata.put("attention", diagnostics.attention());
        metadata.putAll(overlay);
        metadata.put("diagnostics", withOverlay(viewMetadata(resolved.view()), overlay));
        return new HermesPortDispatchResult(
                HermesRuntimePortCatalog.RUNTIME_DIAGNOSTICS,
                resolved.operation(),
                resolved.target(),
                true,
                true,
                true,
                "inspected",
                "runtime diagnostics inspected",
                metadata);
    }

    private Map<String, Object> viewMetadata(String view) {
        return switch (view) {
            case HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY -> summaryMetadata();
            case HermesRuntimeDiagnosticsDirective.VIEW_CAPABILITIES -> diagnostics.capabilities().toMetadata();
            case HermesRuntimeDiagnosticsDirective.VIEW_LIFECYCLE -> diagnostics.lifecycle().toMetadata();
            case HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS -> diagnostics.runtimePorts().toMetadata();
            case HermesRuntimeDiagnosticsDirective.VIEW_SKILL_PERSISTENCE ->
                    diagnostics.skillPersistencePreflight().toMetadata();
            case HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT -> diagnostics.learningAuditMetadata();
            default -> diagnostics.toMetadata();
        };
    }

    private Map<String, Object> summaryMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ready", diagnostics.ready());
        metadata.put("runtimePortsReady", diagnostics.runtimePortsReady());
        metadata.put("skillPersistenceReady", diagnostics.skillPersistenceReady());
        metadata.put("learningAuditConfigured", diagnostics.learningAuditConfigured());
        metadata.put("learningAuditReady", diagnostics.learningAuditReady());
        metadata.put("configuredPortCount", diagnostics.configuredPortCount());
        metadata.put("readyPortCount", diagnostics.readyPortCount());
        metadata.put("noopPortCount", diagnostics.noopPortCount());
        metadata.put("lifecyclePhase", diagnostics.lifecycle().phase());
        metadata.put("assembly", diagnostics.assemblyReport().summaryMetadata());
        metadata.put("attention", diagnostics.attention());
        return Map.copyOf(metadata);
    }

    private Map<String, Object> overlayMetadata() {
        try {
            Map<String, Object> metadata = metadataOverlay.get();
            return metadata == null ? Map.of() : Map.copyOf(metadata);
        } catch (RuntimeException error) {
            return Map.of(
                    "runtimeDiagnosticsOverlayError",
                    error.getClass().getName());
        }
    }

    private static Map<String, Object> withOverlay(
            Map<String, Object> metadata,
            Map<String, Object> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return metadata == null ? Map.of() : metadata;
        }
        Map<String, Object> values = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        values.putAll(overlay);
        return Map.copyOf(values);
    }
}
