package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional capabilities exposed by an A2A agent.
 */
public record A2aAgentCapabilities(
        boolean streaming,
        boolean pushNotifications,
        List<A2aAgentExtension> extensions,
        boolean extendedAgentCard) {

    public A2aAgentCapabilities {
        extensions = A2aValues.copyRecords(extensions);
    }

    public static A2aAgentCapabilities basic() {
        return new A2aAgentCapabilities(false, false, List.of(), false);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (streaming) {
            payload.put("streaming", true);
        }
        if (pushNotifications) {
            payload.put("pushNotifications", true);
        }
        if (!extensions.isEmpty()) {
            payload.put("extensions", extensions.stream().map(A2aAgentExtension::toMap).toList());
        }
        if (extendedAgentCard) {
            payload.put("extendedAgentCard", true);
        }
        return A2aValues.copyMap(payload);
    }

    public static A2aAgentCapabilities fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aAgentCapabilities(
                A2aValues.booleanOrFalse(source, "streaming"),
                A2aValues.booleanOrFalse(source, "pushNotifications"),
                A2aValues.objectList(source.get("extensions"), "extensions").stream()
                        .map(A2aAgentExtension::fromMap)
                        .toList(),
                A2aValues.booleanOrFalse(source, "extendedAgentCard"));
    }
}
