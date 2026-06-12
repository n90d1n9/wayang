package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpActionBindingProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpActionBindingProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP-aware result for probing the A2UI action binding report through exchange.
 */
public record WayangA2uiHttpActionBindingProbeResult(
        int statusCode,
        boolean httpSuccessful,
        String routeOperation,
        String allow,
        String outcome,
        String contentType,
        String mimeType,
        String bodyEncoding,
        boolean complete,
        int policyActionCount,
        int handlerActionCount,
        int missingHandlerCount,
        int orphanHandlerCount,
        int issueCount,
        List<Map<String, Object>> issues,
        List<String> policyActions,
        List<String> handlerActions,
        List<String> missingHandlerActions,
        List<String> orphanHandlerActions,
        Map<String, Object> metadata,
        Map<String, Object> body,
        Map<String, Object> headers) {

    public WayangA2uiHttpActionBindingProbeResult {
        statusCode = RecordNumbers.nonNegative(statusCode);
        routeOperation = RecordValues.text(routeOperation);
        allow = RecordValues.text(allow);
        outcome = RecordValues.text(outcome);
        contentType = RecordValues.text(contentType);
        mimeType = RecordValues.text(mimeType);
        bodyEncoding = RecordValues.text(bodyEncoding);
        policyActionCount = RecordNumbers.nonNegative(policyActionCount);
        handlerActionCount = RecordNumbers.nonNegative(handlerActionCount);
        missingHandlerCount = RecordNumbers.nonNegative(missingHandlerCount);
        orphanHandlerCount = RecordNumbers.nonNegative(orphanHandlerCount);
        issues = TransportMaps.copyMaps(issues);
        issueCount = Math.max(RecordNumbers.nonNegative(issueCount), issues.size());
        policyActions = RecordCollections.copyList(policyActions);
        handlerActions = RecordCollections.copyList(handlerActions);
        missingHandlerActions = RecordCollections.copyList(missingHandlerActions);
        orphanHandlerActions = RecordCollections.copyList(orphanHandlerActions);
        metadata = TransportMaps.copy(metadata);
        body = TransportMaps.copy(body);
        headers = TransportMaps.copy(headers);
    }

    public static WayangA2uiHttpActionBindingProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return from(resolved.exchange(WayangA2uiTransportRequest.actionBindingReport().toJson()));
    }

    public static WayangA2uiHttpActionBindingProbeResult from(WayangA2uiHttpResponse response) {
        return HttpActionBindingProbeResponseDecoder.from(response);
    }

    public static WayangA2uiHttpActionBindingProbeResult fromMap(Map<?, ?> values) {
        return WayangA2uiHttpActionBindingProbeResultDecoder.fromMap(values);
    }

    public static WayangA2uiHttpActionBindingProbeResult fromJson(String json) {
        return WayangA2uiHttpActionBindingProbeResultDecoder.fromJson(json);
    }

    public static WayangA2uiHttpActionBindingProbeResult empty() {
        return new WayangA2uiHttpActionBindingProbeResult(
                0,
                false,
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of());
    }

    public boolean exchangeRoute() {
        return WayangA2uiHttpRoute.OPERATION_EXCHANGE.equals(routeOperation);
    }

    public boolean actionBindingResult() {
        return WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT.equals(
                metadata.get(WayangA2uiTransportFields.RESPONSE_KIND));
    }

    public boolean jsonContent() {
        return WayangA2uiTransportContent.MIME_JSON.equals(contentType)
                && WayangA2uiTransportContent.MIME_JSON.equals(mimeType)
                && WayangA2uiTransportContent.ENCODING_JSON.equals(bodyEncoding);
    }

    public boolean passed() {
        return httpSuccessful && exchangeRoute() && actionBindingResult() && jsonContent() && missingHandlerCount == 0;
    }

    public Map<String, Object> toMap() {
        return HttpActionBindingProbeProjection.actionBinding(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP action binding probe result");
    }

    public static WayangA2uiHttpActionBindingProbeResult compatibilityFallback() {
        return new WayangA2uiHttpActionBindingProbeResult(
                200,
                true,
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                "POST, OPTIONS",
                WayangA2uiTransportOutcome.SUCCESS.name(),
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                true,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT),
                Map.of(WayangA2uiTransportFields.COMPLETE, true),
                Map.of());
    }

    static WayangA2uiHttpActionBindingProbeResult passedWithoutProbe() {
        return compatibilityFallback();
    }
}
