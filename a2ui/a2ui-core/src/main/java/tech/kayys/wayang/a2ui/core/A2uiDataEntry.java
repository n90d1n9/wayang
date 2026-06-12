package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One typed entry in a v0.8 A2UI data model adjacency list.
 */
public record A2uiDataEntry(String key, String valueField, Object value) {

    public A2uiDataEntry {
        key = A2uiValues.required(key, "key");
        valueField = A2uiValues.required(valueField, "valueField");
        value = A2uiValues.copyValue(value);
        if (!valueField.startsWith("value")) {
            throw new IllegalArgumentException("valueField must be an A2UI value* field");
        }
    }

    public static A2uiDataEntry string(String key, String value) {
        return new A2uiDataEntry(key, "valueString", value);
    }

    public static A2uiDataEntry number(String key, Number value) {
        return new A2uiDataEntry(key, "valueNumber", value);
    }

    public static A2uiDataEntry bool(String key, boolean value) {
        return new A2uiDataEntry(key, "valueBoolean", value);
    }

    public static A2uiDataEntry map(String key, List<A2uiDataEntry> entries) {
        List<Map<String, Object>> payload = entries == null
                ? List.of()
                : entries.stream().map(A2uiDataEntry::toPayload).toList();
        return new A2uiDataEntry(key, "valueMap", payload);
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("key", key);
        payload.put(valueField, value);
        return Map.copyOf(payload);
    }
}
