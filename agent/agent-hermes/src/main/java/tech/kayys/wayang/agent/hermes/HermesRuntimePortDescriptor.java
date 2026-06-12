package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight runtime adapter identity and readiness metadata.
 */
public record HermesRuntimePortDescriptor(
        String port,
        String adapterId,
        String adapterType,
        boolean configured,
        boolean noop,
        boolean ready,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public HermesRuntimePortDescriptor {
        port = HermesDirectiveSupport.clean(port, "unknown");
        adapterId = HermesDirectiveSupport.clean(adapterId, noop ? port + "-noop" : port + "-adapter");
        adapterType = HermesDirectiveSupport.clean(adapterType, noop ? "noop" : "custom");
        status = HermesDirectiveSupport.clean(status, ready ? "ready" : "unavailable");
        reason = HermesDirectiveSupport.clean(reason, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesRuntimePortDescriptor noop(String port) {
        String normalizedPort = HermesDirectiveSupport.clean(port, "unknown");
        return new HermesRuntimePortDescriptor(
                normalizedPort,
                normalizedPort + "-noop",
                "noop",
                false,
                true,
                true,
                "noop",
                "runtime adapter not configured",
                Map.of());
    }

    public static HermesRuntimePortDescriptor configured(String port, Object adapter) {
        String adapterId = adapter == null ? "" : adapter.getClass().getName();
        return new HermesRuntimePortDescriptor(
                port,
                adapterId,
                "custom",
                true,
                false,
                true,
                "ready",
                "runtime adapter configured",
                Map.of());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("port", port);
        values.put("adapterId", adapterId);
        values.put("adapterType", adapterType);
        values.put("configured", configured);
        values.put("noop", noop);
        values.put("ready", ready);
        values.put("status", status);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }
}
