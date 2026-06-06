package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI surface descriptors and catalogs.
 */
final class WayangA2uiSurfaceProjection {

    private WayangA2uiSurfaceProjection() {
    }

    static Map<String, Object> descriptor(WayangA2uiSurfaceDescriptor descriptor) {
        WayangA2uiSurfaceDescriptor resolved = Objects.requireNonNull(descriptor, "descriptor");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", resolved.kind());
        values.put("modelType", resolved.modelTypeName());
        values.put("modelSimpleName", resolved.modelType().getSimpleName());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> catalog(WayangA2uiSurfaceCatalog catalog) {
        WayangA2uiSurfaceCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceKinds", resolved.surfaceKinds());
        values.put("descriptorCount", resolved.descriptorCount());
        values.put("descriptors", resolved.descriptors().stream()
                .map(WayangA2uiSurfaceProjection::descriptor)
                .toList());
        return WayangA2uiTransportMaps.freeze(values);
    }
}
