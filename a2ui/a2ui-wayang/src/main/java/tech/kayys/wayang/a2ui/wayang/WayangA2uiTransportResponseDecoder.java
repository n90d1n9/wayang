package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Canonical decoder for inbound Wayang A2UI transport response envelopes.
 */
public final class WayangA2uiTransportResponseDecoder {

    public static WayangA2uiTransportResponse fromMap(Map<?, ?> values) {
        Map<String, Object> response = WayangA2uiTransportMaps.copy(values);
        return new WayangA2uiTransportResponse(
                WayangA2uiDecodeValues.rawText(response.get(WayangA2uiTransportFields.MIME_TYPE)),
                WayangA2uiDecodeValues.rawText(response.get(WayangA2uiTransportFields.BODY_ENCODING)),
                WayangA2uiDecodeValues.rawText(response.get(WayangA2uiTransportFields.BODY)),
                dataParts(response.get(WayangA2uiTransportFields.DATA_PARTS)),
                count(response.get(WayangA2uiTransportFields.HANDLED_COUNT), WayangA2uiTransportFields.HANDLED_COUNT),
                count(response.get(WayangA2uiTransportFields.REJECTED_COUNT), WayangA2uiTransportFields.REJECTED_COUNT),
                metadata(response.get(WayangA2uiTransportFields.METADATA)));
    }

    public static WayangA2uiTransportResponse fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI transport response JSON must not be blank",
                "Unable to decode A2UI transport response JSON"));
    }

    static long count(Object value, String fieldName) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = WayangA2uiDecodeValues.rawText(value).trim();
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A2UI transport response count must be numeric: " + fieldName, e);
        }
    }

    static List<Map<String, Object>> dataParts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(WayangA2uiTransportMaps::copy)
                    .toList();
        }
        if (value instanceof Map<?, ?> map) {
            return List.of(WayangA2uiTransportMaps.copy(map));
        }
        return List.of();
    }

    static Map<String, Object> metadata(Object value) {
        if (value instanceof Map<?, ?> map) {
            return WayangA2uiTransportMaps.copy(map);
        }
        return Map.of();
    }

    private WayangA2uiTransportResponseDecoder() {
    }
}
