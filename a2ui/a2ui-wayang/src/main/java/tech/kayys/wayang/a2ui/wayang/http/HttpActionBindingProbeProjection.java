package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpActionBindingProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP action-binding probe results.
 */
public final class HttpActionBindingProbeProjection {

    private HttpActionBindingProbeProjection() {
    }

    public static Map<String, Object> actionBinding(WayangA2uiHttpActionBindingProbeResult result) {
        WayangA2uiHttpActionBindingProbeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("httpSuccessful", resolved.httpSuccessful());
        values.put("routeOperation", resolved.routeOperation());
        values.put("allow", resolved.allow());
        values.put(WayangA2uiTransportFields.OUTCOME, resolved.outcome());
        values.put("contentType", resolved.contentType());
        values.put(WayangA2uiTransportFields.MIME_TYPE, resolved.mimeType());
        values.put(WayangA2uiTransportFields.BODY_ENCODING, resolved.bodyEncoding());
        values.put("exchangeRoute", resolved.exchangeRoute());
        values.put("actionBindingResult", resolved.actionBindingResult());
        values.put("jsonContent", resolved.jsonContent());
        values.put(WayangA2uiTransportFields.COMPLETE, resolved.complete());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.POLICY_ACTION_COUNT, resolved.policyActionCount());
        values.put(WayangA2uiTransportFields.HANDLER_ACTION_COUNT, resolved.handlerActionCount());
        values.put(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, resolved.missingHandlerCount());
        values.put(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, resolved.orphanHandlerCount());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", resolved.issues());
        values.put("policyActions", resolved.policyActions());
        values.put("handlerActions", resolved.handlerActions());
        values.put("missingHandlerActions", resolved.missingHandlerActions());
        values.put("orphanHandlerActions", resolved.orphanHandlerActions());
        values.put(WayangA2uiTransportFields.METADATA, resolved.metadata());
        values.put(WayangA2uiTransportFields.BODY, resolved.body());
        values.put("headers", resolved.headers());
        return TransportMaps.freeze(values);
    }

    public static List<Map<String, Object>> issues(List<String> missingHandlerActions) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (String action : RecordCollections.nonNullList(missingHandlerActions)) {
            values.add(actionIssue(
                    "missingHandlerActions",
                    action,
                    "A2UI action policy allows an action with no registered handler."));
        }
        return List.copyOf(values);
    }

    private static Map<String, Object> actionIssue(
            String field,
            String action,
            String message) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("source", "actionBinding");
        values.put("field", field);
        values.put("action", action);
        values.put("message", message);
        return TransportMaps.freeze(values);
    }
}
