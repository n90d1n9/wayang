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

class WayangA2aJsonRpcHarnessTest {

    @Test
    void runsBuiltInSmokeScenarioThroughDispatcher() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                store,
                request -> AgentResponse.builder()
                        .runId("run-jsonrpc-smoke")
                        .requestId(request.requestId())
                        .answer("jsonrpc-smoke-ok")
                        .strategy("react")
                        .build());
        WayangA2aJsonRpcScenario scenario = WayangA2aJsonRpcScenarios.smoke(
                WayangA2aSendMessageServiceTest.request(
                        "message-jsonrpc-smoke",
                        "context-jsonrpc-smoke",
                        "task-jsonrpc-smoke",
                        "ping"));

        WayangA2aJsonRpcScenarioResult result = WayangA2aJsonRpcHarness.of(dispatcher).run(scenario);
        String scenarioJson = WayangA2aHttpJson.write(scenario.toMap());
        String resultJson = result.toJson();
        Map<String, Object> listParams = scenario.exchanges().stream()
                .filter(exchange -> WayangA2aJsonRpcMethods.LIST_TASKS.equals(exchange.request().method()))
                .findFirst()
                .map(exchange -> exchange.request().params())
                .orElseThrow();

        assertThat(result.passed()).isTrue();
        assertThat(result.exchangeCount()).isEqualTo(8);
        assertThat(result.issues()).isEmpty();
        assertThat(result.exchanges())
                .extracting(WayangA2aJsonRpcScenarioExchangeResult::method)
                .contains(
                        A2aProtocol.OPERATION_SEND_MESSAGE,
                        A2aProtocol.OPERATION_GET_TASK,
                        A2aProtocol.OPERATION_LIST_TASKS,
                        A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD);
        assertThat(result.exchanges())
                .filteredOn(exchange -> exchange.method().equals(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE))
                .singleElement()
                .satisfies(exchange -> {
                    assertThat(exchange.response().contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
                    assertThat(exchange.decodedEvents()).singleElement().satisfies(event ->
                            assertThat(map(event.get("result"))).containsKey("task"));
                });
        assertThat(store.get("task-jsonrpc-smoke").orElseThrow().status().state())
                .isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(store.get("task-jsonrpc-smoke-stream").orElseThrow().status().state())
                .isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(result.toMap())
                .containsEntry("scenarioId", "a2a.jsonrpc.smoke")
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
        assertThat(listParams.keySet()).containsExactly("contextId", "pageSize");
        assertThat(scenarioJson).startsWith("{\"id\":");
        assertThat(scenarioJson.indexOf("\"exchanges\""))
                .isGreaterThan(scenarioJson.indexOf("\"exchangeCount\""));
        assertThat(resultJson)
                .startsWith("{\"scenarioId\":")
                .contains("\"scenarioId\":\"a2a.jsonrpc.smoke\"");
        assertThat(resultJson.indexOf("\"exchanges\""))
                .isGreaterThan(resultJson.indexOf("\"issueCount\""));
        assertThat(resultJson.indexOf("\"attributes\""))
                .isGreaterThan(resultJson.indexOf("\"issues\""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void runsSmokeRunnerForOperationalEntrypoints() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                store,
                request -> AgentResponse.builder()
                        .runId("run-jsonrpc-runner")
                        .requestId(request.requestId())
                        .answer("jsonrpc-runner-ok")
                        .strategy("react")
                        .build());
        WayangA2aJsonRpcSmokeRunner runner = WayangA2aJsonRpcSmokeRunner.of(
                dispatcher,
                WayangA2aSendMessageServiceTest.request(
                        "message-jsonrpc-runner",
                        "context-jsonrpc-runner",
                        "task-jsonrpc-runner",
                        "ping"));

        WayangA2aJsonRpcSmokeResult result = runner.run();
        Map<String, Object> resultMap = result.toMap();
        String resultJson = result.toJson();

        assertThat(result.passed()).isTrue();
        assertThat(result.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(result.scenarioResult().exchangeCount()).isEqualTo(8);
        assertThat(result.scenarioResult().issues()).isEmpty();
        assertThat(result.attributes())
                .containsEntry("scenarioId", "a2a.jsonrpc.smoke")
                .containsEntry("taskId", "task-jsonrpc-runner")
                .containsEntry("streamingTaskId", "task-jsonrpc-runner-stream")
                .containsEntry("messageId", "message-jsonrpc-runner")
                .containsEntry("exchangeCount", 8);
        assertThat(resultMap)
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0);
        assertThat((Map<String, Object>) resultMap.get("scenarioResult"))
                .containsEntry("scenarioId", "a2a.jsonrpc.smoke")
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
        assertThat(resultJson)
                .startsWith("{\"passed\":")
                .contains("\"exitCode\":0")
                .contains("\"scenarioId\":\"a2a.jsonrpc.smoke\"");
        assertThat(resultJson.indexOf("\"scenarioResult\""))
                .isGreaterThan(resultJson.indexOf("\"exitCode\""));
        assertThat(resultJson.indexOf("\"attributes\""))
                .isGreaterThan(resultJson.indexOf("\"scenarioResult\""));
        assertThat(store.get("task-jsonrpc-runner").orElseThrow().status().state())
                .isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
    }

    @Test
    void summarizesSmokeRunnerResultForConsumers() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                store,
                request -> AgentResponse.builder()
                        .runId("run-jsonrpc-summary")
                        .requestId(request.requestId())
                        .answer("jsonrpc-summary-ok")
                        .strategy("react")
                        .build());
        WayangA2aJsonRpcSmokeResult smokeResult = WayangA2aJsonRpcSmokeRunner.of(
                dispatcher,
                WayangA2aSendMessageServiceTest.request(
                        "message-jsonrpc-summary",
                        "context-jsonrpc-summary",
                        "task-jsonrpc-summary",
                        "ping")).run();

        WayangA2aJsonRpcSmokeSummary summary = WayangA2aJsonRpcSmokeSummary.from(smokeResult);
        String summaryJson = summary.toJson();

        assertThat(summary.passed()).isTrue();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(summary.scenarioId()).isEqualTo("a2a.jsonrpc.smoke");
        assertThat(summary.exchangeCount()).isEqualTo(8);
        assertThat(summary.issueCount()).isZero();
        assertThat(summary.smokeResult()).isTrue();
        assertThat(summary.issues()).isEmpty();
        assertThat(summary.attributes())
                .containsEntry("taskId", "task-jsonrpc-summary")
                .containsEntry("messageId", "message-jsonrpc-summary");
        assertThat(summary.toMap())
                .containsEntry("passed", true)
                .containsEntry("successfulExit", true)
                .containsEntry("scenarioId", "a2a.jsonrpc.smoke");
        assertThat(summaryJson)
                .startsWith("{\"passed\":")
                .contains("\"successfulExit\":true");
        assertThat(summaryJson.indexOf("\"successfulExit\""))
                .isGreaterThan(summaryJson.indexOf("\"smokeResult\""));
        assertThat(summaryJson.indexOf("\"body\""))
                .isGreaterThan(summaryJson.indexOf("\"attributes\""));
    }

    @Test
    void probesSmokeRunnerResponseForOperationalDecision() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                store,
                request -> AgentResponse.builder()
                        .runId("run-jsonrpc-probe")
                        .requestId(request.requestId())
                        .answer("jsonrpc-probe-ok")
                        .strategy("react")
                        .build());
        WayangA2aJsonRpcSmokeRunner runner = WayangA2aJsonRpcSmokeRunner.of(
                dispatcher,
                WayangA2aSendMessageServiceTest.request(
                        "message-jsonrpc-probe",
                        "context-jsonrpc-probe",
                        "task-jsonrpc-probe",
                        "ping"));

        WayangA2aJsonRpcSmokeProbeResult probe = WayangA2aJsonRpcSmokeProbeResult.run(runner);
        String probeJson = probe.toJson();

        assertThat(probe.statusCode()).isEqualTo(200);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.routeOperation()).isEqualTo(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE);
        assertThat(probe.protocolVersion()).isEqualTo(A2aProtocol.VERSION);
        assertThat(probe.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(probe.smokeRoute()).isTrue();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS);
        assertThat(probe.summary().successfulExit()).isTrue();
        assertThat(probe.summary().scenarioId()).isEqualTo("a2a.jsonrpc.smoke");
        assertThat(probe.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE, 0)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO,
                        "a2a.jsonrpc.smoke");
        assertThat(probe.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO);
        assertThat(probe.toMap())
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0)
                .containsEntry("smokeRoute", true);
        assertThat(probeJson)
                .startsWith("{\"statusCode\":")
                .contains("\"routeOperation\":\"JsonRpcSmoke\"");
        assertThat(probeJson.indexOf("\"summary\""))
                .isGreaterThan(probeJson.indexOf("\"exitCode\""));
        assertThat(probeJson.indexOf("\"headers\""))
                .isGreaterThan(probeJson.indexOf("\"summary\""));
    }

    @Test
    void projectsJsonRpcErrorsIntoIssues() {
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder().build());
        WayangA2aJsonRpcScenario scenario = WayangA2aJsonRpcScenarios.methodError(
                "a2a.jsonrpc.bad-method",
                "UnknownMethod");

        WayangA2aJsonRpcScenarioResult result = WayangA2aJsonRpcHarness.of(dispatcher).run(scenario);

        assertThat(result.passed()).isFalse();
        assertThat(result.exchanges()).singleElement().satisfies(exchange -> {
            assertThat(exchange.response().statusCode()).isEqualTo(200);
            assertThat(exchange.successful()).isFalse();
            assertThat(exchange.error()).isPresent();
        });
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.scenarioId()).isEqualTo("a2a.jsonrpc.bad-method");
            assertThat(issue.statusCode()).isEqualTo(200);
            assertThat(issue.method()).isEqualTo("UnknownMethod");
            assertThat(issue.code()).isEqualTo(String.valueOf(WayangA2aJsonRpcError.METHOD_NOT_FOUND));
        });
        assertThat(result.toMap()).containsEntry("issueCount", 1);
    }

    @Test
    void mapsSmokeResultFailuresToFailureExitCode() {
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder().build());
        WayangA2aJsonRpcScenarioResult scenarioResult = WayangA2aJsonRpcHarness.of(dispatcher)
                .run(WayangA2aJsonRpcScenarios.methodError("a2a.jsonrpc.bad-smoke", "UnknownMethod"));

        WayangA2aJsonRpcSmokeResult result = new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                Map.of("scenarioId", "a2a.jsonrpc.bad-smoke"));

        assertThat(scenarioResult.passed()).isFalse();
        assertThat(result.passed()).isFalse();
        assertThat(result.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(result.toMap())
                .containsEntry("passed", false)
                .containsEntry("exitCode", 1);
        assertThat(result.toJson()).contains("\"code\":\"-32601\"");
    }

    @Test
    void summarizesFailureIssuesFromResultJson() {
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder().build());
        WayangA2aJsonRpcScenarioResult scenarioResult = WayangA2aJsonRpcHarness.of(dispatcher)
                .run(WayangA2aJsonRpcScenarios.methodError("a2a.jsonrpc.bad-summary", "UnknownMethod"));
        WayangA2aJsonRpcSmokeResult smokeResult = new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                Map.of("scenarioId", "a2a.jsonrpc.bad-summary"));

        WayangA2aJsonRpcSmokeSummary summary = WayangA2aJsonRpcSmokeSummary.fromResultJson(smokeResult.toJson());

        assertThat(summary.passed()).isFalse();
        assertThat(summary.successfulExit()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(summary.scenarioId()).isEqualTo("a2a.jsonrpc.bad-summary");
        assertThat(summary.exchangeCount()).isEqualTo(1);
        assertThat(summary.issueCount()).isEqualTo(1);
        assertThat(summary.issues()).singleElement().satisfies(issue -> {
            assertThat(issue).containsEntry("code", String.valueOf(WayangA2aJsonRpcError.METHOD_NOT_FOUND));
            assertThat(issue).containsEntry("method", "UnknownMethod");
            assertThat(issue).containsEntry("source", "scenario");
        });
        assertThat(summary.toMap())
                .containsEntry("passed", false)
                .containsEntry("successfulExit", false)
                .containsEntry("issueCount", 1);
        assertThat(summary.toJson()).contains("\"code\":\"-32601\"");
    }

    @Test
    void probesFailedSmokeResponseForOperationalDecision() {
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder().build());
        WayangA2aJsonRpcScenarioResult scenarioResult = WayangA2aJsonRpcHarness.of(dispatcher)
                .run(WayangA2aJsonRpcScenarios.methodError("a2a.jsonrpc.bad-probe", "UnknownMethod"));
        WayangA2aJsonRpcSmokeResult smokeResult = new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                Map.of("scenarioId", "a2a.jsonrpc.bad-probe"));

        WayangA2aHttpResponse response = WayangA2aJsonRpcSmokeProbeResult.response(smokeResult);
        WayangA2aJsonRpcSmokeProbeResult probe = WayangA2aJsonRpcSmokeProbeResult.from(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.smokeRoute()).isTrue();
        assertThat(probe.summary().issueCount()).isEqualTo(1);
        assertThat(probe.passed()).isFalse();
        assertThat(probe.exitCode()).isEqualTo(WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(probe.headers())
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED, false)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE, 1)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO,
                        "a2a.jsonrpc.bad-probe");
        assertThat(probe.toJson()).contains("\"code\":\"-32601\"");
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "Wayang",
                "A2A JSON-RPC endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(true, true, List.of(), true),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
