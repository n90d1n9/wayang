package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical decoder for stored, remote, and transport-backed A2UI action binding reports.
 */
public final class WayangA2uiActionBindingReportDecoder {

    public static WayangA2uiActionBindingReport from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return from(WayangA2uiTransportResponse.fromJson(resolved.body()));
    }

    public static WayangA2uiActionBindingReport from(WayangA2uiTransportResponse response) {
        WayangA2uiTransportResponse resolved = Objects.requireNonNull(response, "response");
        return fromJson(resolved.body());
    }

    public static WayangA2uiActionBindingReport fromMap(Map<?, ?> values) {
        Map<String, Object> copy = TransportMaps.copy(values);
        List<String> policyActions = DecodeCollections.distinctTokens(copy.get("policyActions"));
        List<String> handlerActions = DecodeCollections.distinctTokens(copy.get("handlerActions"));
        List<String> missingHandlerActions =
                DecodeCollections.distinctTokens(copy.get("missingHandlerActions"));
        List<String> orphanHandlerActions =
                DecodeCollections.distinctTokens(copy.get("orphanHandlerActions"));
        return new WayangA2uiActionBindingReport(
                policyActions,
                handlerActions,
                missingHandlerActions,
                orphanHandlerActions);
    }

    public static WayangA2uiActionBindingReport fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI action binding report JSON must not be blank",
                "Unable to decode A2UI action binding report JSON"));
    }

    private WayangA2uiActionBindingReportDecoder() {
    }
}
