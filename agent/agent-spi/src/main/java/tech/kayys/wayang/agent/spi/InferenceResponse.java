package tech.kayys.wayang.agent.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Backend-agnostic inference response.
 */
public record InferenceResponse(
        String responseId,
        String requestId,
        String model,
        InferenceTypes.AssistantMessage message,
        String finishReason,
        InferenceTypes.TokenUsage usage,
        long durationMs,
        Instant completedAt) {

    public InferenceResponse {
        completedAt = completedAt != null ? completedAt : Instant.now();
        finishReason = finishReason != null ? finishReason : "stop";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String responseId;
        private String requestId;
        private String model;
        private InferenceTypes.AssistantMessage message;
        private String finishReason;
        private InferenceTypes.TokenUsage usage;
        private long durationMs;

        public Builder responseId(String v) { this.responseId = v; return this; }
        public Builder requestId(String v) { this.requestId = v; return this; }
        public Builder model(String v) { this.model = v; return this; }
        public Builder message(InferenceTypes.AssistantMessage v) { this.message = v; return this; }
        public Builder content(String v) {
            this.message = new InferenceTypes.AssistantMessage(v, List.of()); return this;
        }
        public Builder toolCalls(List<InferenceTypes.ToolCall> v) {
            this.message = new InferenceTypes.AssistantMessage("", v); return this;
        }
        public Builder finishReason(String v) { this.finishReason = v; return this; }
        public Builder usage(InferenceTypes.TokenUsage v) { this.usage = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }

        public InferenceResponse build() {
            return new InferenceResponse(responseId, requestId, model, message,
                finishReason, usage, durationMs, Instant.now());
        }
    }
}
