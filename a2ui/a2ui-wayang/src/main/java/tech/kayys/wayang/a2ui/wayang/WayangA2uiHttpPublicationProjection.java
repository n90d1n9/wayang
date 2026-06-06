package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP endpoint publication manifests.
 */
final class WayangA2uiHttpPublicationProjection {

    private WayangA2uiHttpPublicationProjection() {
    }

    static Map<String, Object> publication(WayangA2uiHttpEndpointPublication publication) {
        WayangA2uiHttpEndpointPublication resolved = Objects.requireNonNull(publication, "publication");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("routeCount", resolved.routeCount());
        values.put("operations", resolved.operations());
        values.put("paths", resolved.paths());
        values.put("routes", resolved.routes());
        return WayangA2uiTransportMaps.freeze(values);
    }
}
