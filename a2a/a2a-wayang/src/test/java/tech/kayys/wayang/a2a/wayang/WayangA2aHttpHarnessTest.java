package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpHarnessTest {

    @Test
    void runsBuiltInSmokeScenarioThroughDispatcher() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                card(),
                WayangA2aHttpHandlers.forExecution(store, request -> AgentResponse.builder()
                        .runId("run-smoke")
                        .requestId(request.requestId())
                        .answer("smoke-ok")
                        .strategy("react")
                        .build()));
        WayangA2aHttpScenario scenario = WayangA2aHttpScenarios.smoke(
                WayangA2aSendMessageServiceTest.request("message-smoke", "context-smoke", "task-smoke", "ping"));

        WayangA2aHttpScenarioResult result = WayangA2aHttpHarness.of(dispatcher).run(scenario);
        String scenarioJson = WayangA2aHttpJson.write(scenario.toMap());
        String resultJson = result.toJson();

        assertThat(result.passed()).isTrue();
        assertThat(result.exchangeCount()).isEqualTo(7);
        assertThat(result.issues()).isEmpty();
        assertThat(result.exchanges())
                .extracting(exchange -> exchange.operation().orElse(""))
                .contains(
                        A2aProtocol.OPERATION_DISCOVER_AGENT_CARD,
                        A2aProtocol.OPERATION_SEND_MESSAGE,
                        A2aProtocol.OPERATION_GET_TASK,
                        A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK,
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
        assertThat(store.get("task-smoke").orElseThrow().status().state())
                .isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(result.toMap())
                .containsEntry("scenarioId", "a2a.http.smoke")
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
        assertThat(scenarioJson).startsWith("{\"id\":");
        assertThat(scenarioJson.indexOf("\"exchanges\""))
                .isGreaterThan(scenarioJson.indexOf("\"exchangeCount\""));
        assertThat(resultJson)
                .startsWith("{\"scenarioId\":")
                .contains("\"scenarioId\":\"a2a.http.smoke\"");
        assertThat(resultJson.indexOf("\"exchanges\""))
                .isGreaterThan(resultJson.indexOf("\"issueCount\""));
        assertThat(resultJson.indexOf("\"attributes\""))
                .isGreaterThan(resultJson.indexOf("\"issues\""));
    }

    @Test
    void projectsRouteFailuresIntoIssues() {
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(card());
        WayangA2aHttpScenario scenario = WayangA2aHttpScenarios.routeError(
                "a2a.http.bad-method",
                WayangA2aHttpRequest.get("/message:send"));

        WayangA2aHttpScenarioResult result = WayangA2aHttpHarness.of(dispatcher).run(scenario);

        assertThat(result.passed()).isFalse();
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.scenarioId()).isEqualTo("a2a.http.bad-method");
            assertThat(issue.statusCode()).isEqualTo(405);
            assertThat(issue.operation()).isEqualTo(A2aProtocol.OPERATION_SEND_MESSAGE);
            assertThat(issue.code()).isEqualTo("method_not_allowed");
        });
        assertThat(result.toMap()).containsEntry("issueCount", 1);
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "Wayang",
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(true, true, List.of(), false),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }
}
