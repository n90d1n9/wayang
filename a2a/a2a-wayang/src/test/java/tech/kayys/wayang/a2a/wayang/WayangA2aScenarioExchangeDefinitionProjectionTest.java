package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aScenarioExchangeDefinitionProjectionTest {

    @Test
    void httpExchangeDefinitionKeepsOrderedRequestFields() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        Map<String, Object> requestAttributes = new LinkedHashMap<>();
        requestAttributes.put("afterSequence", 0);
        requestAttributes.put("limit", 50);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", "subscribe");
        WayangA2aHttpScenarioExchange exchange = new WayangA2aHttpScenarioExchange(
                new WayangA2aHttpRequest(
                        "POST",
                        "/tasks/task-1:subscribe",
                        "",
                        headers,
                        requestAttributes),
                attributes);

        Map<String, Object> values = exchange.toMap();

        assertThat(values.keySet()).containsExactly(
                "method",
                "path",
                "headers",
                "requestAttributes",
                "attributes");
        assertThat(values)
                .containsEntry("method", "POST")
                .containsEntry("path", "/tasks/task-1:subscribe");
        assertThat(map(values.get("headers")).keySet()).containsExactly(WayangA2aHttpResponse.HEADER_ACCEPT);
        assertThat(map(values.get("requestAttributes")).keySet()).containsExactly("afterSequence", "limit");
        assertThat(map(values.get("attributes")).keySet()).containsExactly("name");
    }

    @Test
    void jsonRpcExchangeDefinitionKeepsOrderedRequestFields() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contextId", "context-1");
        params.put("pageSize", 50);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", "list");
        WayangA2aJsonRpcScenarioExchange exchange = new WayangA2aJsonRpcScenarioExchange(
                WayangA2aJsonRpcRequest.of("rpc-1", WayangA2aJsonRpcMethods.LIST_TASKS, params),
                attributes);

        Map<String, Object> values = exchange.toMap();

        assertThat(values.keySet()).containsExactly("requestId", "method", "params", "attributes");
        assertThat(values)
                .containsEntry("requestId", "rpc-1")
                .containsEntry("method", WayangA2aJsonRpcMethods.LIST_TASKS);
        assertThat(map(values.get("params")).keySet()).containsExactly("contextId", "pageSize");
        assertThat(map(values.get("attributes")).keySet()).containsExactly("name");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
