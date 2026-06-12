package tech.kayys.wayang.a2ui.wayang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class A2uiContractAssert {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final A2uiJsonlCodec codec = new A2uiJsonlCodec(objectMapper);

    void matchesFixture(String resourcePath, List<? extends A2uiServerMessage> messages) throws IOException {
        String expected = resource(resourcePath);
        String actual = codec.stream(messages);
        assertThat(jsonLines(actual))
                .as("A2UI contract fixture %s", resourcePath)
                .containsExactlyElementsOf(jsonLines(expected));
    }

    void matchesJsonFixture(String resourcePath, String actualJson) throws IOException {
        JsonNode expected = objectMapper.readTree(resource(resourcePath));
        JsonNode actual = objectMapper.readTree(actualJson);
        assertThat(actual)
                .as("A2UI JSON contract fixture %s", resourcePath)
                .isEqualTo(expected);
    }

    private String resource(String resourcePath) throws IOException {
        InputStream stream = A2uiContractAssert.class.getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream).as("missing test resource %s", resourcePath).isNotNull();
        try (InputStream closeable = stream) {
            return new String(closeable.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<JsonNode> jsonLines(String jsonl) throws IOException {
        if (jsonl == null || jsonl.isBlank()) {
            return List.of();
        }
        List<JsonNode> nodes = new ArrayList<>();
        for (String line : jsonl.split("\\R")) {
            if (!line.isBlank()) {
                nodes.add(objectMapper.readTree(line));
            }
        }
        return List.copyOf(nodes);
    }
}
