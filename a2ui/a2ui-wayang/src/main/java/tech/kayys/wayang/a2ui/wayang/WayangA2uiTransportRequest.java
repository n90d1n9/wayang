package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;
import java.util.Objects;

/**
 * Transport-neutral inbound A2UI payload.
 */
public record WayangA2uiTransportRequest(
        WayangA2uiTransportPayloadKind kind,
        String body,
        Map<String, Object> dataPart) {

    public WayangA2uiTransportRequest {
        kind = Objects.requireNonNull(kind, WayangA2uiTransportFields.KIND);
        body = body == null ? "" : body;
        dataPart = TransportMaps.copy(dataPart);
    }

    public static WayangA2uiTransportRequest jsonLine(String line) {
        return new WayangA2uiTransportRequest(WayangA2uiTransportPayloadKind.JSON_LINE, line, Map.of());
    }

    public static WayangA2uiTransportRequest jsonl(String jsonl) {
        return new WayangA2uiTransportRequest(WayangA2uiTransportPayloadKind.JSONL, jsonl, Map.of());
    }

    public static WayangA2uiTransportRequest dataPart(String dataPart) {
        return new WayangA2uiTransportRequest(WayangA2uiTransportPayloadKind.DATA_PART_JSON, dataPart, Map.of());
    }

    public static WayangA2uiTransportRequest dataPart(Map<?, ?> dataPart) {
        return new WayangA2uiTransportRequest(
                WayangA2uiTransportPayloadKind.DATA_PART_MAP,
                "",
                TransportMaps.copy(dataPart));
    }

    public static WayangA2uiTransportRequest surfaceCatalog() {
        return new WayangA2uiTransportRequest(WayangA2uiTransportPayloadKind.SURFACE_CATALOG, "", Map.of());
    }

    public static WayangA2uiTransportRequest actionBindingReport() {
        return new WayangA2uiTransportRequest(WayangA2uiTransportPayloadKind.ACTION_BINDING_REPORT, "", Map.of());
    }

    public static WayangA2uiTransportRequest fromMap(Map<?, ?> values) {
        return WayangA2uiTransportRequestDecoder.fromMap(values);
    }

    public static WayangA2uiTransportRequest fromJson(String json) {
        return fromMap(TransportJson.map(
                json,
                "A2UI transport request JSON must not be blank",
                "Unable to decode A2UI transport request JSON"));
    }

    public Map<String, Object> toMap() {
        return WayangA2uiTransportEnvelope.request(kind, body, dataPart);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI transport request");
    }
}
