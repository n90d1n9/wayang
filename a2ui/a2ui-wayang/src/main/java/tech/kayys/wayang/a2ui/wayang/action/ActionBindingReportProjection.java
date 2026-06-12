package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionBindingReport;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection for A2UI action policy/handler binding coverage reports.
 */
public final class ActionBindingReportProjection {

    private ActionBindingReportProjection() {
    }

    public static Map<String, Object> report(WayangA2uiActionBindingReport report) {
        WayangA2uiActionBindingReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("complete", resolved.complete());
        values.put("policyActionCount", resolved.policyActionCount());
        values.put("handlerActionCount", resolved.handlerActionCount());
        values.put("policyActions", resolved.policyActions());
        values.put("handlerActions", resolved.handlerActions());
        values.put("missingHandlerActions", resolved.missingHandlerActions());
        values.put("orphanHandlerActions", resolved.orphanHandlerActions());
        return TransportMaps.freeze(values);
    }
}
