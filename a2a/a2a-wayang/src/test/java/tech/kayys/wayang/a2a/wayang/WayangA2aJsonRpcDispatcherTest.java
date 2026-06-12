package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageConfiguration;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDispatcherTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
            card(),
            store,
            request -> AgentResponse.builder()
                    .runId("run-1")
                    .requestId(request.requestId())
                    .answer("pong")
                    .strategy("react")
                    .build());

    @Test
    void exposesCompleteDispatchMethodsFromRegistry() {
        assertThat(dispatcher.dispatchMethods())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(dispatcher.methodDispatchCoverage().complete()).isTrue();
        assertThat(dispatcher.dispatchCoverage())
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(dispatcher.methodHandlerRegistrySnapshot().toMap())
                .containsEntry("reported", true)
                .containsEntry("groupCount", 3)
                .containsEntry("overridePolicy", "ALLOW_REPLACE");
    }

    @Test
    void dispatchesSendGetListAndCancel() {
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-1",
                "context-1",
                "task-1",
                "ping");

        WayangA2aHttpResponse send = dispatch("1", WayangA2aJsonRpcMethods.SEND_MESSAGE, sendRequest.toMap());

        assertThat(send.statusCode()).isEqualTo(200);
        assertThat(send.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(result(send)).containsKey("task");
        assertThat(status(resultTask(send))).containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
        assertThat(store.get("task-1")).isPresent();

        WayangA2aHttpResponse get = dispatch("2", WayangA2aJsonRpcMethods.GET_TASK, Map.of("id", "task-1"));
        assertThat(result(get)).containsEntry("id", "task-1");
        assertThat(status(result(get))).containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());

        WayangA2aHttpResponse list = dispatch("3", WayangA2aJsonRpcMethods.LIST_TASKS, Map.of(
                "contextId", "context-1",
                "pageSize", 10));
        assertThat(result(list))
                .containsEntry("totalSize", 1)
                .containsEntry("pageSize", 10);
        assertThat(listOfMaps(result(list), "tasks"))
                .singleElement()
                .satisfies(task -> assertThat(task).containsEntry("id", "task-1"));

        store.create(new A2aTask(
                "task-cancel",
                "context-cancel",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of()));

        WayangA2aHttpResponse cancel = dispatch("4", WayangA2aJsonRpcMethods.CANCEL_TASK, Map.of("id", "task-cancel"));
        assertThat(result(cancel)).containsEntry("id", "task-cancel");
        assertThat(status(result(cancel))).containsEntry("state", A2aTaskState.TASK_STATE_CANCELED.value());
    }

    @Test
    void dispatchesStreamingSendAndTaskSubscriptionEvents() {
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-2",
                "context-2",
                "task-2",
                "stream ping");

        WayangA2aHttpResponse stream = dispatch(
                "stream-1",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                sendRequest.toMap());

        assertThat(stream.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(stream.body())
                .contains("data: ")
                .contains("\"jsonrpc\":\"2.0\"")
                .contains("\"task\"");

        store.create(new A2aTask(
                "task-working",
                "context-working",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of()));
        store.updateStatus("task-working", A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING));

        WayangA2aHttpResponse subscribe = dispatch("stream-2", WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK, Map.of(
                "id", "task-working",
                "pageSize", 10));

        assertThat(subscribe.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(subscribe.body())
                .contains("data: ")
                .contains("\"task\"")
                .contains("\"statusUpdate\"");
    }

    @Test
    void dispatchesPushNotificationConfigMethodsAndExtendedAgentCard() {
        store.create(new A2aTask(
                "task-push",
                "context-push",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_SUBMITTED),
                List.of(),
                List.of(),
                Map.of()));

        WayangA2aHttpResponse create = dispatch("push-1",
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                Map.of(
                        "taskId", "task-push",
                        "configId", "primary",
                        "url", "https://hooks.example/a2a"));
        assertThat(result(create))
                .containsEntry("taskId", "task-push")
                .containsEntry("configId", "primary")
                .containsEntry("url", "https://hooks.example/a2a");

        WayangA2aHttpResponse get = dispatch("push-2",
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                Map.of("taskId", "task-push", "configId", "primary"));
        assertThat(result(get)).containsEntry("configId", "primary");

        WayangA2aHttpResponse list = dispatch("push-3",
                WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                Map.of("taskId", "task-push"));
        assertThat(listOfMaps(result(list), "configs"))
                .singleElement()
                .satisfies(config -> assertThat(config).containsEntry("url", "https://hooks.example/a2a"));

        WayangA2aHttpResponse delete = dispatch("push-4",
                WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                Map.of("taskId", "task-push", "configId", "primary"));
        assertThat(result(delete)).containsEntry("deleted", true);
        assertThat(store.listPushNotificationConfigs("task-push")).isEmpty();

        WayangA2aHttpResponse card = dispatch("card-1", WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD, Map.of());
        assertThat(result(card))
                .containsEntry("name", "wayang-test-agent")
                .containsEntry("version", "1.0.0");
    }

    @Test
    void returnsJsonRpcErrors() {
        WayangA2aHttpResponse methodNotFound = dispatch("error-1", "UnknownMethod", Map.of());
        assertThat(error(methodNotFound)).containsEntry("code", WayangA2aJsonRpcError.METHOD_NOT_FOUND);

        WayangA2aHttpResponse parseError = dispatcher.dispatchJson("{");
        assertThat(error(parseError)).containsEntry("code", WayangA2aJsonRpcError.PARSE_ERROR);

        WayangA2aHttpResponse taskNotFound = dispatch("error-2", WayangA2aJsonRpcMethods.GET_TASK, Map.of("id", "missing"));
        assertThat(error(taskNotFound)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
        assertThat(listOfMaps(error(taskNotFound), "data"))
                .singleElement()
                .satisfies(detail -> assertThat(detail).containsEntry("reason", "TASK_NOT_FOUND"));

        store.create(new A2aTask(
                "task-terminal",
                "context-terminal",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_COMPLETED),
                List.of(),
                List.of(),
                Map.of()));

        WayangA2aHttpResponse unsupported = dispatch(
                "error-3",
                WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK,
                Map.of("id", "task-terminal"));
        WayangA2aHttpResponse terminalCancel = dispatch(
                "error-4",
                WayangA2aJsonRpcMethods.CANCEL_TASK,
                Map.of("id", "task-terminal"));
        assertThat(error(unsupported)).containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION);
        assertThat(error(terminalCancel)).containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION);
    }

    @Test
    void validatesOptionalCapabilitiesBeforeJsonRpcExecution() {
        WayangA2aJsonRpcDispatcher noCapabilities = WayangA2aJsonRpcDispatcher.forExecution(
                A2aAgentCard.minimal(
                        "wayang-basic-agent",
                        "A basic A2A card",
                        "https://agents.example/a2a",
                        List.of(A2aAgentSkill.of("chat", "Chat", "Answer prompts", List.of("chat")))),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder()
                        .runId("run-basic")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());

        WayangA2aHttpResponse streaming = noCapabilities.dispatch(WayangA2aJsonRpcRequest.of(
                "cap-stream",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                WayangA2aSendMessageServiceTest.request(
                        "message-cap-stream",
                        "context-cap-stream",
                        "task-cap-stream",
                        "ping").toMap()));
        WayangA2aHttpResponse push = noCapabilities.dispatch(WayangA2aJsonRpcRequest.of(
                "cap-push",
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                Map.of("taskId", "task-cap-push", "configId", "primary", "url", "https://hooks.example/a2a")));
        WayangA2aHttpResponse extended = noCapabilities.dispatch(WayangA2aJsonRpcRequest.of(
                "cap-card",
                WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                Map.of()));

        assertThat(error(streaming)).containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION);
        assertThat(error(push)).containsEntry("code", WayangA2aJsonRpcError.PUSH_NOTIFICATION_NOT_SUPPORTED);
        assertThat(error(extended)).containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION);
    }

    @Test
    void validatesSendMessageRequestPayloadBeforeJsonRpcExecution() {
        InMemoryWayangA2aTaskStore payloadStore = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher payloadDispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                payloadStore,
                request -> AgentResponse.builder()
                        .runId("run-payload")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest agentRole = new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        "message-payload-role",
                        "context-payload-role",
                        "task-payload-role",
                        A2aRole.ROLE_AGENT,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of());
        A2aSendMessageRequest jsonPart = new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        "message-payload-input",
                        "context-payload-input",
                        "task-payload-input",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.data(Map.of("prompt", "ping"))),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of());
        A2aSendMessageRequest unknownSkill = new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        "message-payload-skill",
                        "context-payload-skill",
                        "task-payload-skill",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("missing-skill")));

        WayangA2aHttpResponse roleRejected = payloadDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "payload-role",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                agentRole.toMap()));
        WayangA2aHttpResponse inputRejected = payloadDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "payload-input",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                jsonPart.toMap()));
        WayangA2aHttpResponse skillRejected = payloadDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "payload-skill",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                unknownSkill.toMap()));

        assertThat(error(roleRejected)).containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS);
        assertThat(error(inputRejected)).containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS);
        assertThat(error(skillRejected)).containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS);
        assertThat(payloadStore.get("task-payload-role")).isEmpty();
        assertThat(payloadStore.get("task-payload-input")).isEmpty();
        assertThat(payloadStore.get("task-payload-skill")).isEmpty();
    }

    @Test
    void validatesSendMessageConfigurationBeforeJsonRpcExecution() {
        InMemoryWayangA2aTaskStore configStore = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher configDispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                card(),
                configStore,
                request -> AgentResponse.builder()
                        .runId("run-config")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest unsupportedOutputMode = WayangA2aSendMessageServiceTest.request(
                "message-config-mode",
                "context-config-mode",
                "task-config-mode",
                "ping",
                new A2aSendMessageConfiguration(
                        List.of("application/json"),
                        Map.of(),
                        null,
                        null));
        A2aSendMessageRequest wildcardOutputMode = WayangA2aSendMessageServiceTest.request(
                "message-config-wildcard",
                "context-config-wildcard",
                "task-config-wildcard",
                "ping",
                new A2aSendMessageConfiguration(
                        List.of("application/json", "text/*"),
                        Map.of(),
                        null,
                        null));

        WayangA2aHttpResponse modeRejected = configDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "config-mode",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                unsupportedOutputMode.toMap()));
        WayangA2aHttpResponse wildcardAccepted = configDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "config-wildcard",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                wildcardOutputMode.toMap()));

        assertThat(error(modeRejected)).containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS);
        assertThat(configStore.get("task-config-mode")).isEmpty();
        assertThat(status(resultTask(wildcardAccepted)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
        assertThat(configStore.get("task-config-wildcard")).isPresent();

        InMemoryWayangA2aTaskStore modeSkillStore = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher modeSkillDispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                cardWithModeSkills(),
                modeSkillStore,
                request -> AgentResponse.builder()
                        .runId("run-mode-skill")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest jsonSkillOutput = new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        "message-config-json-skill",
                        "context-config-json-skill",
                        "task-config-json-skill",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.data(Map.of("prompt", "ping"))),
                        Map.of(),
                        List.of(),
                        List.of()),
                new A2aSendMessageConfiguration(
                        List.of("application/json"),
                        Map.of(),
                        null,
                        null),
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("json")));
        A2aSendMessageRequest textSkillOutput = new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        "message-config-text-skill",
                        "context-config-text-skill",
                        "task-config-text-skill",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                new A2aSendMessageConfiguration(
                        List.of("application/json"),
                        Map.of(),
                        null,
                        null),
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("chat")));

        WayangA2aHttpResponse jsonSkillAccepted = modeSkillDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "config-json-skill",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                jsonSkillOutput.toMap()));
        WayangA2aHttpResponse textSkillRejected = modeSkillDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "config-text-skill",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                textSkillOutput.toMap()));

        assertThat(status(resultTask(jsonSkillAccepted)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
        assertThat(modeSkillStore.get("task-config-json-skill")).isPresent();
        assertThat(error(textSkillRejected)).containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS);
        assertThat(modeSkillStore.get("task-config-text-skill")).isEmpty();

        InMemoryWayangA2aTaskStore noPushStore = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher noPushDispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                A2aAgentCard.minimal(
                        "wayang-no-push-agent",
                        "A basic A2A card",
                        "https://agents.example/a2a",
                        List.of(A2aAgentSkill.of("chat", "Chat", "Answer prompts", List.of("chat")))),
                noPushStore,
                request -> AgentResponse.builder()
                        .runId("run-no-push")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest pushConfig = WayangA2aSendMessageServiceTest.request(
                "message-config-push",
                "context-config-push",
                "task-config-push",
                "ping",
                new A2aSendMessageConfiguration(
                        List.of("text/plain"),
                        Map.of(
                                "configId", "primary",
                                "url", "https://hooks.test/a2a"),
                        null,
                        null));

        WayangA2aHttpResponse pushRejected = noPushDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "config-push",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                pushConfig.toMap()));

        assertThat(error(pushRejected)).containsEntry("code", WayangA2aJsonRpcError.PUSH_NOTIFICATION_NOT_SUPPORTED);
        assertThat(noPushStore.get("task-config-push")).isEmpty();
    }

    @Test
    void validatesExplicitTenantAgainstAdvertisedInterfacesBeforeJsonRpcExecution() {
        InMemoryWayangA2aTaskStore tenantStore = new InMemoryWayangA2aTaskStore();
        WayangA2aJsonRpcDispatcher tenantDispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                cardForTenant("tenant-a"),
                tenantStore,
                request -> AgentResponse.builder()
                        .runId("run-tenant")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest wrongTenantRequest = tenantRequest("tenant-b", "task-tenant-wrong");
        A2aSendMessageRequest matchingTenantRequest = tenantRequest("tenant-a", "task-tenant-ok");

        WayangA2aHttpResponse rejected = tenantDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "tenant-wrong",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                wrongTenantRequest.toMap()));
        WayangA2aHttpResponse accepted = tenantDispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "tenant-ok",
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                matchingTenantRequest.toMap()));

        assertThat(error(rejected))
                .containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS)
                .containsEntry("message", "A2A tenant is not advertised by Agent Card: tenant-b.");
        assertThat(tenantStore.get("task-tenant-wrong")).isEmpty();
        assertThat(status(resultTask(accepted)))
                .containsEntry("state", A2aTaskState.TASK_STATE_COMPLETED.value());
    }

    @Test
    void scopesTaskReadsAndListsByTenantBeforeJsonRpcExecution() {
        store.create(task("task-tenant-a", "context-tenant", A2aTaskState.TASK_STATE_WORKING, "tenant-a"));
        store.create(task("task-tenant-b", "context-tenant", A2aTaskState.TASK_STATE_WORKING, "tenant-b"));

        WayangA2aHttpResponse hiddenGet = dispatch(
                "tenant-get",
                WayangA2aJsonRpcMethods.GET_TASK,
                Map.of("id", "task-tenant-b", "tenant", "tenant-a"));
        WayangA2aHttpResponse list = dispatch(
                "tenant-list",
                WayangA2aJsonRpcMethods.LIST_TASKS,
                Map.of("contextId", "context-tenant", "tenant", "tenant-a"));

        assertThat(error(hiddenGet)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
        assertThat(result(list)).containsEntry("totalSize", 1);
        assertThat(listOfMaps(result(list), "tasks"))
                .singleElement()
                .satisfies(task -> assertThat(task).containsEntry("id", "task-tenant-a"));
    }

    private WayangA2aHttpResponse dispatch(Object id, String method, Map<String, Object> params) {
        return dispatcher.dispatch(WayangA2aJsonRpcRequest.of(id, method, params));
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "wayang-test-agent",
                "A test A2A card",
                List.of(A2aAgentInterface.httpJson("https://agents.example/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(true, true, List.of(), true),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of(
                        "chat",
                        "Chat",
                        "Answer plain-text prompts",
                        List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aAgentCard cardForTenant(String tenant) {
        return new A2aAgentCard(
                "wayang-tenant-agent",
                "A tenant-scoped A2A card",
                List.of(new A2aAgentInterface(
                        "https://agents.example/a2a/" + tenant,
                        A2aProtocol.BINDING_HTTP_JSON,
                        tenant,
                        A2aProtocol.VERSION)),
                null,
                "1.0.0",
                null,
                A2aAgentCapabilities.basic(),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "Answer plain-text prompts", List.of("chat"))),
                List.of(),
                null);
    }

    private static A2aAgentCard cardWithModeSkills() {
        return new A2aAgentCard(
                "wayang-mode-agent",
                "An A2A card with skill-specific modes",
                List.of(A2aAgentInterface.httpJson("https://agents.example/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(true, true, List.of(), true),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(
                        new A2aAgentSkill(
                                "chat",
                                "Chat",
                                "Plain text chat",
                                List.of("chat"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("text/plain"),
                                List.of()),
                        new A2aAgentSkill(
                                "json",
                                "JSON",
                                "Structured JSON exchange",
                                List.of("json"),
                                List.of(),
                                List.of("application/json"),
                                List.of("application/json"),
                                List.of())),
                List.of(),
                null);
    }

    private static A2aSendMessageRequest tenantRequest(String tenant, String taskId) {
        return new A2aSendMessageRequest(
                tenant,
                WayangA2aSendMessageServiceTest.request(
                        "message-" + taskId,
                        "context-" + tenant,
                        taskId,
                        "ping").message(),
                null,
                Map.of());
    }

    private static A2aTask task(String id, String contextId, A2aTaskState state, String tenant) {
        return new A2aTask(
                id,
                contextId,
                A2aTaskStatus.of(state),
                List.of(),
                List.of(),
                Map.of("tenant", tenant));
    }

    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return map(WayangA2aHttpJson.read(response.body()).get("result"));
    }

    private static Map<String, Object> resultTask(WayangA2aHttpResponse response) {
        return map(result(response).get("task"));
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        return map(WayangA2aHttpJson.read(response.body()).get("error"));
    }

    private static Map<String, Object> status(Map<String, Object> task) {
        return map(task.get("status"));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    private static List<Map<String, Object>> listOfMaps(Map<String, Object> source, String key) {
        assertThat(source.get(key)).isInstanceOf(List.class);
        return ((List<?>) source.get(key)).stream()
                .map(value -> {
                    assertThat(value).isInstanceOf(Map.class);
                    return WayangA2aMaps.copyMap((Map<?, ?>) value);
                })
                .toList();
    }
}
