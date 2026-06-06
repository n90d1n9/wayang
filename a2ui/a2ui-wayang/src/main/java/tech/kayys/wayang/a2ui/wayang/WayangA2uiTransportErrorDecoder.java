package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Canonical decoder for stored or remote A2UI transport errors.
 */
public final class WayangA2uiTransportErrorDecoder {

    public static WayangA2uiTransportError fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new WayangA2uiTransportError(
                    WayangA2uiTransportError.DEFAULT_CODE,
                    WayangA2uiTransportError.DEFAULT_MESSAGE);
        }
        return new WayangA2uiTransportError(
                WayangA2uiDecodeValues.text(values.get(WayangA2uiTransportFields.CODE)),
                WayangA2uiDecodeValues.text(values.get(WayangA2uiTransportFields.MESSAGE)));
    }

    public static WayangA2uiTransportError fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI transport error JSON must not be blank",
                "Unable to decode A2UI transport error JSON"));
    }

    private WayangA2uiTransportErrorDecoder() {
    }
}
