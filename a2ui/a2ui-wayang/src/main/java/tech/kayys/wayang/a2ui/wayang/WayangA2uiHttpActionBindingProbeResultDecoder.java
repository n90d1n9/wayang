package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpActionBindingProbeProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI HTTP action-binding probe results.
 */
public final class WayangA2uiHttpActionBindingProbeResultDecoder {

    public static WayangA2uiHttpActionBindingProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = TransportMaps.copy(values);
        if (copy.isEmpty()) {
            return WayangA2uiHttpActionBindingProbeResult.empty();
        }
        List<String> policyActions = DecodeCollections.distinctTokens(copy.get("policyActions"));
        List<String> handlerActions = DecodeCollections.distinctTokens(copy.get("handlerActions"));
        List<String> missingHandlerActions =
                DecodeCollections.distinctTokens(copy.get("missingHandlerActions"));
        List<String> orphanHandlerActions =
                DecodeCollections.distinctTokens(copy.get("orphanHandlerActions"));
        List<Map<String, Object>> issues = TransportMaps.copyMapList(copy.get("issues"));
        if (issues.isEmpty()) {
            issues = HttpActionBindingProbeProjection.issues(missingHandlerActions);
        }
        int missingHandlerCount = DecodeValues.nonNegativeInt(
                copy.get(WayangA2uiTransportFields.MISSING_HANDLER_COUNT),
                missingHandlerActions.size());
        return new WayangA2uiHttpActionBindingProbeResult(
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
                        copy.get(WayangA2uiTransportFields.POLICY_ACTION_COUNT),
                        policyActions.size()),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.HANDLER_ACTION_COUNT),
                        handlerActions.size()),
                missingHandlerCount,
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT),
                        orphanHandlerActions.size()),
                DecodeValues.nonNegativeInt(
                        copy.get(WayangA2uiTransportFields.ISSUE_COUNT),
                        Math.max(missingHandlerCount, issues.size())),
                issues,
                policyActions,
                handlerActions,
                missingHandlerActions,
                orphanHandlerActions,
                TransportMaps.copyMap(copy.get(WayangA2uiTransportFields.METADATA)),
                TransportMaps.copyMap(copy.get(WayangA2uiTransportFields.BODY)),
                TransportMaps.copyMap(copy.get("headers")));
    }

    public static WayangA2uiHttpActionBindingProbeResult fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI HTTP action binding probe result JSON must not be blank",
                "Unable to decode A2UI HTTP action binding probe result JSON"));
    }

    private WayangA2uiHttpActionBindingProbeResultDecoder() {
    }
}
