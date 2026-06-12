package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI transport errors.
 */
public final class WayangA2uiTransportErrorDecoder {

    public static WayangA2uiTransportError fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return WayangA2uiTransportError.defaultError();
        }
        return new WayangA2uiTransportError(
                DecodeValues.text(values.get(WayangA2uiTransportFields.CODE)),
                DecodeValues.text(values.get(WayangA2uiTransportFields.MESSAGE)));
    }

    public static WayangA2uiTransportError fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI transport error JSON must not be blank",
                "Unable to decode A2UI transport error JSON"));
    }

    private WayangA2uiTransportErrorDecoder() {
    }
}
