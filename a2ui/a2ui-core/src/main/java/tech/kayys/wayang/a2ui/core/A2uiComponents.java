package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small helpers for standard-catalog-style component payloads.
 */
public final class A2uiComponents {

    private A2uiComponents() {
    }

    public static A2uiComponent text(String id, String text) {
        return text(id, A2uiBoundValue.literalString(text));
    }

    public static A2uiComponent text(String id, A2uiBoundValue text) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("text", text.toPayload());
        return A2uiComponent.of(id, "Text", properties);
    }

    public static A2uiComponent column(String id, List<String> children) {
        return container(id, "Column", children);
    }

    public static A2uiComponent row(String id, List<String> children) {
        return container(id, "Row", children);
    }

    public static A2uiComponent card(String id, String child) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("child", A2uiValues.required(child, "child"));
        return A2uiComponent.of(id, "Card", properties);
    }

    public static A2uiComponent button(String id, String child, A2uiAction action) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("child", A2uiValues.required(child, "child"));
        if (action != null) {
            properties.put("action", action.toPayload());
        }
        return A2uiComponent.of(id, "Button", properties);
    }

    public static Map<String, Object> explicitList(List<String> children) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("explicitList", children == null ? List.of() : List.copyOf(children));
        return Map.copyOf(payload);
    }

    private static A2uiComponent container(String id, String type, List<String> children) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("children", explicitList(children));
        return A2uiComponent.of(id, type, properties);
    }
}
