package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC HTTP adapter configuration.
 */
final class WayangA2aJsonRpcHttpConfigProjection {

    private WayangA2aJsonRpcHttpConfigProjection() {
    }

    static Map<String, Object> config(WayangA2aJsonRpcHttpConfig config) {
        WayangA2aJsonRpcHttpConfig resolved = Objects.requireNonNull(config, "config");
        Map<String, Object> values = new LinkedHashMap<>();
        WayangA2aJsonRpcHttpRouteSurface.ordered()
                .forEach(surface -> putConfigValues(values, surface, resolved));
        return WayangA2aMaps.copyMap(values);
    }

    private static void putConfigValues(
            Map<String, Object> values,
            WayangA2aJsonRpcHttpRouteSurface surface,
            WayangA2aJsonRpcHttpConfig config) {
        values.put(surface.pathField(), surface.configPath(config));
        if (!surface.enabledField().isBlank()) {
            values.put(surface.enabledField(), surface.configEnabled(config));
        }
    }
}
