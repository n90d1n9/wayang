package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One key/value entry in an A2UI action context adjacency list.
 */
public record A2uiActionContextEntry(String key, A2uiBoundValue value) {

    public A2uiActionContextEntry {
        key = A2uiValues.required(key, "key");
        value = Objects.requireNonNull(value, "value");
    }

    public static A2uiActionContextEntry literalString(String key, String value) {
        return new A2uiActionContextEntry(key, A2uiBoundValue.literalString(value));
    }

    public static A2uiActionContextEntry literalNumber(String key, Number value) {
        return new A2uiActionContextEntry(key, A2uiBoundValue.literalNumber(value));
    }

    public static A2uiActionContextEntry path(String key, String path) {
        return new A2uiActionContextEntry(key, A2uiBoundValue.path(path));
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("key", key);
        payload.put("value", value.toPayload());
        return Map.copyOf(payload);
    }
}
