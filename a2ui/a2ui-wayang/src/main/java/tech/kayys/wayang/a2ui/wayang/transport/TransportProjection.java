package tech.kayys.wayang.a2ui.wayang.transport;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportError;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportPayloadKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered transport envelope projections for A2UI bridge requests and responses.
 */
public final class TransportProjection {

    private TransportProjection() {
    }

    public static Map<String, Object> request(
            WayangA2uiTransportPayloadKind kind,
            String body,
            Map<?, ?> dataPart) {
        WayangA2uiTransportPayloadKind resolvedKind = Objects.requireNonNull(kind, "kind");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.KIND, resolvedKind.name());
        values.put(WayangA2uiTransportFields.BODY, body == null ? "" : body);
        values.put(WayangA2uiTransportFields.DATA_PART, TransportMaps.copy(dataPart));
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> response(
            String mimeType,
            String bodyEncoding,
            String body,
            List<? extends Map<?, ?>> dataParts,
            long handledCount,
            long rejectedCount,
            Map<?, ?> metadata,
            WayangA2uiTransportOutcome outcome,
            boolean empty) {
        WayangA2uiTransportOutcome resolvedOutcome = Objects.requireNonNull(outcome, "outcome");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.MIME_TYPE, mimeType);
        values.put(WayangA2uiTransportFields.BODY_ENCODING, bodyEncoding);
        values.put(WayangA2uiTransportFields.BODY, body == null ? "" : body);
        values.put(WayangA2uiTransportFields.DATA_PARTS, TransportMaps.copyMaps(dataParts));
        values.put(WayangA2uiTransportFields.HANDLED_COUNT, handledCount);
        values.put(WayangA2uiTransportFields.REJECTED_COUNT, rejectedCount);
        values.put(WayangA2uiTransportFields.METADATA, TransportMaps.copy(metadata));
        values.put(WayangA2uiTransportFields.OUTCOME, resolvedOutcome.name());
        values.put(WayangA2uiTransportFields.EMPTY, empty);
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> error(WayangA2uiTransportError error) {
        WayangA2uiTransportError resolved = Objects.requireNonNull(error, "error");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.CODE, resolved.code());
        values.put(WayangA2uiTransportFields.MESSAGE, resolved.message());
        return TransportMaps.freeze(values);
    }
}
