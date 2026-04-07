package tech.kayys.wayang.agent.spi;

import java.time.Instant;
import java.util.Map;

/**
 * Event emitted during agent execution.
 *
 * @param runId     Run identifier
 * @param timestamp Event timestamp
 * @param type      Event type (STARTED, THOUGHT, ACTION, OBSERVATION, FINAL_ANSWER, COMPLETE, ERROR, LOG)
 * @param metadata  Additional event data
 */
public record AgentEvent(
        String runId,
        Instant timestamp,
        String type,
        Map<String, Object> metadata) {

    public AgentEvent {
        timestamp = timestamp != null ? timestamp : Instant.now();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static AgentEvent started(String runId, String prompt) {
        return new AgentEvent(runId, Instant.now(), "STARTED", Map.of("prompt", prompt));
    }

    public static AgentEvent runStart(String runId, String strategy) {
        return new AgentEvent(runId, Instant.now(), "STARTED", Map.of("strategy", strategy));
    }

    public static AgentEvent thought(String runId, String thought) {
        return new AgentEvent(runId, Instant.now(), "THOUGHT", Map.of("content", thought));
    }

    public static AgentEvent action(String runId, Object action) {
        return new AgentEvent(runId, Instant.now(), "ACTION", Map.of("action", action.toString()));
    }

    public static AgentEvent observation(String runId, String obs) {
        return new AgentEvent(runId, Instant.now(), "OBSERVATION", Map.of("content", obs));
    }

    public static AgentEvent finalAnswer(String runId, String answer) {
        return new AgentEvent(runId, Instant.now(), "FINAL_ANSWER", Map.of("content", answer));
    }

    public static AgentEvent finalAnswer(String runId, int totalSteps, String answer) {
        return new AgentEvent(runId, Instant.now(), "FINAL_ANSWER", Map.of("steps", totalSteps, "content", answer));
    }

    public static AgentEvent runComplete(String runId, int totalSteps, boolean successful, long durationMs) {
        return new AgentEvent(runId, Instant.now(), "COMPLETE", Map.of(
                "steps", totalSteps,
                "successful", successful,
                "durationMs", durationMs));
    }

    public static AgentEvent error(String runId, String error) {
        return new AgentEvent(runId, Instant.now(), "ERROR", Map.of("message", error));
    }

    public static AgentEvent log(String runId, int step, String message) {
        return new AgentEvent(runId, Instant.now(), "LOG", Map.of("step", step, "message", message));
    }
}
