package tech.kayys.wayang.agent.core.inference;

import tech.kayys.wayang.agent.spi.InferenceTypes.ToolCall;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Agent-facing inference response with token and tool-loop metadata.
 */
public class AgentInferenceResponse {

    private String content;
    private String error;
    private String providerUsed;
    private String modelUsed;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private Duration latency;
    private boolean cached;
    private List<ToolCall> toolCalls;
    private List<ToolExecutionResult> toolResults;
    private String finishReason;
    private int iterations;

    public String getContent() {
        return content;
    }

    public String getError() {
        return error;
    }

    public boolean isError() {
        return error != null && !error.isBlank();
    }

    public String getProviderUsed() {
        return providerUsed;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public Duration getLatency() {
        return latency;
    }

    public boolean isCached() {
        return cached;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public List<ToolExecutionResult> getToolResults() {
        return toolResults;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public int getIterations() {
        return iterations;
    }

    public void setToolResults(List<ToolExecutionResult> toolResults) {
        this.toolResults = toolResults != null ? List.copyOf(toolResults) : List.of();
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final AgentInferenceResponse response = new AgentInferenceResponse();

        public Builder content(String value) {
            response.content = value;
            return this;
        }

        public Builder error(String value) {
            response.error = value;
            return this;
        }

        public Builder providerUsed(String value) {
            response.providerUsed = value;
            return this;
        }

        public Builder modelUsed(String value) {
            response.modelUsed = value;
            return this;
        }

        public Builder promptTokens(int value) {
            response.promptTokens = value;
            return this;
        }

        public Builder completionTokens(int value) {
            response.completionTokens = value;
            return this;
        }

        public Builder totalTokens(int value) {
            response.totalTokens = value;
            return this;
        }

        public Builder latency(Duration value) {
            response.latency = value;
            return this;
        }

        public Builder cached(boolean value) {
            response.cached = value;
            return this;
        }

        public Builder toolCalls(List<ToolCall> value) {
            response.toolCalls = value != null ? List.copyOf(value) : List.of();
            return this;
        }

        public Builder toolResults(List<ToolExecutionResult> value) {
            response.toolResults = value != null ? List.copyOf(value) : List.of();
            return this;
        }

        public Builder finishReason(String value) {
            response.finishReason = value;
            return this;
        }

        public Builder iterations(int value) {
            response.iterations = value;
            return this;
        }

        public AgentInferenceResponse build() {
            if (response.toolCalls == null) {
                response.toolCalls = List.of();
            }
            if (response.toolResults == null) {
                response.toolResults = List.of();
            }
            if (response.latency == null) {
                response.latency = Duration.ZERO;
            }
            return response;
        }
    }

    public record ToolExecutionResult(
            String toolName,
            Map<String, Object> arguments,
            Map<String, Object> result,
            boolean success,
            String error,
            long latencyMs) {
    }
}
