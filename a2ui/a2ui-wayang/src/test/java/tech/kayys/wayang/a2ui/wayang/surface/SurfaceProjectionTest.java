package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceDescriptor;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceRegistry;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SurfaceProjectionTest {

    @Test
    void projectsOrderedSurfaceDescriptorEnvelopeAndRecordDelegates() {
        WayangA2uiSurfaceDescriptor descriptor = new WayangA2uiSurfaceDescriptor(
                WayangA2uiSurfaceRegistry.RUN_STATUS,
                AgentRunStatus.class);

        Map<String, Object> values = SurfaceProjection.descriptor(descriptor);

        assertThat(descriptor.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("kind", "modelType", "modelSimpleName");
        assertThat(values)
                .containsEntry("kind", WayangA2uiSurfaceRegistry.RUN_STATUS)
                .containsEntry("modelType", AgentRunStatus.class.getName())
                .containsEntry("modelSimpleName", AgentRunStatus.class.getSimpleName());
    }

    @Test
    void projectsOrderedSurfaceCatalogEnvelopeAndRecordDelegates() {
        WayangA2uiSurfaceCatalog catalog = WayangA2uiSurfaceRegistry.readOnly().surfaceCatalog();

        Map<String, Object> values = SurfaceProjection.catalog(catalog);

        assertThat(catalog.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("surfaceKinds", "descriptorCount", "descriptors");
        assertThat(values).containsEntry("descriptorCount", catalog.descriptorCount());
        assertThat((List<String>) values.get("surfaceKinds"))
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS, WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat((Iterable<Map<String, Object>>) values.get("descriptors"))
                .anySatisfy(descriptor -> assertThat(descriptor)
                        .containsEntry("kind", WayangA2uiSurfaceRegistry.ACTION_RESULT)
                        .containsEntry("modelType", WayangA2uiActionResult.class.getName())
                        .containsEntry("modelSimpleName", WayangA2uiActionResult.class.getSimpleName()));
    }

    @Test
    void catalogProjectionKeepsDescriptorOrderAndDistinctSurfaceKinds() {
        WayangA2uiSurfaceCatalog catalog = new WayangA2uiSurfaceCatalog(
                List.of("custom.one", "custom.one", "custom.two"),
                List.of(
                        new WayangA2uiSurfaceDescriptor("custom.one", String.class),
                        new WayangA2uiSurfaceDescriptor("custom.two", Integer.class)));

        Map<String, Object> values = SurfaceProjection.catalog(catalog);

        assertThat((List<String>) values.get("surfaceKinds"))
                .containsExactly("custom.one", "custom.two");
        assertThat((Iterable<Map<String, Object>>) values.get("descriptors"))
                .extracting(descriptor -> descriptor.get("kind"))
                .containsExactly("custom.one", "custom.two");
    }
}
