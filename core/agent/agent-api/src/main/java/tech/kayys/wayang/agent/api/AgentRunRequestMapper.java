package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Maps transport-level run requests into backend-agnostic agent requests.
 */
final class AgentRunRequestMapper {

    AgentRequest toAgentRequest(AgentResource.AgentRunRequest request, boolean stream) {
        AgentResource.AgentRunRequest safeRequest = safeRequest(request);
        if (safeRequest.prompt() == null || safeRequest.prompt().isBlank()) {
            throw new IllegalArgumentException("Prompt is required");
        }

        AgentRequest.Builder builder = AgentRequest.builder()
                .prompt(safeRequest.prompt())
                .systemPrompt(safeRequest.systemPrompt())
                .strategy(parseStrategy(safeRequest.strategy()))
                .tenantId(defaultText(safeRequest.tenantId(), "default"))
                .userId(safeRequest.userId())
                .sessionId(safeRequest.sessionId())
                .modelId(safeRequest.modelId())
                .stream(stream)
                .maxSteps(safeRequest.maxSteps() > 0 ? safeRequest.maxSteps() : 15);

        if (safeRequest.skills() != null) {
            safeRequest.skills().stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .forEach(builder::skill);
        }
        if (safeRequest.context() != null) {
            builder.context(safeRequest.context());
        }
        if (safeRequest.timeout() != null && !safeRequest.timeout().isBlank()) {
            builder.timeout(parseDuration(safeRequest.timeout()));
        }
        return builder.build();
    }

    private AgentResource.AgentRunRequest safeRequest(AgentResource.AgentRunRequest request) {
        return request == null
                ? new AgentResource.AgentRunRequest(
                        null,
                        null,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)
                : request;
    }

    private Duration parseDuration(String timeout) {
        try {
            return Duration.parse(timeout);
        } catch (DateTimeParseException ignored) {
            return Duration.ofSeconds(Long.parseLong(timeout));
        }
    }

    private OrchestrationStrategy parseStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return OrchestrationStrategy.REACT;
        }
        for (OrchestrationStrategy candidate : OrchestrationStrategy.values()) {
            if (candidate.id.equalsIgnoreCase(strategy) || candidate.name().equalsIgnoreCase(strategy)) {
                return candidate;
            }
        }
        return OrchestrationStrategy.CUSTOM;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
