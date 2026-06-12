package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportProjection;

import java.util.Map;

/**
 * Transport-neutral problem detail for failed A2UI bridge exchanges.
 */
public record WayangA2uiTransportError(
        String code,
        String message) {

    public static final String DEFAULT_CODE = "transport_error";
    public static final String DEFAULT_MESSAGE = "A2UI transport request failed.";

    public WayangA2uiTransportError {
        code = code == null || code.isBlank() ? DEFAULT_CODE : code.trim();
        message = message == null || message.isBlank() ? DEFAULT_MESSAGE : message.trim();
    }

    public static WayangA2uiTransportError of(String code, String message) {
        return new WayangA2uiTransportError(code, message);
    }

    public static WayangA2uiTransportError defaultError() {
        return new WayangA2uiTransportError(DEFAULT_CODE, DEFAULT_MESSAGE);
    }

    public static WayangA2uiTransportError fromMap(Map<?, ?> values) {
        return WayangA2uiTransportErrorDecoder.fromMap(values);
    }

    public static WayangA2uiTransportError fromJson(String json) {
        return WayangA2uiTransportErrorDecoder.fromJson(json);
    }

    public Map<String, Object> toMap() {
        return TransportProjection.error(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI transport error");
    }
}
