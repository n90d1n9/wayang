package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpBindingReportProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decodes HTTP binding-report probe responses into canonical probe results.
 */
public final class HttpBindingReportProbeResponseDecoder {

    public static WayangA2uiHttpBindingReportProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        HttpResponseBodyDecoder.Envelope envelope =
                HttpResponseBodyDecoder.lenientJsonEnvelope(
                        resolved.body(),
                        "A2UI binding report JSON must not be blank",
                        "Unable to decode A2UI binding report JSON");
        Map<String, Object> metadata = envelope.metadata();
        Map<String, Object> body = envelope.body();
        List<String> routeOperations = DecodeCollections.distinctTokens(body.get("routeOperations"));
        List<String> handlerOperations = DecodeCollections.distinctTokens(body.get("handlerOperations"));
        List<String> missingHandlerOperations =
                DecodeCollections.distinctTokens(body.get("missingHandlerOperations"));
        List<String> orphanHandlerOperations =
                DecodeCollections.distinctTokens(body.get("orphanHandlerOperations"));
        int missingHandlerCount = DecodeValues.nonNegativeInt(metadata.get(
                        WayangA2uiTransportFields.MISSING_HANDLER_COUNT),
                missingHandlerOperations.size());
        int orphanHandlerCount = DecodeValues.nonNegativeInt(metadata.get(
                        WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT),
                orphanHandlerOperations.size());
        List<Map<String, Object>> issues = HttpBindingReportProbeProjection.issues(
                missingHandlerOperations,
                orphanHandlerOperations);
        return new WayangA2uiHttpBindingReportProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION),
                resolved.header(WayangA2uiHttpResponse.HEADER_ALLOW),
                DecodeValues.text(
                        resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME),
                        envelope.outcome()),
                resolved.contentType(),
                envelope.mimeType(),
                envelope.bodyEncoding(),
                DecodeValues.bool(metadata.get(WayangA2uiTransportFields.COMPLETE),
                        DecodeValues.bool(body.get(WayangA2uiTransportFields.COMPLETE), false)),
                DecodeValues.nonNegativeInt(metadata.get(
                                WayangA2uiTransportFields.ROUTE_OPERATION_COUNT),
                        DecodeValues.nonNegativeInt(
                                body.get("routeOperationCount"),
                                routeOperations.size())),
                DecodeValues.nonNegativeInt(metadata.get(
                                WayangA2uiTransportFields.HANDLER_OPERATION_COUNT),
                        DecodeValues.nonNegativeInt(
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

    private HttpBindingReportProbeResponseDecoder() {
    }
}
