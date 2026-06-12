package tech.kayys.gamelan.agent.agui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.Map;

/**
 * AG-UI protocol event — typed SSE event conforming to
 * <a href="https://docs.ag-ui.com/concepts/events">AG-UI event spec</a>.
 *
 * <h2>Standard event types</h2>
 * <table>
 *   <tr><th>Type</th><th>When emitted</th></tr>
 *   <tr><td>RUN_STARTED</td><td>agent loop begins</td></tr>
 *   <tr><td>RUN_FINISHED</td><td>agent loop completes (success or error)</td></tr>
 *   <tr><td>TEXT_MESSAGE_START</td><td>LLM starts streaming a message</td></tr>
 *   <tr><td>TEXT_MESSAGE_CONTENT</td><td>LLM token delta</td></tr>
 *   <tr><td>TEXT_MESSAGE_END</td><td>LLM message complete</td></tr>
 *   <tr><td>TOOL_CALL_START</td><td>a tool call begins executing</td></tr>
 *   <tr><td>TOOL_CALL_END</td><td>tool call result available</td></tr>
 *   <tr><td>STATE_SNAPSHOT</td><td>full agent state for client sync</td></tr>
 *   <tr><td>STATE_DELTA</td><td>JSON-patch update to agent state</td></tr>
 *   <tr><td>ERROR</td><td>unrecoverable error occurred</td></tr>
 *   <tr><td>STEP_STARTED</td><td>a reasoning step begins</td></tr>
 *   <tr><td>STEP_FINISHED</td><td>a reasoning step ends</td></tr>
 * </table>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AguiEvent(
        String              type,
        String              runId,
        String              messageId,
        String              toolCallId,
        String              toolName,
        String              delta,
        Object              result,
        Map<String, Object> state,
        String              error,
        int                 step,
        long                timestamp
) {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── Factory methods ────────────────────────────────────────────────────

    public static AguiEvent runStarted(String runId) {
        return builder("RUN_STARTED").runId(runId).build();
    }

    public static AguiEvent runFinished(String runId, boolean success, String error) {
        return builder("RUN_FINISHED").runId(runId).error(success ? null : error).build();
    }

    public static AguiEvent textStart(String runId, String messageId) {
        return builder("TEXT_MESSAGE_START").runId(runId).messageId(messageId).build();
    }

    public static AguiEvent textDelta(String runId, String messageId, String delta) {
        return builder("TEXT_MESSAGE_CONTENT").runId(runId).messageId(messageId).delta(delta).build();
    }

    public static AguiEvent textEnd(String runId, String messageId) {
        return builder("TEXT_MESSAGE_END").runId(runId).messageId(messageId).build();
    }

    public static AguiEvent toolCallStart(String runId, String toolCallId, String toolName) {
        return builder("TOOL_CALL_START").runId(runId)
                .toolCallId(toolCallId).toolName(toolName).build();
    }

    public static AguiEvent toolCallEnd(String runId, String toolCallId,
                                         String toolName, Object result) {
        return builder("TOOL_CALL_END").runId(runId)
                .toolCallId(toolCallId).toolName(toolName).result(result).build();
    }

    public static AguiEvent stepStarted(String runId, int step) {
        return builder("STEP_STARTED").runId(runId).step(step).build();
    }

    public static AguiEvent stepFinished(String runId, int step) {
        return builder("STEP_FINISHED").runId(runId).step(step).build();
    }

    public static AguiEvent stateSnapshot(String runId, Map<String, Object> state) {
        return builder("STATE_SNAPSHOT").runId(runId).state(state).build();
    }

    public static AguiEvent error(String runId, String message) {
        return builder("ERROR").runId(runId).error(message).build();
    }

    // ── SSE serialisation ──────────────────────────────────────────────────

    /**
     * Formats this event as an SSE frame as specified by AG-UI:
     * <pre>
     * data: {"type":"TEXT_MESSAGE_CONTENT","runId":"...","delta":"hello"}
     *
     * </pre>
     */
    public String toSseFrame() {
        try {
            return "data: " + MAPPER.writeValueAsString(this) + "\n\n";
        } catch (JsonProcessingException e) {
            return "data: {\"type\":\"ERROR\",\"error\":\"serialization failed\"}\n\n";
        }
    }

    /** Serialise to JSON (for WebSocket / HTTP body). */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"ERROR\",\"error\":\"serialization failed\"}";
        }
    }

    // ── Builder ────────────────────────────────────────────────────────────

    public static Builder builder(String type) { return new Builder(type); }

    public static final class Builder {
        private final String type;
        private String              runId;
        private String              messageId;
        private String              toolCallId;
        private String              toolName;
        private String              delta;
        private Object              result;
        private Map<String, Object> state;
        private String              error;
        private int                 step;

        Builder(String type) { this.type = type; }

        public Builder runId(String v)      { this.runId = v;      return this; }
        public Builder messageId(String v)  { this.messageId = v;  return this; }
        public Builder toolCallId(String v) { this.toolCallId = v; return this; }
        public Builder toolName(String v)   { this.toolName = v;   return this; }
        public Builder delta(String v)      { this.delta = v;      return this; }
        public Builder result(Object v)     { this.result = v;     return this; }
        public Builder state(Map<String, Object> v) { this.state = v; return this; }
        public Builder error(String v)      { this.error = v;      return this; }
        public Builder step(int v)          { this.step = v;       return this; }

        public AguiEvent build() {
            return new AguiEvent(type, runId, messageId, toolCallId, toolName,
                    delta, result, state, error, step,
                    Instant.now().toEpochMilli());
        }
    }
}
