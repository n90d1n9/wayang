package tech.kayys.wayang.agent.spi;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Backend-agnostic inference request.
 */
public record InferenceRequest(
        String requestId,
        String model,
        List<InferenceTypes.ChatMessage> messages,
        List<InferenceTypes.ToolDefinition> tools,
        String toolChoice,
        Double temperature,
        Integer maxTokens,
        Double topP,
        List<String> stopSequences,
        boolean stream,
        Duration timeout,
        Map<String, Object> metadata) {

    public InferenceRequest {
        messages = messages != null ? List.copyOf(messages) : List.of();
        tools = tools != null ? List.copyOf(tools) : List.of();
        stopSequences = stopSequences != null ? List.copyOf(stopSequences) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        temperature = temperature != null ? temperature : 0.7;
        maxTokens = maxTokens != null ? maxTokens : 2048;
        topP = topP != null ? topP : 1.0;
        toolChoice = toolChoice != null ? toolChoice : "auto";
        timeout = timeout != null ? timeout : Duration.ofSeconds(30);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String requestId;
        private String model;
        private List<InferenceTypes.ChatMessage> messages;
        private List<InferenceTypes.ToolDefinition> tools;
        private String toolChoice;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private List<String> stopSequences;
        private boolean stream;
        private Duration timeout;
        private Map<String, Object> metadata;

        public Builder requestId(String v) { this.requestId = v; return this; }
        public Builder model(String v) { this.model = v; return this; }
        public Builder messages(List<InferenceTypes.ChatMessage> v) { this.messages = v; return this; }
        public Builder message(InferenceTypes.ChatMessage v) {
            if (this.messages == null) this.messages = new java.util.ArrayList<>();
            this.messages.add(v); return this;
        }
        public Builder tools(List<InferenceTypes.ToolDefinition> v) { this.tools = v; return this; }
        public Builder tool(InferenceTypes.ToolDefinition v) {
            if (this.tools == null) this.tools = new java.util.ArrayList<>();
            this.tools.add(v); return this;
        }
        public Builder toolChoice(String v) { this.toolChoice = v; return this; }
        public Builder temperature(Double v) { this.temperature = v; return this; }
        public Builder maxTokens(Integer v) { this.maxTokens = v; return this; }
        public Builder topP(Double v) { this.topP = v; return this; }
        public Builder stopSequences(List<String> v) { this.stopSequences = v; return this; }
        public Builder stream(boolean v) { this.stream = v; return this; }
        public Builder timeout(Duration v) { this.timeout = v; return this; }
        public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }

        public InferenceRequest build() {
            return new InferenceRequest(requestId, model, messages, tools, toolChoice,
                temperature, maxTokens, topP, stopSequences, stream, timeout, metadata);
        }
    }
}
