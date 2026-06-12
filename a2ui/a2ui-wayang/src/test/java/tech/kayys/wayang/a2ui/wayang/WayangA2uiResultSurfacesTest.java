package tech.kayys.wayang.a2ui.wayang;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiResultSurfacesTest {

    private final A2uiJsonlTestSupport jsonl = new A2uiJsonlTestSupport();

    @Test
    void rendersActionResultFeedbackWithTypedMetadata() throws Exception {
        WayangA2uiActionResult result = WayangA2uiActionResult.handled(
                "custom.action",
                "run-1",
                "No domain surface was returned.",
                List.of(),
                Map.of("attempts", 2, "terminal", false, "note", "fallback"));

        List<A2uiServerMessage> messages = WayangA2uiResultSurfaces.actionResult(result, 7);

        assertThat(messages).hasSize(3);
        JsonNode data = jsonl.dataModelUpdate(messages);
        assertThat(data.at("/dataModelUpdate/surfaceId").asText())
                .isEqualTo("wayang-action-result-7-custom-action");
        assertThat(data.toString())
                .contains("valueNumber")
                .contains("valueBoolean")
                .contains("fallback");

        JsonNode update = jsonl.surfaceUpdate(messages);
        assertThat(update.toString())
                .contains("Action handled")
                .contains("custom.action")
                .contains("run-1");

        JsonNode begin = jsonl.beginRendering(messages);
        assertThat(begin.at("/beginRendering/root").asText())
                .isEqualTo("wayang-action-result-7-custom-action-root");
    }
}
