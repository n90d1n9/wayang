package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aScenarioIssueProjectionTest {

    @Test
    void httpScenarioIssueKeepsOrderedFailureShape() {
        WayangA2aHttpScenarioIssue issue = new WayangA2aHttpScenarioIssue(
                "http-scenario",
                2,
                "POST",
                "/message:send",
                405,
                " message/send ",
                " ",
                null);

        Map<String, Object> values = issue.toMap();

        assertThat(values.keySet()).containsExactly(
                "scenarioId",
                "exchangeIndex",
                "method",
                "path",
                "statusCode",
                "operation",
                "code",
                "message");
        assertThat(values)
                .containsEntry("scenarioId", "http-scenario")
                .containsEntry("exchangeIndex", 2)
                .containsEntry("method", "POST")
                .containsEntry("path", "/message:send")
                .containsEntry("statusCode", 405)
                .containsEntry("operation", "message/send")
                .containsEntry("code", "http_405")
                .containsEntry("message", "A2A HTTP exchange failed");
    }

    @Test
    void jsonRpcScenarioIssueKeepsOrderedFailureShape() {
        WayangA2aJsonRpcScenarioIssue issue = new WayangA2aJsonRpcScenarioIssue(
                "jsonrpc-scenario",
                3,
                "request-3",
                "message/send",
                200,
                null,
                " ");

        Map<String, Object> values = issue.toMap();

        assertThat(values.keySet()).containsExactly(
                "scenarioId",
                "exchangeIndex",
                "requestId",
                "method",
                "statusCode",
                "code",
                "message");
        assertThat(values)
                .containsEntry("scenarioId", "jsonrpc-scenario")
                .containsEntry("exchangeIndex", 3)
                .containsEntry("requestId", "request-3")
                .containsEntry("method", "message/send")
                .containsEntry("statusCode", 200)
                .containsEntry("code", "jsonrpc_error")
                .containsEntry("message", "A2A JSON-RPC exchange failed");
    }

    @Test
    void jsonRpcScenarioIssueUsesHttpFallbackForHttpFailures() {
        WayangA2aJsonRpcScenarioIssue issue = new WayangA2aJsonRpcScenarioIssue(
                "jsonrpc-scenario",
                4,
                null,
                "message/send",
                404,
                null,
                null);

        assertThat(issue.toMap().keySet()).containsExactly(
                "scenarioId",
                "exchangeIndex",
                "method",
                "statusCode",
                "code",
                "message");
        assertThat(issue.toMap()).containsEntry("code", "http_404");
    }
}
