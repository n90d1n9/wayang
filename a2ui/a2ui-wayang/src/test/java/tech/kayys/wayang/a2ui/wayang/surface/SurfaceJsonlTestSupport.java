package tech.kayys.wayang.a2ui.wayang.surface;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

import java.io.IOException;
import java.util.List;

/**
 * Small JSONL codec/parser helper for surface renderer tests.
 */
final class SurfaceJsonlTestSupport {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final A2uiJsonlCodec codec = new A2uiJsonlCodec(objectMapper);

    String stream(List<? extends A2uiServerMessage> messages) {
        return codec.stream(messages);
    }

    JsonNode dataModelUpdate(List<? extends A2uiServerMessage> messages) throws IOException {
        return jsonLine(messages, 0);
    }

    JsonNode surfaceUpdate(List<? extends A2uiServerMessage> messages) throws IOException {
        return jsonLine(messages, 1);
    }

    JsonNode beginRendering(List<? extends A2uiServerMessage> messages) throws IOException {
        return jsonLine(messages, 2);
    }

    private JsonNode jsonLine(List<? extends A2uiServerMessage> messages, int index) throws IOException {
        String[] lines = stream(messages).strip().split("\\R");
        return objectMapper.readTree(lines[index]);
    }
}
