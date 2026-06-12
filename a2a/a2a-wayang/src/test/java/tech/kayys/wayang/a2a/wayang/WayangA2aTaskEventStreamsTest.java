package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskEventStreamsTest {

    @Test
    void rendersHttpEventStreamFrames() {
        WayangA2aTaskEvent event = event();
        String body = WayangA2aTaskEventStreams.http(List.of(event));

        assertThat(event.toMap().keySet()).containsExactly(
                "sequence",
                "taskId",
                "contextId",
                "type",
                "payload",
                "timestamp");
        assertThat(body)
                .startsWith("data: {\"sequence\":")
                .contains("\"type\":\"task.status.updated\"")
                .endsWith("\n\n");
        assertThat(body.indexOf("\"taskId\"")).isGreaterThan(body.indexOf("\"sequence\""));
        assertThat(body.indexOf("\"contextId\"")).isGreaterThan(body.indexOf("\"taskId\""));
        assertThat(body.indexOf("\"type\"")).isGreaterThan(body.indexOf("\"contextId\""));
        assertThat(body.indexOf("\"payload\"")).isGreaterThan(body.indexOf("\"type\""));
        assertThat(body.indexOf("\"timestamp\"")).isGreaterThan(body.indexOf("\"payload\""));
    }

    @Test
    void rendersJsonRpcEventStreamFrames() {
        String body = WayangA2aTaskEventStreams.jsonRpc("request-1", List.of(event()));

        assertThat(body)
                .startsWith("data: ")
                .contains("\"jsonrpc\":\"2.0\"")
                .contains("\"id\":\"request-1\"")
                .contains("\"status\":\"working\"");
    }

    private static WayangA2aTaskEvent event() {
        return new WayangA2aTaskEvent(
                1,
                "task-1",
                "context-1",
                WayangA2aTaskEvent.TYPE_STATUS_UPDATED,
                Map.of("status", "working"),
                "2026-06-04T00:00:00Z");
    }
}
