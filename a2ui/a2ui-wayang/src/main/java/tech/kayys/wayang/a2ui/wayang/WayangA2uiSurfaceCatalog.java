package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.surface.SurfaceProjection;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Transport-friendly catalog of registered A2UI surface renderers.
 */
public record WayangA2uiSurfaceCatalog(
        List<String> surfaceKinds,
        List<WayangA2uiSurfaceDescriptor> descriptors) {

    public WayangA2uiSurfaceCatalog {
        surfaceKinds = surfaceKinds == null
                ? List.of()
                : surfaceKinds.stream()
                        .filter(kind -> kind != null && !kind.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
        descriptors = descriptors == null
                ? List.of()
                : descriptors.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    public static WayangA2uiSurfaceCatalog from(WayangA2uiSurfaceRegistry registry) {
        WayangA2uiSurfaceRegistry resolved = registry == null
                ? WayangA2uiSurfaceRegistry.readOnly()
                : registry;
        return new WayangA2uiSurfaceCatalog(resolved.surfaceKinds(), resolved.surfaceDescriptors());
    }

    public int descriptorCount() {
        return descriptors.size();
    }

    public List<WayangA2uiSurfaceDescriptor> descriptors(String kind) {
        String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
        return descriptors.stream()
                .filter(descriptor -> descriptor.kind().equals(normalizedKind))
                .toList();
    }

    public Map<String, Object> toMap() {
        return SurfaceProjection.catalog(this);
    }
}
