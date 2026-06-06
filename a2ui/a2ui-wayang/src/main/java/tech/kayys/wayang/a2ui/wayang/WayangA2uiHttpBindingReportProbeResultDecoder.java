package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP binding report probe results.
 */
public final class WayangA2uiHttpBindingReportProbeResultDecoder {

    public static WayangA2uiHttpBindingReportProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2uiTransportMaps.copy(values);
        List<String> routeOperations = WayangA2uiDecodeCollections.distinctTokens(copy.get("routeOperations"));
        List<String> handlerOperations = WayangA2uiDecodeCollections.distinctTokens(copy.get("handlerOperations"));
        List<String> missingHandlerOperations =
                WayangA2uiDecodeCollections.distinctTokens(copy.get("missingHandlerOperations"));
        List<String> orphanHandlerOperations =
                WayangA2uiDecodeCollections.distinctTokens(copy.get("orphanHandlerOperations"));
        List<Map<String, Object>> issues = WayangA2uiTransportMaps.copyMapList(copy.get("issues"));
        return new WayangA2uiHttpBindingReportProbeResult(
                WayangA2uiDecodeValues.nonNegativeInt(copy.get("statusCode"), 0),
                WayangA2uiDecodeValues.bool(copy.get("httpSuccessful"), false),
                WayangA2uiDecodeValues.text(copy.get("routeOperation"), ""),
                WayangA2uiDecodeValues.text(copy.get("allow"), ""),
                WayangA2uiDecodeValues.text(copy.get(WayangA2uiTransportFields.OUTCOME), ""),
                WayangA2uiDecodeValues.text(copy.get("contentType"), ""),
                WayangA2uiDecodeValues.text(copy.get(WayangA2uiTransportFields.MIME_TYPE), ""),
                WayangA2uiDecodeValues.text(copy.get(WayangA2uiTransportFields.BODY_ENCODING), ""),
                WayangA2uiDecodeValues.bool(copy.get(WayangA2uiTransportFields.COMPLETE), false),
                WayangA2uiDecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT),
                        routeOperations.size()),
                WayangA2uiDecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.HANDLER_OPERATION_COUNT),
                        handlerOperations.size()),
                WayangA2uiDecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.MISSING_HANDLER_COUNT),
                        missingHandlerOperations.size()),
                WayangA2uiDecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT),
                        orphanHandlerOperations.size()),
                WayangA2uiDecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.ISSUE_COUNT), issues.size()),
                issues,
                routeOperations,
                handlerOperations,
                missingHandlerOperations,
                orphanHandlerOperations,
                WayangA2uiTransportMaps.copyMap(copy.get(WayangA2uiTransportFields.METADATA)),
                WayangA2uiTransportMaps.copyMap(copy.get(WayangA2uiTransportFields.BODY)),
                WayangA2uiTransportMaps.copyMap(copy.get("headers")));
    }

    public static WayangA2uiHttpBindingReportProbeResult fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI HTTP binding report probe result JSON must not be blank",
                "Unable to decode A2UI HTTP binding report probe result JSON"));
    }

    private WayangA2uiHttpBindingReportProbeResultDecoder() {
    }
}
