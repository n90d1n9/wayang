package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aSendMessageResponse;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between A2A request/response records and Wayang Agent SPI records.
 */
public final class WayangA2aMessageMapper {

    public AgentRequest toAgentRequest(A2aSendMessageRequest request) {
        return toAgentRequest(request, false);
    }

    public AgentRequest toAgentRequest(A2aSendMessageRequest request, boolean stream) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        A2aMessage message = request.message();
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(WayangA2a.CONTEXT_KEY, a2aContext(request));

        AgentRequest.Builder builder = AgentRequest.builder()
                .requestId(message.messageId())
                .prompt(prompt(message))
                .tenantId(resolveTenant(request))
                .stream(stream)
                .context(context);
        if (message.contextId() != null) {
            builder.sessionId(message.contextId());
        }
        for (String skillId : allowedSkills(request)) {
            builder.skill(skillId);
        }
        return builder.build();
    }

    public A2aMessage toAgentMessage(AgentResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        return new A2aMessage(
                WayangA2aAgentResponseProjection.messageId(response),
                null,
                null,
                A2aRole.ROLE_AGENT,
                List.of(A2aPart.text(WayangA2aAgentResponseProjection.text(response))),
                WayangA2aAgentResponseProjection.metadata(response),
                List.of(),
                List.of());
    }

    public A2aTask toTask(String taskId, String contextId, AgentResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        A2aTaskState state = response.successful()
                ? A2aTaskState.TASK_STATE_COMPLETED
                : A2aTaskState.TASK_STATE_FAILED;
        A2aMessage statusMessage = response.successful()
                ? null
                : new A2aMessage(
                        WayangA2aAgentResponseProjection.messageId(response),
                        contextId,
                        taskId,
                        A2aRole.ROLE_AGENT,
                        List.of(A2aPart.text(WayangA2aAgentResponseProjection.text(response))),
                        WayangA2aAgentResponseProjection.metadata(response),
                        List.of(),
                        List.of());
        List<A2aArtifact> artifacts = response.successful() && WayangA2aMaps.optional(response.answer()) != null
                ? List.of(new A2aArtifact(taskId + "-answer", "answer", null,
                        List.of(A2aPart.text(response.answer())),
                        WayangA2aAgentResponseProjection.metadata(response),
                        List.of()))
                : List.of();
        return new A2aTask(
                WayangA2aMaps.required(taskId, "taskId"),
                WayangA2aMaps.optional(contextId),
                new A2aTaskStatus(state, statusMessage, response.completedAt().toString()),
                artifacts,
                List.of(),
                WayangA2aAgentResponseProjection.metadata(response));
    }

    public A2aSendMessageResponse toSendMessageResponse(String taskId, String contextId, AgentResponse response) {
        return A2aSendMessageResponse.task(toTask(taskId, contextId, response));
    }

    private static Map<String, Object> a2aContext(A2aSendMessageRequest request) {
        A2aMessage message = request.message();
        Map<String, Object> a2a = new LinkedHashMap<>();
        a2a.put(WayangA2a.MESSAGE_ID_KEY, message.messageId());
        if (message.contextId() != null) {
            a2a.put(WayangA2a.CONTEXT_ID_KEY, message.contextId());
        }
        if (message.taskId() != null) {
            a2a.put(WayangA2a.TASK_ID_KEY, message.taskId());
        }
        if (request.tenant() != null) {
            a2a.put(WayangA2a.TENANT_KEY, request.tenant());
        }
        if (!message.extensions().isEmpty()) {
            a2a.put(WayangA2a.EXTENSIONS_KEY, message.extensions());
        }
        if (!message.metadata().isEmpty()) {
            a2a.put(WayangA2a.METADATA_KEY, message.metadata());
        }
        if (request.configuration() != null) {
            a2a.put(WayangA2a.CONFIGURATION_KEY, request.configuration().toMap());
        }
        a2a.put(WayangA2a.PARTS_KEY, message.parts().stream().map(A2aPart::toMap).toList());
        return WayangA2aMaps.copyMap(a2a);
    }

    private static String prompt(A2aMessage message) {
        List<String> texts = message.parts().stream()
                .map(A2aPart::text)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        if (!texts.isEmpty()) {
            return String.join("\n", texts);
        }
        return "Process the attached A2A message parts.";
    }

    private static String resolveTenant(A2aSendMessageRequest request) {
        return WayangA2aTenantHints.fromSendMessageRequest(request).orElse("default");
    }

    private static List<String> allowedSkills(A2aSendMessageRequest request) {
        return WayangA2aSkillHints.allowedSkills(request);
    }
}
