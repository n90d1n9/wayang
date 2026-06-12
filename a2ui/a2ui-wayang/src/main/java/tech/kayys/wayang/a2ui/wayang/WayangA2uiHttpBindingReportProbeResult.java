package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpBindingReportProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpBindingReportProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP-aware result for probing the A2UI binding report surface.
 */
public record WayangA2uiHttpBindingReportProbeResult(
        int statusCode,
        boolean httpSuccessful,
        String routeOperation,
        String allow,
        String outcome,
        String contentType,
        String mimeType,
        String bodyEncoding,
        boolean complete,
        int routeOperationCount,
        int handlerOperationCount,
        int missingHandlerCount,
        int orphanHandlerCount,
        int issueCount,
        List<Map<String, Object>> issues,
        List<String> routeOperations,
        List<String> handlerOperations,
        List<String> missingHandlerOperations,
        List<String> orphanHandlerOperations,
        Map<String, Object> metadata,
        Map<String, Object> body,
        Map<String, Object> headers) {

    public WayangA2uiHttpBindingReportProbeResult {
        statusCode = RecordNumbers.nonNegative(statusCode);
        routeOperation = RecordValues.text(routeOperation);
        allow = RecordValues.text(allow);
        outcome = RecordValues.text(outcome);
        contentType = RecordValues.text(contentType);
        mimeType = RecordValues.text(mimeType);
        bodyEncoding = RecordValues.text(bodyEncoding);
        routeOperationCount = RecordNumbers.nonNegative(routeOperationCount);
        handlerOperationCount = RecordNumbers.nonNegative(handlerOperationCount);
        missingHandlerCount = RecordNumbers.nonNegative(missingHandlerCount);
        orphanHandlerCount = RecordNumbers.nonNegative(orphanHandlerCount);
        issues = TransportMaps.copyMaps(issues);
        issueCount = Math.max(RecordNumbers.nonNegative(issueCount), issues.size());
        routeOperations = RecordCollections.copyList(routeOperations);
        handlerOperations = RecordCollections.copyList(handlerOperations);
        missingHandlerOperations = RecordCollections.copyList(missingHandlerOperations);
        orphanHandlerOperations = RecordCollections.copyList(orphanHandlerOperations);
        metadata = TransportMaps.copy(metadata);
        body = TransportMaps.copy(body);
        headers = TransportMaps.copy(headers);
    }

    public static WayangA2uiHttpBindingReportProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return from(resolved.bindingReportResponse());
    }

    public static WayangA2uiHttpBindingReportProbeResult from(WayangA2uiHttpResponse response) {
        return HttpBindingReportProbeResponseDecoder.from(response);
    }

    public static WayangA2uiHttpBindingReportProbeResult fromMap(Map<?, ?> values) {
        return WayangA2uiHttpBindingReportProbeResultDecoder.fromMap(values);
    }

    public static WayangA2uiHttpBindingReportProbeResult fromJson(String json) {
        return WayangA2uiHttpBindingReportProbeResultDecoder.fromJson(json);
    }

    public static WayangA2uiHttpBindingReportProbeResult empty() {
        return new WayangA2uiHttpBindingReportProbeResult(
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

    public boolean bindingReportRoute() {
        return WayangA2uiHttpRoute.OPERATION_BINDING_REPORT.equals(routeOperation);
    }

    public boolean bindingReportResult() {
        return WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT.equals(
                metadata.get(WayangA2uiTransportFields.RESPONSE_KIND));
    }

    public boolean jsonContent() {
        return WayangA2uiTransportContent.MIME_JSON.equals(contentType)
                && WayangA2uiTransportContent.MIME_JSON.equals(mimeType)
                && WayangA2uiTransportContent.ENCODING_JSON.equals(bodyEncoding);
    }

    public boolean hasRouteOperation(String operation) {
        String normalized = RecordValues.text(operation);
        return !normalized.isEmpty()
                && routeOperations.stream()
                        .map(RecordValues::text)
                        .anyMatch(normalized::equals);
    }

    public boolean requiresSmokeProbe() {
        return hasRouteOperation(WayangA2uiHttpRoute.OPERATION_SMOKE);
    }

    public boolean passed() {
        return httpSuccessful && bindingReportRoute() && bindingReportResult() && jsonContent() && complete;
    }

    public Map<String, Object> toMap() {
        return HttpBindingReportProbeProjection.bindingReport(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP binding report probe result");
    }

}
