package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpBindingReport;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection for A2UI HTTP route/handler binding coverage reports.
 */
public final class HttpBindingReportProjection {

    private HttpBindingReportProjection() {
    }

    public static Map<String, Object> report(WayangA2uiHttpBindingReport report) {
        WayangA2uiHttpBindingReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("complete", resolved.complete());
        values.put("routeOperationCount", resolved.routeOperationCount());
        values.put("handlerOperationCount", resolved.handlerOperationCount());
        values.put("routeOperations", resolved.routeOperations());
        values.put("handlerOperations", resolved.handlerOperations());
        values.put("missingHandlerOperations", resolved.missingHandlerOperations());
        values.put("orphanHandlerOperations", resolved.orphanHandlerOperations());
        return TransportMaps.freeze(values);
    }
}
