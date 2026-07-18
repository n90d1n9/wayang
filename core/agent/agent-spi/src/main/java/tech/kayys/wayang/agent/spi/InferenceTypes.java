package tech.kayys.wayang.agent.spi;

import java.util.List;
import java.util.Map;

/**
 * Container for inference-related value types.
 * All types are static nested classes to avoid multiple public types per file.
 */
public final class InferenceTypes {

    private InferenceTypes() {}

    // ── Chat Messages ─────────────────────────────────────────────────────────

    public sealed interface ChatMessage permits SystemMessage, UserMessage, AssistantMessage, ToolResultMessage {
        String role();
        String content();
    }

    public record SystemMessage(String content) implements ChatMessage {
        @Override public String role() { return "system"; }
    }

    public record UserMessage(String content, List<ContentPart> parts) implements ChatMessage {
        public UserMessage(String content) { this(content, List.of()); }
        @Override public String role() { return "user"; }
    }

    public record AssistantMessage(String content, List<ToolCall> toolCalls) implements ChatMessage {
        public AssistantMessage(String content) { this(content, List.of()); }
        @Override public String role() { return "assistant"; }
    }

    public record ToolResultMessage(String toolCallId, String toolName, String content) implements ChatMessage {
        @Override public String role() { return "tool"; }
    }

    // ── Content Parts (Multimodal) ────────────────────────────────────────────

    public sealed interface ContentPart permits TextPart, ImagePart {
        String type();
    }

    public record TextPart(String text) implements ContentPart {
        @Override public String type() { return "text"; }
    }

    public record ImagePart(String imageUrl, String mimeType) implements ContentPart {
        @Override public String type() { return "image_url"; }
    }

    // ── Tool Definitions ──────────────────────────────────────────────────────

    public record ToolDefinition(String name, String description, Map<String, Object> parameters) {}

    // ── Tool Calls ────────────────────────────────────────────────────────────

    public record ToolCall(String id, String type, String name, String arguments) {}

    // ── Token Usage ───────────────────────────────────────────────────────────

    public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        public static TokenUsage of(int prompt, int completion) {
            return new TokenUsage(prompt, completion, prompt + completion);
        }
    }

    // ── Streaming Chunk ───────────────────────────────────────────────────────

    public record StreamingChunk(
            String responseId,
            String delta,
            List<ToolCall> toolCalls,
            String finishReason,
            TokenUsage usage) {
        public boolean isLast() {
            return finishReason != null && !finishReason.isBlank();
        }
    }

    // ── Provider Info ─────────────────────────────────────────────────────────

    public record ProviderInfo(String id, String name, String model, boolean isHealthy, Map<String, Object> metadata) {}
}
