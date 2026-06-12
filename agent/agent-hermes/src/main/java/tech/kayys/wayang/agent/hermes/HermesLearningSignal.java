package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalized run outcome used to decide whether Hermes should persist a skill.
 */
public record HermesLearningSignal(
        String requestId,
        String task,
        String answer,
        boolean successful,
        List<AgentState.ReasoningStep> steps,
        List<String> toolIds,
        Map<String, Object> metadata,
        Instant observedAt) {

    public HermesLearningSignal {
        task = HermesText.trimToEmpty(task);
        answer = HermesText.trimToEmpty(answer);
        steps = steps == null ? List.of() : List.copyOf(steps);
        toolIds = toolIds == null ? List.of() : List.copyOf(toolIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList());
        metadata = HermesMetadata.copy(metadata);
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }

    public static HermesLearningSignal from(AgentRequest request, AgentResponse response) {
        return new HermesLearningSignalFactory().from(request, response);
    }

    public boolean shouldLearn(HermesAgentModeConfig config) {
        return new HermesLearningPolicy(config).assess(this).eligible();
    }

}
