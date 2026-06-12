package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP binding report probe results.
 */
public final class WayangA2uiHttpBindingReportProbeResultDecoder {

    public static WayangA2uiHttpBindingReportProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = TransportMaps.copy(values);
        if (copy.isEmpty()) {
            return WayangA2uiHttpBindingReportProbeResult.empty();
        }
        List<String> routeOperations = DecodeCollections.distinctTokens(copy.get("routeOperations"));
        List<String> handlerOperations = DecodeCollections.distinctTokens(copy.get("handlerOperations"));
        List<String> missingHandlerOperations =
                DecodeCollections.distinctTokens(copy.get("missingHandlerOperations"));
        List<String> orphanHandlerOperations =
                DecodeCollections.distinctTokens(copy.get("orphanHandlerOperations"));
        List<Map<String, Object>> issues = TransportMaps.copyMapList(copy.get("issues"));
        return new WayangA2uiHttpBindingReportProbeResult(
                DecodeValues.nonNegativeInt(copy.get("statusCode"), 0),
                DecodeValues.bool(copy.get("httpSuccessful"), false),
                DecodeValues.text(copy.get("routeOperation"), ""),
                DecodeValues.text(copy.get("allow"), ""),
                DecodeValues.text(copy.get(WayangA2uiTransportFields.OUTCOME), ""),
                DecodeValues.text(copy.get("contentType"), ""),
                DecodeValues.text(copy.get(WayangA2uiTransportFields.MIME_TYPE), ""),
                DecodeValues.text(copy.get(WayangA2uiTransportFields.BODY_ENCODING), ""),
                DecodeValues.bool(copy.get(WayangA2uiTransportFields.COMPLETE), false),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT),
                        routeOperations.size()),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.HANDLER_OPERATION_COUNT),
                        handlerOperations.size()),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.MISSING_HANDLER_COUNT),
                        missingHandlerOperations.size()),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT),
                        orphanHandlerOperations.size()),
                DecodeValues.nonNegativeInt(copy.get(WayangA2uiTransportFields.ISSUE_COUNT), issues.size()),
                issues,
                routeOperations,
                handlerOperations,
                missingHandlerOperations,
                orphanHandlerOperations,
                TransportMaps.copyMap(copy.get(WayangA2uiTransportFields.METADATA)),
                TransportMaps.copyMap(copy.get(WayangA2uiTransportFields.BODY)),
                TransportMaps.copyMap(copy.get("headers")));
    }

    public static WayangA2uiHttpBindingReportProbeResult fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP binding report probe result JSON must not be blank",
                "Unable to decode A2UI HTTP binding report probe result JSON"));
    }

    private WayangA2uiHttpBindingReportProbeResultDecoder() {
    }
}
