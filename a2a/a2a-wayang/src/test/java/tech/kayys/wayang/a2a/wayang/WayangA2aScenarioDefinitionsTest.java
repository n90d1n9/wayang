package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aScenarioDefinitionsTest {

    @Test
    @SuppressWarnings("unchecked")
    void httpSmokeScenarioKeepsOrderedFixtureAttributes() {
        A2aSendMessageRequest request = request();

        WayangA2aHttpScenario scenario = WayangA2aHttpScenarios.smoke(request);
        WayangA2aHttpScenarioExchange subscription = scenario.exchanges().stream()
                .filter(exchange -> exchange.request().path().endsWith(":subscribe"))
                .findFirst()
                .orElseThrow();
        Map<String, Object> scenarioAttributes = (Map<String, Object>) scenario.toMap().get("attributes");
        Map<String, Object> subscriptionRequestAttributes =
                (Map<String, Object>) subscription.toMap().get("requestAttributes");
        Map<String, Object> subscriptionExchangeAttributes =
                (Map<String, Object>) subscription.toMap().get("attributes");

        assertThat(scenario.attributes().keySet()).containsExactly("taskId", "messageId");
        assertThat(scenario.attributes())
                .containsEntry("taskId", "task-scenario")
                .containsEntry("messageId", "message-scenario");
        assertThat(scenarioAttributes.keySet()).containsExactly("taskId", "messageId");
        assertThat(subscription.request().attributes().keySet()).containsExactly("afterSequence", "limit");
        assertThat(subscription.request().attributes())
                .containsEntry("afterSequence", 0)
                .containsEntry("limit", 50);
        assertThat(subscriptionRequestAttributes.keySet()).containsExactly("afterSequence", "limit");
        assertThat(subscriptionExchangeAttributes).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void jsonRpcSmokeScenarioKeepsOrderedFixtureParamsAndAttributes() {
        A2aSendMessageRequest request = request();

        WayangA2aJsonRpcScenario scenario = WayangA2aJsonRpcScenarios.smoke(request);
        Map<String, Object> scenarioAttributes = (Map<String, Object>) scenario.toMap().get("attributes");
        Map<String, Object> getParams = exchange(scenario, WayangA2aJsonRpcMethods.GET_TASK)
                .request()
                .params();
        Map<String, Object> pushCreateParams = exchange(
                scenario,
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG)
                .request()
                .params();
        Map<String, Object> pushGetParams = exchange(
                scenario,
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG)
                .request()
                .params();
        Map<String, Object> pushCreateProjection = (Map<String, Object>) exchange(
                scenario,
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG)
                .toMap()
                .get("params");

        assertThat(scenario.attributes().keySet())
                .containsExactly("taskId", "streamingTaskId", "messageId");
        assertThat(scenario.attributes())
                .containsEntry("taskId", "task-scenario")
                .containsEntry("streamingTaskId", "task-scenario-stream")
                .containsEntry("messageId", "message-scenario");
        assertThat(scenarioAttributes.keySet())
                .containsExactly("taskId", "streamingTaskId", "messageId");
        assertThat(getParams.keySet()).containsExactly("id");
        assertThat(pushCreateParams.keySet()).containsExactly("taskId", "configId", "url");
        assertThat(pushCreateParams)
                .containsEntry("taskId", "task-scenario")
                .containsEntry("configId", "smoke")
                .containsEntry("url", "https://hooks.example/a2a");
        assertThat(pushGetParams.keySet()).containsExactly("taskId", "id");
        assertThat(pushCreateProjection.keySet()).containsExactly("taskId", "configId", "url");
    }

    private static WayangA2aJsonRpcScenarioExchange exchange(
            WayangA2aJsonRpcScenario scenario,
            String method) {
        return scenario.exchanges().stream()
                .filter(exchange -> method.equals(exchange.request().method()))
                .findFirst()
                .orElseThrow();
    }

    private static A2aSendMessageRequest request() {
        return WayangA2aSendMessageServiceTest.request(
                "message-scenario",
                "context-scenario",
                "task-scenario",
                "ping");
    }
}
