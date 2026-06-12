package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aScenarioDefinitionProjectionTest {

    @Test
    void keepsSharedScenarioDefinitionOrder() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("taskId", "task-definition");
        attributes.put("messageId", "message-definition");

        Map<String, Object> definition = WayangA2aScenarioDefinitionProjection.definition(
                "scenario-definition",
                " Scenario definition ",
                List.of("exchange-one", "exchange-two"),
                value -> single("exchange", value),
                attributes);

        assertThat(definition.keySet()).containsExactly(
                "id",
                "description",
                "exchangeCount",
                "exchanges",
                "attributes");
        assertThat(definition)
                .containsEntry("id", "scenario-definition")
                .containsEntry("description", "Scenario definition")
                .containsEntry("exchangeCount", 2);
        assertThat(WayangA2aMaps.objectList(definition.get("exchanges")))
                .extracting(exchange -> exchange.get("exchange"))
                .containsExactly("exchange-one", "exchange-two");
        assertThat(map(definition.get("attributes")).keySet()).containsExactly("taskId", "messageId");
    }

    @Test
    void omitsBlankDescriptionAndKeepsHttpAndJsonRpcScenarioShapeAligned() {
        WayangA2aHttpScenario httpScenario = new WayangA2aHttpScenario(
                "http-scenario",
                " ",
                List.of(WayangA2aHttpScenarioExchange.of(WayangA2aHttpRequest.get("/tasks/task-1"))),
                Map.of());
        WayangA2aJsonRpcScenario jsonRpcScenario = new WayangA2aJsonRpcScenario(
                "jsonrpc-scenario",
                null,
                List.of(WayangA2aJsonRpcScenarioExchange.of(WayangA2aJsonRpcRequest.of(
                        "rpc-1",
                        WayangA2aJsonRpcMethods.GET_TASK,
                        Map.of("id", "task-1")))),
                Map.of());

        assertThat(httpScenario.toMap().keySet()).containsExactly("id", "exchangeCount", "exchanges", "attributes");
        assertThat(jsonRpcScenario.toMap().keySet()).containsExactlyElementsOf(httpScenario.toMap().keySet());
        assertThat(httpScenario.toMap()).containsEntry("exchangeCount", 1);
        assertThat(jsonRpcScenario.toMap()).containsEntry("exchangeCount", 1);
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
