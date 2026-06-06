package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP binding report probe results.
 */
final class WayangA2uiHttpBindingReportProbeProjection {

    private WayangA2uiHttpBindingReportProbeProjection() {
    }

    static Map<String, Object> bindingReport(WayangA2uiHttpBindingReportProbeResult result) {
        WayangA2uiHttpBindingReportProbeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("httpSuccessful", resolved.httpSuccessful());
        values.put("routeOperation", resolved.routeOperation());
        values.put("allow", resolved.allow());
        values.put(WayangA2uiTransportFields.OUTCOME, resolved.outcome());
        values.put("contentType", resolved.contentType());
        values.put(WayangA2uiTransportFields.MIME_TYPE, resolved.mimeType());
        values.put(WayangA2uiTransportFields.BODY_ENCODING, resolved.bodyEncoding());
        values.put("bindingReportRoute", resolved.bindingReportRoute());
        values.put("bindingReportResult", resolved.bindingReportResult());
        values.put("jsonContent", resolved.jsonContent());
        values.put(WayangA2uiTransportFields.COMPLETE, resolved.complete());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT, resolved.routeOperationCount());
        values.put(WayangA2uiTransportFields.HANDLER_OPERATION_COUNT, resolved.handlerOperationCount());
        values.put(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, resolved.missingHandlerCount());
        values.put(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, resolved.orphanHandlerCount());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", resolved.issues());
        values.put("routeOperations", resolved.routeOperations());
        values.put("handlerOperations", resolved.handlerOperations());
        values.put("missingHandlerOperations", resolved.missingHandlerOperations());
        values.put("orphanHandlerOperations", resolved.orphanHandlerOperations());
        values.put(WayangA2uiTransportFields.METADATA, resolved.metadata());
        values.put(WayangA2uiTransportFields.BODY, resolved.body());
        values.put("headers", resolved.headers());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static List<Map<String, Object>> issues(
            List<String> missingHandlerOperations,
            List<String> orphanHandlerOperations) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (String operation : operations(missingHandlerOperations)) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("source", "bindingReport");
            issue.put("field", "missingHandlerOperations");
            issue.put("operation", operation);
            issue.put("message", "A2UI HTTP route has no registered handler.");
            values.add(WayangA2uiTransportMaps.freeze(issue));
        }
        for (String operation : operations(orphanHandlerOperations)) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("source", "bindingReport");
            issue.put("field", "orphanHandlerOperations");
            issue.put("operation", operation);
            issue.put("message", "A2UI HTTP handler is not declared by the route catalog.");
            values.add(WayangA2uiTransportMaps.freeze(issue));
        }
        return List.copyOf(values);
    }

    private static List<String> operations(List<String> values) {
        return values == null ? List.of() : values;
    }
}
