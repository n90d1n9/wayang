package tech.kayys.golok.tools.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Map;

/**
 * The result of executing a tool call — sent back to the model as a
 * {@code tool} role message in the next inference turn.
 */
public record ToolCallResult(
        /** The {@code id} from the original {@link ToolCall}. */
        String toolCallId,
        /** Skill/function name (informational). */
        String toolName,
        /** Serialised result content sent back to the model. */
        String content,
        /** Whether the tool succeeded. */
        boolean success,
        /**
         * Raw structured output from the skill (not sent to model, used internally).
         */
        Map<String, Object> rawOutputs,
        Instant completedAt) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ToolCallResult {
        completedAt = completedAt != null ? completedAt : Instant.now();
        rawOutputs = rawOutputs != null ? Map.copyOf(rawOutputs) : Map.of();
        content = content != null ? content : (success ? "OK" : "Error");
    }

    public static ToolCallResult ok(String toolCallId, String toolName, String content, Map<String, Object> outputs) {
        return new ToolCallResult(toolCallId, toolName, content, true, outputs, Instant.now());
    }

    public static ToolCallResult error(String toolCallId, String toolName, String errorMessage) {
        String content = "Error executing tool '" + toolName + "': " + errorMessage;
        return new ToolCallResult(toolCallId, toolName, content, false, Map.of(), Instant.now());
    }

    public ObjectNode toOpenAIMessage() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", "tool");
        node.put("tool_call_id", toolCallId);
        node.put("content", content);
        return node;
    }

    public ObjectNode toAnthropicBlock() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "tool_result");
        node.put("tool_use_id", toolCallId);
        node.put("content", content);
        if (!success)
            node.put("is_error", true);
        return node;
    }

    public String toObservationText() {
        return success
                ? "Observation: " + content
                : "Observation: [Error] " + content;
    }
}
