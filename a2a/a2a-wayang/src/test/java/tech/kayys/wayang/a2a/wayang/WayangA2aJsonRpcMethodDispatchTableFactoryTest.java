package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodDispatchTableFactoryTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final A2aAgentCard agentCard = card("Wayang Extended");
    private final WayangA2aSendMessageService service = new WayangA2aSendMessageService(
            store,
            request -> AgentResponse.builder()
                    .runId("run-factory")
                    .requestId(request.requestId())
                    .answer("pong")
                    .strategy("react")
                    .build());

    @Test
    void createsCompleteDispatchTableFromCoreHandlerGroups() {
        WayangA2aJsonRpcMethodDispatchTable table =
                WayangA2aJsonRpcMethodDispatchTableFactory.create(agentCard, store, service);

        assertThat(table.complete()).isTrue();
        assertThat(table.methods()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(table.methodGroups().keySet()).containsExactlyElementsOf(
                WayangA2aJsonRpcMethods.methodGroups().keySet());
        assertThat(table.coverage().complete()).isTrue();
    }

    @Test
    void exposesOrderedCoreHandlerRegistryForDispatchTableAssembly() {
        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodDispatchTableFactory.coreHandlerRegistry(agentCard, store, service);

        assertThat(registry.groupNames()).containsExactly(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_AGENT_CARD);
        assertThat(registry.handlers().keySet()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(registry.groups())
                .extracting(group -> group.contribution().providerId())
                .containsExactly(
                        WayangA2aJsonRpcCoreMethodHandlerContributions.PROVIDER_SEND_MESSAGE,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.PROVIDER_TASK,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.PROVIDER_AGENT_CARD);
        assertThat(registry.groups().getFirst().contribution().toMap())
                .containsEntry("moduleId", WayangA2aJsonRpcCoreMethodHandlerContributions.MODULE_ID)
                .containsEntry("priority", 0);
    }

    @Test
    void appendsExtraProvidersAfterCoreProvidersForConfigurableAssembly() {
        WayangA2aJsonRpcMethodHandlerProvider provider = provider(
                "agent-card-override",
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                (request, preflight) -> WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                        request.id(),
                        Map.of("name", "Provided Extended")));

        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodDispatchTableFactory.coreHandlerRegistry(
                        agentCard,
                        store,
                        service,
                        List.of(provider));
        WayangA2aJsonRpcMethodDispatchTable table =
                WayangA2aJsonRpcMethodDispatchTableFactory.create(agentCard, store, service, List.of(provider));
        WayangA2aHttpResponse card = dispatch(
                table,
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                "card-extra",
                Map.of(),
                WayangA2aSendMessagePreflight.JsonRpcResult.empty());

        assertThat(registry.groupNames()).containsExactly(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_AGENT_CARD,
                "agent-card-override");
        assertThat(result(card)).containsEntry("name", "Provided Extended");
    }

    @Test
    void dispatchesSendTaskAndAgentCardHandlersFromAssembledTable() {
        WayangA2aJsonRpcMethodDispatchTable table =
                WayangA2aJsonRpcMethodDispatchTableFactory.create(agentCard, store, service);
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-factory",
                "context-factory",
                "task-factory",
                "ping");

        WayangA2aHttpResponse send = dispatch(
                table,
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                "send-1",
                Map.of(),
                preflight(sendRequest));
        WayangA2aHttpResponse get = dispatch(
                table,
                WayangA2aJsonRpcMethods.GET_TASK,
                "get-1",
                Map.of("id", "task-factory"),
                WayangA2aSendMessagePreflight.JsonRpcResult.empty());
        WayangA2aHttpResponse card = dispatch(
                table,
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                "card-1",
                Map.of(),
                WayangA2aSendMessagePreflight.JsonRpcResult.empty());

        assertThat(resultTask(send)).containsEntry("id", "task-factory");
        assertThat(result(get)).containsEntry("id", "task-factory");
        assertThat(result(card)).containsEntry("name", "Wayang Extended");
    }

    private static WayangA2aHttpResponse dispatch(
            WayangA2aJsonRpcMethodDispatchTable table,
            String method,
            Object id,
            Map<String, Object> params,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        return table.entry(method)
                .orElseThrow()
                .dispatch(WayangA2aJsonRpcRequest.of(id, method, params), preflight);
    }

    private static WayangA2aSendMessagePreflight.JsonRpcResult preflight(A2aSendMessageRequest request) {
        return new WayangA2aSendMessagePreflight.JsonRpcResult(Optional.of(request), Optional.empty());
    }

    private static Map<String, Object> resultTask(WayangA2aHttpResponse response) {
        return map(result(response).get("task"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return (Map<String, Object>) WayangA2aHttpJson.read(response.body()).get("result");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    private static A2aAgentCard card(String name) {
        return A2aAgentCard.minimal(
                name,
                "A2A endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }

    private static WayangA2aJsonRpcMethodHandlerProvider provider(
            String name,
            String method,
            WayangA2aJsonRpcMethodDispatchTable.Handler handler) {
        return () -> WayangA2aJsonRpcMethodHandlerGroup.of(name, Map.of(method, handler));
    }
}
