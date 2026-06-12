package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extracts the normalized Hermes learning signal from a completed agent run.
 */
public final class HermesLearningSignalFactory {

    public HermesLearningSignal from(AgentRequest request, AgentResponse response) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");
        List<AgentState.ReasoningStep> steps = safeSteps(response);
        return new HermesLearningSignal(
                request.requestId(),
                request.prompt(),
                response.answer(),
                response.successful(),
                steps,
                toolIds(steps),
                metadata(request, response),
                response.completedAt());
    }

    private static List<AgentState.ReasoningStep> safeSteps(AgentResponse response) {
        return response.steps() == null ? List.of() : response.steps();
    }

    private static List<String> toolIds(List<AgentState.ReasoningStep> steps) {
        return steps.stream()
                .map(AgentState.ReasoningStep::action)
                .filter(Objects::nonNull)
                .map(AgentState.AgentAction::skillId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private static Map<String, Object> metadata(AgentRequest request, AgentResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "tenantId", request.tenantId());
        putIfPresent(metadata, "sessionId", request.sessionId());
        putIfPresent(metadata, "userId", request.userId());
        putIfPresent(metadata, "modelId", request.modelId());
        putIfPresent(metadata, "strategy", response.strategy());
        putAll(metadata, request.context());
        putAll(metadata, request.parameters());
        return metadata;
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private static void putAll(Map<String, Object> metadata, Map<String, ?> values) {
        if (values != null) {
            metadata.putAll(values);
        }
    }
}
