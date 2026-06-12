package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.surface.SurfaceProjection;

import java.util.Map;
import java.util.Objects;

/**
 * Public descriptor for one model-to-surface renderer binding.
 */
public record WayangA2uiSurfaceDescriptor(
        String kind,
        Class<?> modelType) {

    public WayangA2uiSurfaceDescriptor {
        kind = normalizeKind(kind);
        modelType = Objects.requireNonNull(modelType, "modelType");
    }

    public String modelTypeName() {
        return modelType.getName();
    }

    public boolean supportsModelType(Class<?> candidateType) {
        return candidateType != null && modelType.isAssignableFrom(candidateType);
    }

    public Map<String, Object> toMap() {
        return SurfaceProjection.descriptor(this);
    }

    static String normalizeKind(String kind) {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("surface kind must not be blank");
        }
        return kind.trim();
    }
}
