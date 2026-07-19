package tech.kayys.wayang.agent.run;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangSecretRedactor;

/**
 * Redacting, null-preserving map boundary for operator-facing agent run lifecycle envelopes.
 */
final public class AgentRunEnvelopeMaps {

    private AgentRunEnvelopeMaps() {
    }

    public static Map<String, Object> copy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(key);
            if (!normalizedKey.isBlank()) {
                copy.put(normalizedKey, copyValue(value));
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof String text) {
            return WayangSecretRedactor.connectionString(text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                String normalizedKey = SdkText.trimToEmpty(String.valueOf(key));
                if (!normalizedKey.isBlank()) {
                    copy.put(normalizedKey, copyValue(nested));
                }
            });
            return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.ArrayList<Object> copy = new java.util.ArrayList<>();
            iterable.forEach(item -> copy.add(copyValue(item)));
            return copy.isEmpty() ? List.of() : Collections.unmodifiableList(copy);
        }
        return value;
    }
}
