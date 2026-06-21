package tech.kayys.gamelan.tool.builtin;

import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link HttpTool}.
 * Network calls are NOT made in unit tests — only parameter validation is tested here.
 * Integration tests would use a WireMock server.
 */
class HttpToolTest {

    private final HttpTool tool = new HttpTool();

    @Test
    void toolNameIsHttpGet() {
        assertThat(tool.toolName()).isEqualTo("http_get");
    }

    @Test
    void toolNamesIncludesAllMethods() {
        assertThat(tool.toolNames()).containsExactlyInAnyOrder(
                "http_get", "http_post", "http_put", "http_delete");
    }

    @Test
    void failsWhenUrlIsBlank() {
        ToolCall call = new ToolCall("http_get", Map.of("url", ""), "<tc/>");
        ToolResult result = tool.execute(call);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).contains("url");
    }

    @Test
    void failsWhenUrlHasNoScheme() {
        ToolCall call = new ToolCall("http_get", Map.of("url", "example.com"), "<tc/>");
        ToolResult result = tool.execute(call);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).contains("http");
    }

    @Test
    void failsWhenUrlIsMissing() {
        ToolCall call = new ToolCall("http_get", Map.of(), "<tc/>");
        ToolResult result = tool.execute(call);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void descriptionMentionsHttpMethods() {
        assertThat(tool.description()).containsIgnoringCase("GET");
        assertThat(tool.description()).containsIgnoringCase("POST");
    }

    @Test
    void parametersListUrl() {
        assertThat(tool.parameters()).anyMatch(p -> p.contains("url"));
    }

    @Test
    void supportsTools() {
        // HttpTool should handle all 4 method names
        for (String name : tool.toolNames()) {
            assertThat(name).startsWith("http_");
        }
    }
}
