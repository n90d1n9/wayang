package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds or updates components on an A2UI surface.
 */
public record A2uiSurfaceUpdate(String surfaceId, List<A2uiComponent> components)
        implements A2uiServerMessage {

    public A2uiSurfaceUpdate {
        surfaceId = A2uiValues.optional(surfaceId);
        components = components == null ? List.of() : List.copyOf(components);
    }

    public static A2uiSurfaceUpdate of(String surfaceId, A2uiComponent... components) {
        return new A2uiSurfaceUpdate(surfaceId, components == null ? List.of() : List.of(components));
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        A2uiValues.putOptional(body, "surfaceId", surfaceId);
        body.put("components", components.stream()
                .map(A2uiComponent::toPayload)
                .toList());
        return A2uiValues.payload("surfaceUpdate", body);
    }
}
