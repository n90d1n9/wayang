package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A2UI bound value for literal values, data-model paths, or both.
 */
public record A2uiBoundValue(Map<String, Object> value) {

    public A2uiBoundValue {
        value = A2uiValues.copyMap(value);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("bound value must contain a literal or path");
        }
    }

    public static A2uiBoundValue literalString(String value) {
        return of("literalString", value);
    }

    public static A2uiBoundValue literalNumber(Number value) {
        return of("literalNumber", value);
    }

    public static A2uiBoundValue literalBoolean(boolean value) {
        return of("literalBoolean", value);
    }

    public static A2uiBoundValue path(String path) {
        return of("path", A2uiValues.required(path, "path"));
    }

    public static A2uiBoundValue literalStringAtPath(String path, String value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("path", A2uiValues.required(path, "path"));
        payload.put("literalString", value);
        return new A2uiBoundValue(payload);
    }

    public Map<String, Object> toPayload() {
        return value;
    }

    private static A2uiBoundValue of(String key, Object value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, value);
        return new A2uiBoundValue(payload);
    }
}
