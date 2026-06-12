package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSmokeResultProjectionTest {

    @Test
    void buildsOrderedSmokeAttributesFromScenario() {
        Map<String, Object> scenarioAttributes = new LinkedHashMap<>();
        scenarioAttributes.put("taskId", "task-smoke");
        scenarioAttributes.put("messageId", "message-smoke");
        WayangA2aJsonRpcScenario scenario = new WayangA2aJsonRpcScenario(
                "a2a.jsonrpc.smoke-test",
                "Smoke test",
                List.of(WayangA2aJsonRpcScenarioExchange.of(WayangA2aJsonRpcRequest.of(
                        "rpc-1",
                        WayangA2aJsonRpcMethods.GET_TASK,
                        Map.of("id", "task-smoke")))),
                scenarioAttributes);

        Map<String, Object> attributes = WayangA2aJsonRpcSmokeResultProjection.attributes(scenario);

        assertThat(attributes.keySet()).containsExactly("scenarioId", "exchangeCount", "taskId", "messageId");
        assertThat(attributes)
                .containsEntry("scenarioId", "a2a.jsonrpc.smoke-test")
                .containsEntry("exchangeCount", 1)
                .containsEntry("taskId", "task-smoke")
                .containsEntry("messageId", "message-smoke");
    }

    @Test
    void buildsOrderedSmokeResultEnvelope() {
        Map<String, Object> scenarioResult = new LinkedHashMap<>();
        scenarioResult.put("scenarioId", "a2a.jsonrpc.smoke-test");
        scenarioResult.put("passed", true);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("scenarioId", "a2a.jsonrpc.smoke-test");
        attributes.put("exchangeCount", 1);

        Map<String, Object> result = WayangA2aJsonRpcSmokeResultProjection.result(
                true,
                WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS,
                scenarioResult,
                attributes);

        assertThat(result.keySet()).containsExactly("passed", "exitCode", "scenarioResult", "attributes");
        assertThat(result)
                .containsEntry("passed", true)
                .containsEntry("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(map(result.get("scenarioResult")).keySet()).containsExactly("scenarioId", "passed");
        assertThat(map(result.get("attributes")).keySet()).containsExactly("scenarioId", "exchangeCount");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
