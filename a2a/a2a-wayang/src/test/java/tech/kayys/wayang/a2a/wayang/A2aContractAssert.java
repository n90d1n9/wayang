package tech.kayys.wayang.a2a.wayang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

final class A2aContractAssert {

    private final ObjectMapper objectMapper = new ObjectMapper();

    void matchesJsonFixture(String resourcePath, String actualJson) throws IOException {
        JsonNode expected = objectMapper.readTree(resource(resourcePath));
        JsonNode actual = objectMapper.readTree(actualJson);
        assertThat(actual)
                .as("A2A JSON contract fixture %s", resourcePath)
                .isEqualTo(expected);
    }

    private String resource(String resourcePath) throws IOException {
        InputStream stream = A2aContractAssert.class.getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream).as("missing test resource %s", resourcePath).isNotNull();
        try (InputStream closeable = stream) {
            return new String(closeable.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
