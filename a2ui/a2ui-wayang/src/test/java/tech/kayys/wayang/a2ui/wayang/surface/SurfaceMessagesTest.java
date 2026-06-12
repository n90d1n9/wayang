package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SurfaceMessagesTest {

    @Test
    @SuppressWarnings("unchecked")
    void assemblesStandardSurfaceMessageSequence() {
        List<A2uiComponent> components = List.of(
                A2uiComponents.column("surface-1-root", List.of("surface-1-title")),
                A2uiComponents.text("surface-1-title", "Surface title"));

        List<A2uiServerMessage> messages = SurfaceMessages.standard(
                "surface-1",
                "surface-1-root",
                components,
                A2uiDataEntry.string("runId", "run-1"));

        assertThat(messages).hasSize(3);
        Map<String, Object> data = messages.get(0).toPayload();
        Map<String, Object> update = messages.get(1).toPayload();
        Map<String, Object> begin = messages.get(2).toPayload();

        assertThat(data).containsOnlyKeys("dataModelUpdate");
        assertThat(update).containsOnlyKeys("surfaceUpdate");
        assertThat(begin).containsOnlyKeys("beginRendering");

        Map<String, Object> dataModel = (Map<String, Object>) data.get("dataModelUpdate");
        Map<String, Object> surfaceUpdate = (Map<String, Object>) update.get("surfaceUpdate");
        Map<String, Object> beginRendering = (Map<String, Object>) begin.get("beginRendering");

        assertThat(dataModel).containsEntry("surfaceId", "surface-1");
        assertThat(surfaceUpdate).containsEntry("surfaceId", "surface-1");
        assertThat(beginRendering)
                .containsEntry("surfaceId", "surface-1")
                .containsEntry("root", "surface-1-root");
    }
}
