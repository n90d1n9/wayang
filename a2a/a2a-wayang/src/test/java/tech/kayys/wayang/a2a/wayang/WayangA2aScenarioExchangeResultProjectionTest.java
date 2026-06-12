package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aScenarioExchangeResultProjectionTest {

    @Test
    void httpExchangeResultKeepsOrderedFailureShape() {
        WayangA2aHttpScenarioExchange exchange = WayangA2aHttpScenarioExchange.of(
                WayangA2aHttpRequest.get("/tasks/task-missing"));
        WayangA2aHttpResponse response = WayangA2aHttpResponse
                .error(404, "task_missing", "Task missing")
                .withHeaders(WayangA2aHttpResponse.protocolHeaders("tasks/get"));
        WayangA2aHttpScenarioExchangeResult result = new WayangA2aHttpScenarioExchangeResult(
                1,
                exchange,
                response,
                WayangA2aHttpJson.read(response.body()));

        Map<String, Object> values = result.toMap();

        assertThat(values.keySet()).containsExactly(
                "index",
                "method",
                "path",
                "statusCode",
                "successful",
                "contentType",
                "operation",
                "error",
                "headers",
                "body");
        assertThat(values)
                .containsEntry("index", 1)
                .containsEntry("method", "GET")
                .containsEntry("path", "/tasks/task-missing")
                .containsEntry("statusCode", 404)
                .containsEntry("successful", false)
                .containsEntry("operation", "tasks/get");
        assertThat(map(values.get("error"))).containsEntry("code", "task_missing");
        assertThat(map(values.get("body"))).containsKey("error");
    }

    @Test
    void jsonRpcExchangeResultKeepsOrderedFailureShape() {
        WayangA2aJsonRpcScenarioExchange exchange = WayangA2aJsonRpcScenarioExchange.of(
                WayangA2aJsonRpcRequest.of("rpc-1", "unknown/method", Map.of()));
        WayangA2aHttpResponse response = WayangA2aHttpResponse.json(
                200,
                WayangA2aJsonRpcResponse
                        .error("rpc-1", WayangA2aJsonRpcError.methodNotFound("unknown/method"))
                        .toJson());
        WayangA2aJsonRpcScenarioExchangeResult result = new WayangA2aJsonRpcScenarioExchangeResult(
                2,
                exchange,
                response,
                WayangA2aHttpJson.read(response.body()),
                List.of());

        Map<String, Object> values = result.toMap();

        assertThat(values.keySet()).containsExactly(
                "index",
                "requestId",
                "method",
                "statusCode",
                "successful",
                "contentType",
                "error",
                "headers",
                "body");
        assertThat(values)
                .containsEntry("index", 2)
                .containsEntry("requestId", "rpc-1")
                .containsEntry("method", "unknown/method")
                .containsEntry("statusCode", 200)
                .containsEntry("successful", false);
        assertThat(map(values.get("error"))).containsEntry("code", WayangA2aJsonRpcError.METHOD_NOT_FOUND);
        assertThat(map(values.get("body")).keySet()).containsExactly("jsonrpc", "id", "error");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
