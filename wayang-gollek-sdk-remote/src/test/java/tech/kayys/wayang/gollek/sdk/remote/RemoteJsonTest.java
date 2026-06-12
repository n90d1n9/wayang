package tech.kayys.wayang.gollek.sdk.remote;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteJsonTest {

    @Test
    void objectFieldParsesScalarNestedAndArrayMetadata() {
        Map<String, Object> metadata = RemoteJson.objectField("""
                {
                  "metadata": {
                    "tenant": "tenant-a",
                    "retry": 2,
                    "latencyMs": 1234567890123,
                    "score": 0.75,
                    "successful": true,
                    "ignored": null,
                    "nested": {"surface": "assistant-agent"},
                    "tags": ["rag", "mcp", 3, false]
                  }
                }
                """, "metadata");

        assertThat(metadata)
                .containsEntry("tenant", "tenant-a")
                .containsEntry("retry", 2)
                .containsEntry("latencyMs", 1_234_567_890_123L)
                .containsEntry("score", 0.75)
                .containsEntry("successful", true)
                .doesNotContainKey("ignored");
        assertThat(metadata.get("nested")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) metadata.get("nested");
        assertThat(nested).containsEntry("surface", "assistant-agent");

        assertThat(metadata.get("tags")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Object> tags = (List<Object>) metadata.get("tags");
        assertThat(tags).containsExactly("rag", "mcp", 3, false);
    }

    @Test
    void objectFieldHandlesEscapesAndMissingFields() {
        Map<String, Object> metadata = RemoteJson.objectField("""
                {"metadata":{"line":"a\\nb","quote":"say \\\"wayang\\\"","comma":"a,b"},"other":true}
                """, "metadata");

        assertThat(metadata)
                .containsEntry("line", "a\nb")
                .containsEntry("quote", "say \"wayang\"")
                .containsEntry("comma", "a,b");
        assertThat(RemoteJson.objectField("{}", "metadata")).isEmpty();
        assertThat(RemoteJson.objectField("{\"metadata\":true}", "metadata")).isEmpty();
    }

    @Test
    void objectParsesRootObjectForRemoteSkillDetails() {
        Map<String, Object> skill = RemoteJson.object("""
                {"id":"remote.rag","surfaceIds":["assistant-agent"],"metadata":{"priority":3}}
                """);

        assertThat(skill)
                .containsEntry("id", "remote.rag")
                .containsKey("surfaceIds")
                .containsKey("metadata");
        assertThat(skill.get("surfaceIds")).isEqualTo(List.of("assistant-agent"));
        assertThat(skill.get("metadata")).isEqualTo(Map.of("priority", 3));
    }
}
