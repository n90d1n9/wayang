package tech.kayys.wayang.agent.spi.inference;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import tech.kayys.gollek.spi.inference.InferenceResponse;

/**
 * Agent-specific inference response wrapper.
 *
 * <p>Extends the basic response with agent-specific fields:
 * <ul>
 * <li>{@code toolCalls} — tool invocations requested by the LLM</li>
 * <li>{@code toolResults} — results from executed tools (for audit/debugging)</li>
 * <li>{@code finishReason} — why the LLM stopped generating</li>
 * <li>{@code iterations} — number of ReAct loop iterations used</li>
 * </ul>
 */
public class AgentInferenceResponse {
    private String content;
    private String providerUsed;
    private String modelUsed;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Duration latency;
    private Boolean cached = false;
    private String error;
    private Map<String, Object> metadata;

    /**
     * Tool calls requested by the LLM in the final response.
     * Empty if the LLM didn't invoke any tools or all tool calls were resolved.
     */
    private List<InferenceResponse.ToolCall> toolCalls;

    /**
     * Results from tool executions during the ReAct loop.
     * Each entry maps tool name → result for audit/debugging purposes.
     */
    private List<ToolExecutionResult> toolResults;

    /**
     * Why the LLM stopped generating. Maps to the SDK's FinishReason.
     */
    private String finishReason;

    /**
     * Number of ReAct loop iterations used (0 = no tools were called).
     */
    private int iterations;

    public AgentInferenceResponse() {
    }

    // ==================== Tool Execution Result ====================

    /**
     * Record of a single tool execution during the ReAct loop.
     */
    public record ToolExecutionResult(
            String toolName,
            Map<String, Object> arguments,
            Object result,
            boolean success,
            String error,
            long durationMs) {
    }

    // ==================== Getters & Setters ====================

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProviderUsed() {
        return providerUsed;
    }

    public void setProviderUsed(String providerUsed) {
        this.providerUsed = providerUsed;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Duration getLatency() {
        return latency;
    }

    public void setLatency(Duration latency) {
        this.latency = latency;
    }

    public Boolean getCached() {
        return cached;
    }

    public void setCached(Boolean cached) {
        this.cached = cached;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<InferenceResponse.ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<InferenceResponse.ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<ToolExecutionResult> getToolResults() {
        return toolResults;
    }

    public void setToolResults(List<ToolExecutionResult> toolResults) {
        this.toolResults = toolResults;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentInferenceResponse response = new AgentInferenceResponse();

        public Builder content(String content) {
            response.setContent(content);
            return this;
        }

        public Builder providerUsed(String providerUsed) {
            response.setProviderUsed(providerUsed);
            return this;
        }

        public Builder modelUsed(String modelUsed) {
            response.setModelUsed(modelUsed);
            return this;
        }

        public Builder promptTokens(Integer promptTokens) {
            response.setPromptTokens(promptTokens);
            return this;
        }

        public Builder completionTokens(Integer completionTokens) {
            response.setCompletionTokens(completionTokens);
            return this;
        }

        public Builder totalTokens(Integer totalTokens) {
            response.setTotalTokens(totalTokens);
            return this;
        }

        public Builder latency(Duration latency) {
            response.setLatency(latency);
            return this;
        }

        public Builder cached(Boolean cached) {
            response.setCached(cached);
            return this;
        }

        public Builder error(String error) {
            response.setError(error);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            response.setMetadata(metadata);
            return this;
        }

        public Builder toolCalls(List<InferenceResponse.ToolCall> toolCalls) {
            response.setToolCalls(toolCalls);
            return this;
        }

        public Builder toolResults(List<ToolExecutionResult> toolResults) {
            response.setToolResults(toolResults);
            return this;
        }

        public Builder finishReason(String finishReason) {
            response.setFinishReason(finishReason);
            return this;
        }

        public Builder iterations(int iterations) {
            response.setIterations(iterations);
            return this;
        }

        public AgentInferenceResponse build() {
            return response;
        }
    }
}
