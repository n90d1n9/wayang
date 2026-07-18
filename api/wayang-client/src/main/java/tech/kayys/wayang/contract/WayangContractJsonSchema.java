package tech.kayys.wayang.contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WayangContractJsonSchema(
        WayangContractDescriptor contract,
        String id,
        Map<String, Object> document) {

    public WayangContractJsonSchema {
        contract = contract == null ? WayangContractDescriptors.empty() : contract;
        id = SdkText.trimToEmpty(id);
        document = copyDocument(document);
    }

    public WayangContractKey key() {
        return contract.key();
    }

    private static Map<String, Object> copyDocument(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(key);
            Object normalizedValue = copyValue(value);
            if (!normalizedKey.isEmpty() && normalizedValue != null) {
                copy.put(normalizedKey, normalizedValue);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                String normalizedKey = SdkText.trimToEmpty(String.valueOf(key));
                Object normalizedValue = copyValue(mapValue);
                if (!normalizedKey.isEmpty() && normalizedValue != null) {
                    copy.put(normalizedKey, normalizedValue);
                }
            });
            return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> copy = new ArrayList<>();
            iterable.forEach(item -> {
                Object normalized = copyValue(item);
                if (normalized != null) {
                    copy.add(normalized);
                }
            });
            return copy.isEmpty() ? List.of() : List.copyOf(copy);
        }
        return value;
    }
}
