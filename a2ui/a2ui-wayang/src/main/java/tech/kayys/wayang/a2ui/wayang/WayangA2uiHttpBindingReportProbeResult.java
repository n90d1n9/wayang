package tech.kayys.wayang.a2ui.wayang;

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
        statusCode = Math.max(0, statusCode);
        routeOperation = WayangA2uiDecodeValues.text(routeOperation);
        allow = WayangA2uiDecodeValues.text(allow);
        outcome = WayangA2uiDecodeValues.text(outcome);
        contentType = WayangA2uiDecodeValues.text(contentType);
        mimeType = WayangA2uiDecodeValues.text(mimeType);
        bodyEncoding = WayangA2uiDecodeValues.text(bodyEncoding);
        routeOperationCount = Math.max(0, routeOperationCount);
        handlerOperationCount = Math.max(0, handlerOperationCount);
        missingHandlerCount = Math.max(0, missingHandlerCount);
        orphanHandlerCount = Math.max(0, orphanHandlerCount);
        issues = WayangA2uiTransportMaps.copyMaps(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        routeOperations = routeOperations == null ? List.of() : List.copyOf(routeOperations);
        handlerOperations = handlerOperations == null ? List.of() : List.copyOf(handlerOperations);
        missingHandlerOperations = missingHandlerOperations == null ? List.of() : List.copyOf(missingHandlerOperations);
        orphanHandlerOperations = orphanHandlerOperations == null ? List.of() : List.copyOf(orphanHandlerOperations);
        metadata = WayangA2uiTransportMaps.copy(metadata);
        body = WayangA2uiTransportMaps.copy(body);
        headers = WayangA2uiTransportMaps.copy(headers);
    }

    public static WayangA2uiHttpBindingReportProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return from(resolved.bindingReportResponse());
    }

    public static WayangA2uiHttpBindingReportProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        BindingReportEnvelope envelope = envelope(resolved.body());
        Map<String, Object> metadata = envelope.metadata();
        Map<String, Object> body = envelope.body();
        List<String> routeOperations = WayangA2uiDecodeCollections.distinctTokens(body.get("routeOperations"));
        List<String> handlerOperations = WayangA2uiDecodeCollections.distinctTokens(body.get("handlerOperations"));
        List<String> missingHandlerOperations =
                WayangA2uiDecodeCollections.distinctTokens(body.get("missingHandlerOperations"));
        List<String> orphanHandlerOperations =
                WayangA2uiDecodeCollections.distinctTokens(body.get("orphanHandlerOperations"));
        int missingHandlerCount = WayangA2uiDecodeValues.nonNegativeInt(metadata.get(
                        WayangA2uiTransportFields.MISSING_HANDLER_COUNT),
                missingHandlerOperations.size());
        int orphanHandlerCount = WayangA2uiDecodeValues.nonNegativeInt(metadata.get(
                        WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT),
                orphanHandlerOperations.size());
        List<Map<String, Object>> issues = WayangA2uiHttpBindingReportProbeProjection.issues(
                missingHandlerOperations,
                orphanHandlerOperations);
        return new WayangA2uiHttpBindingReportProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                header(resolved, WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION),
                header(resolved, WayangA2uiHttpResponse.HEADER_ALLOW),
                WayangA2uiDecodeValues.text(
                        header(resolved, WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME),
                        envelope.outcome()),
                resolved.contentType(),
                envelope.mimeType(),
                envelope.bodyEncoding(),
                WayangA2uiDecodeValues.bool(metadata.get(WayangA2uiTransportFields.COMPLETE),
                        WayangA2uiDecodeValues.bool(body.get(WayangA2uiTransportFields.COMPLETE), false)),
                WayangA2uiDecodeValues.nonNegativeInt(metadata.get(
                                WayangA2uiTransportFields.ROUTE_OPERATION_COUNT),
                        WayangA2uiDecodeValues.nonNegativeInt(
                                body.get("routeOperationCount"),
                                routeOperations.size())),
                WayangA2uiDecodeValues.nonNegativeInt(metadata.get(
                                WayangA2uiTransportFields.HANDLER_OPERATION_COUNT),
                        WayangA2uiDecodeValues.nonNegativeInt(
                                body.get("handlerOperationCount"),
                                handlerOperations.size())),
                missingHandlerCount,
                orphanHandlerCount,
                Math.max(missingHandlerCount + orphanHandlerCount, issues.size()),
                issues,
                routeOperations,
                handlerOperations,
                missingHandlerOperations,
                orphanHandlerOperations,
                metadata,
                body,
                resolved.headers());
    }

    public static WayangA2uiHttpBindingReportProbeResult fromMap(Map<?, ?> values) {
        return WayangA2uiHttpBindingReportProbeResultDecoder.fromMap(values);
    }

    public static WayangA2uiHttpBindingReportProbeResult fromJson(String json) {
        return WayangA2uiHttpBindingReportProbeResultDecoder.fromJson(json);
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

    public boolean passed() {
        return httpSuccessful && bindingReportRoute() && bindingReportResult() && jsonContent() && complete;
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpBindingReportProbeProjection.bindingReport(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP binding report probe result");
    }

    private static BindingReportEnvelope envelope(String body) {
        if (body == null || body.isBlank()) {
            return BindingReportEnvelope.empty();
        }
        try {
            WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(body);
            return new BindingReportEnvelope(
                    transport.metadata(),
                    bodyMap(transport.body()),
                    transport.mimeType(),
                    transport.bodyEncoding(),
                    transport.outcome().name());
        } catch (IllegalArgumentException ignored) {
            return new BindingReportEnvelope(Map.of(), bodyMap(body), "", "", "");
        }
    }

    private static Map<String, Object> bodyMap(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return WayangA2uiTransportJson.map(
                    body,
                    "A2UI binding report JSON must not be blank",
                    "Unable to decode A2UI binding report JSON");
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

    private static String header(WayangA2uiHttpResponse response, String name) {
        Object value = response.headers().get(name);
        return WayangA2uiDecodeValues.rawText(value);
    }

    private record BindingReportEnvelope(
            Map<String, Object> metadata,
            Map<String, Object> body,
            String mimeType,
            String bodyEncoding,
            String outcome) {

        private BindingReportEnvelope {
            metadata = WayangA2uiTransportMaps.copy(metadata);
            body = WayangA2uiTransportMaps.copy(body);
            mimeType = WayangA2uiDecodeValues.text(mimeType);
            bodyEncoding = WayangA2uiDecodeValues.text(bodyEncoding);
            outcome = WayangA2uiDecodeValues.text(outcome);
        }

        private static BindingReportEnvelope empty() {
            return new BindingReportEnvelope(Map.of(), Map.of(), "", "", "");
        }
    }
}
