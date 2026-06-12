package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.Locale;
import java.util.Map;

/**
 * Canonical decoder for inbound Wayang A2UI transport request envelopes.
 */
public final class WayangA2uiTransportRequestDecoder {

    public static WayangA2uiTransportRequest fromMap(Map<?, ?> values) {
        Map<String, Object> request = TransportMaps.copy(values);
        return new WayangA2uiTransportRequest(
                payloadKind(request.get(WayangA2uiTransportFields.KIND)),
                DecodeValues.rawText(request.get(WayangA2uiTransportFields.BODY)),
                dataPart(request.get(WayangA2uiTransportFields.DATA_PART)));
    }

    public static WayangA2uiTransportPayloadKind payloadKind(Object value) {
        if (value instanceof WayangA2uiTransportPayloadKind requestKind) {
            return requestKind;
        }
        String text = DecodeValues.rawText(value);
        if (text.isBlank()) {
            throw new IllegalArgumentException("A2UI transport request kind must not be blank");
        }
        String normalized = normalizeKind(text);
        try {
            return WayangA2uiTransportPayloadKind.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported A2UI transport request kind: " + text, e);
        }
    }

    static Map<String, Object> dataPart(Object value) {
        if (value instanceof Map<?, ?> map) {
            return TransportMaps.copy(map);
        }
        return Map.of();
    }

    private static String normalizeKind(String value) {
        StringBuilder normalized = new StringBuilder();
        char previous = 0;
        for (char current : value.trim().toCharArray()) {
            if (current == '-' || current == '.' || current == ' ') {
                normalized.append('_');
            } else {
                if (Character.isUpperCase(current)
                        && previous != 0
                        && previous != '_'
                        && (Character.isLowerCase(previous) || Character.isDigit(previous))) {
                    normalized.append('_');
                }
                normalized.append(current);
            }
            previous = normalized.charAt(normalized.length() - 1);
        }
        return normalized.toString().toUpperCase(Locale.ROOT);
    }

    private WayangA2uiTransportRequestDecoder() {
    }
}
