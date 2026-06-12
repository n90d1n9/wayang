package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceDescriptor;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI surface descriptors and catalogs.
 */
public final class SurfaceProjection {

    private SurfaceProjection() {
    }

    public static Map<String, Object> descriptor(WayangA2uiSurfaceDescriptor descriptor) {
        WayangA2uiSurfaceDescriptor resolved = Objects.requireNonNull(descriptor, "descriptor");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", resolved.kind());
        values.put("modelType", resolved.modelTypeName());
        values.put("modelSimpleName", resolved.modelType().getSimpleName());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> catalog(WayangA2uiSurfaceCatalog catalog) {
        WayangA2uiSurfaceCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceKinds", resolved.surfaceKinds());
        values.put("descriptorCount", resolved.descriptorCount());
        values.put("descriptors", resolved.descriptors().stream()
                .map(SurfaceProjection::descriptor)
                .toList());
        return TransportMaps.freeze(values);
    }
}
