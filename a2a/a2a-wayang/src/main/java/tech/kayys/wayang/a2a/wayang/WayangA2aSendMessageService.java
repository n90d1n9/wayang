package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes A2A send-message requests and records task lifecycle transitions.
 */
public final class WayangA2aSendMessageService {

    private final WayangA2aTaskStore store;
    private final WayangA2aAgentExecutor executor;
    private final WayangA2aMessageMapper mapper;

    public WayangA2aSendMessageService(WayangA2aTaskStore store, WayangA2aAgentExecutor executor) {
        this(store, executor, new WayangA2aMessageMapper());
    }

    public WayangA2aSendMessageService(
            WayangA2aTaskStore store,
            WayangA2aAgentExecutor executor,
            WayangA2aMessageMapper mapper) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        this.store = store;
        this.executor = executor;
        this.mapper = mapper == null ? new WayangA2aMessageMapper() : mapper;
    }

    public WayangA2aSendMessageResult send(A2aSendMessageRequest request) {
        return execute(request, false);
    }

    public WayangA2aSendMessageResult stream(A2aSendMessageRequest request) {
        return execute(request, true);
    }

    private WayangA2aSendMessageResult execute(A2aSendMessageRequest request, boolean stream) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String taskId = WayangA2aSendMessageIdentity.taskId(request);
        String contextId = WayangA2aSendMessageIdentity.contextId(request, taskId);
        A2aTask task = store.create(initialTask(taskId, contextId, request));
        storePushNotificationConfig(taskId, request);
        store.appendMessage(taskId, request.message());
        store.updateStatus(taskId, status(A2aTaskState.TASK_STATE_WORKING, null));

        AgentRequest agentRequest = mapper.toAgentRequest(request, stream);
        AgentResponse response = executeAgent(agentRequest);
        task = applyResponse(taskId, contextId, response);
        return new WayangA2aSendMessageResult(request, agentRequest, response, task);
    }

    private void storePushNotificationConfig(String taskId, A2aSendMessageRequest request) {
        if (request.configuration() == null || request.configuration().taskPushNotificationConfig().isEmpty()) {
            return;
        }
        store.putPushNotificationConfig(WayangA2aPushNotificationConfig.fromMap(
                taskId,
                request.configuration().taskPushNotificationConfig()));
    }

    private AgentResponse executeAgent(AgentRequest request) {
        try {
            AgentResponse response = executor.execute(request);
            return response == null ? failedResponse(request, "A2A executor returned null response") : response;
        } catch (RuntimeException e) {
            return failedResponse(request, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private A2aTask applyResponse(String taskId, String contextId, AgentResponse response) {
        A2aMessage agentMessage = new A2aMessage(
                WayangA2aAgentResponseProjection.messageId(response),
                contextId,
                taskId,
                A2aRole.ROLE_AGENT,
                List.of(A2aPart.text(WayangA2aAgentResponseProjection.text(response))),
                WayangA2aAgentResponseProjection.metadata(response),
                List.of(),
                List.of());
        if (response.successful()) {
            store.appendMessage(taskId, agentMessage);
            if (WayangA2aMaps.optional(response.answer()) != null) {
                store.appendArtifact(taskId, new A2aArtifact(
                        taskId + "-answer",
                        "answer",
                        null,
                        List.of(A2aPart.text(response.answer())),
                        WayangA2aAgentResponseProjection.metadata(response),
                        List.of()));
            }
            return store.updateStatus(taskId, status(A2aTaskState.TASK_STATE_COMPLETED, null));
        }
        return store.updateStatus(taskId, status(A2aTaskState.TASK_STATE_FAILED, agentMessage));
    }

    private static A2aTask initialTask(String taskId, String contextId, A2aSendMessageRequest request) {
        return new A2aTask(
                taskId,
                contextId,
                status(A2aTaskState.TASK_STATE_SUBMITTED, null),
                List.of(),
                List.of(),
                initialMetadata(request));
    }

    private static Map<String, Object> initialMetadata(A2aSendMessageRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>(request.metadata());
        metadata.put("messageId", request.message().messageId());
        if (request.tenant() != null) {
            metadata.put("tenant", request.tenant());
        }
        return WayangA2aMaps.copyMap(metadata);
    }

    private static A2aTaskStatus status(A2aTaskState state, A2aMessage message) {
        return new A2aTaskStatus(state, message, Instant.now().toString());
    }

    private static AgentResponse failedResponse(AgentRequest request, String error) {
        return AgentResponse.builder()
                .runId(request.requestId() + "-failed")
                .requestId(request.requestId())
                .successful(false)
                .error(error)
                .strategy(request.strategy().id)
                .build();
    }
}
