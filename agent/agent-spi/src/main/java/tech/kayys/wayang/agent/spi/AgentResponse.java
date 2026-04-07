package tech.kayys.wayang.agent.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Immutable response from an agent execution.
 */
public record AgentResponse(
        String runId,
        String requestId,
        String answer,
        List<AgentState.ReasoningStep> steps,
        int totalSteps,
        boolean successful,
        String error,
        String strategy,
        long durationMs,
        Instant completedAt) {

    public AgentResponse {
        steps = steps != null ? List.copyOf(steps) : List.of();
        completedAt = completedAt != null ? completedAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String runId;
        private String requestId;
        private String answer;
        private List<AgentState.ReasoningStep> steps;
        private int totalSteps;
        private boolean successful = true;
        private String error;
        private String strategy;
        private long durationMs;

        public Builder runId(String v) { this.runId = v; return this; }
        public Builder requestId(String v) { this.requestId = v; return this; }
        public Builder answer(String v) { this.answer = v; return this; }
        public Builder steps(List<AgentState.ReasoningStep> v) { this.steps = v; return this; }
        public Builder totalSteps(int v) { this.totalSteps = v; return this; }
        public Builder successful(boolean v) { this.successful = v; return this; }
        public Builder error(String v) { this.error = v; return this; }
        public Builder strategy(String v) { this.strategy = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }

        public AgentResponse build() {
            return new AgentResponse(runId, requestId, answer, steps, totalSteps,
                    successful, error, strategy, durationMs, Instant.now());
        }
    }
}
