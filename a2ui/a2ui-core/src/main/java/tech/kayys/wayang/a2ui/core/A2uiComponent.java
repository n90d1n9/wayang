package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A2UI component wrapper. The component map must contain exactly one component type.
 */
public record A2uiComponent(String id, Map<String, Object> component) {

    public A2uiComponent {
        id = A2uiValues.required(id, "id");
        component = A2uiValues.copyMap(component);
        if (component.size() != 1) {
            throw new IllegalArgumentException("component must contain exactly one component type");
        }
    }

    public static A2uiComponent of(String id, String type, Map<?, ?> properties) {
        Map<String, Object> component = new LinkedHashMap<>();
        component.put(A2uiValues.required(type, "type"), A2uiValues.copyMap(properties));
        return new A2uiComponent(id, component);
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("component", component);
        return Map.copyOf(payload);
    }
}
