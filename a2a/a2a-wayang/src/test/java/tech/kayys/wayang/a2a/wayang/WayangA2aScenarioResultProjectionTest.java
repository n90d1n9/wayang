package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aScenarioResultProjectionTest {

    @Test
    void keepsSharedAggregateResultOrder() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("taskId", "task-projection");
        attributes.put("messageId", "message-projection");

        Map<String, Object> result = WayangA2aScenarioResultProjection.result(
                "scenario-projection",
                true,
                1,
                List.of("exchange-one"),
                value -> single("exchange", value),
                List.of("issue-one", "issue-two"),
                value -> single("issue", value),
                attributes);

        assertThat(result.keySet()).containsExactly(
                "scenarioId",
                "passed",
                "exchangeCount",
                "issueCount",
                "exchanges",
                "issues",
                "attributes");
        assertThat(result)
                .containsEntry("scenarioId", "scenario-projection")
                .containsEntry("passed", true)
                .containsEntry("exchangeCount", 1)
                .containsEntry("issueCount", 2);
        List<Map<String, Object>> exchanges = WayangA2aMaps.objectList(result.get("exchanges"));
        assertThat(exchanges).hasSize(1);
        assertThat(exchanges.get(0)).containsEntry("exchange", "exchange-one");
        assertThat(WayangA2aMaps.objectList(result.get("issues")))
                .extracting(issue -> issue.get("issue"))
                .containsExactly("issue-one", "issue-two");
        assertThat(map(result.get("attributes")).keySet()).containsExactly("taskId", "messageId");
    }

    @Test
    void httpAndJsonRpcScenarioResultsSharePublicShape() {
        WayangA2aHttpScenarioResult httpResult = new WayangA2aHttpScenarioResult(
                WayangA2aHttpScenarios.routeError("http-scenario", WayangA2aHttpRequest.get("/message:send")),
                List.of(),
                List.of());
        WayangA2aJsonRpcScenarioResult jsonRpcResult = new WayangA2aJsonRpcScenarioResult(
                WayangA2aJsonRpcScenarios.methodError("jsonrpc-scenario", "unknown/method"),
                List.of(),
                List.of());

        assertThat(httpResult.toMap().keySet()).containsExactlyElementsOf(jsonRpcResult.toMap().keySet());
        assertThat(httpResult.toJson()).startsWith("{\"scenarioId\":");
        assertThat(jsonRpcResult.toJson()).startsWith("{\"scenarioId\":");
    }

    private static Map<String, Object> single(String key, Object value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(key, value);
        return values;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
