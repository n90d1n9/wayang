package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component action metadata sent in a component definition.
 */
public record A2uiAction(String name, List<A2uiActionContextEntry> context) {

    public A2uiAction {
        name = A2uiValues.required(name, "name");
        context = context == null ? List.of() : List.copyOf(context);
    }

    public static A2uiAction of(String name, A2uiActionContextEntry... context) {
        return new A2uiAction(name, context == null ? List.of() : List.of(context));
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        if (!context.isEmpty()) {
            payload.put("context", context.stream()
                    .map(A2uiActionContextEntry::toPayload)
                    .toList());
        }
        return Map.copyOf(payload);
    }
}
