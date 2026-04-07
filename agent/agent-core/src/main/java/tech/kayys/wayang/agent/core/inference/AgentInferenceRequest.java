package tech.kayys.wayang.agent.core.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.tool.ToolDefinition;

/**
 * Agent-specific inference request wrapper.
 *
 * <p>
 * Supports:
 * <ul>
 * <li>Single-shot inference (systemPrompt + userPrompt)</li>
 * <li>Multi-turn conversation (conversationHistory)</li>
 * <li>Tool definitions for LLM function calling</li>
 * <li>ReAct loop configuration (maxToolIterations)</li>
 * </ul>
 */
public class AgentInferenceRequest {
    private String systemPrompt;
    private String userPrompt;
    private String preferredProvider;
    private Double temperature = 0.7;
    private Integer maxTokens = 2048;
    private String model;
    private Map<String, Object> additionalParams = new HashMap<>();
    private String agentId;
    private Boolean useMemory = false;
    private Boolean stream = false;
    private List<ToolDefinition> tools = new ArrayList<>();

    /**
     * Conversation history for multi-turn interactions.
     * Messages should be ordered chronologically (oldest first).
     * If provided, these messages are inserted between the system prompt
     * and the current user prompt.
     */
    private List<Message> conversationHistory = new ArrayList<>();

    /**
     * Maximum number of tool-call iterations in a ReAct loop.
     * Set to 0 to disable tool calling. Default is 10.
     */
    private int maxToolIterations = 10;

    public AgentInferenceRequest() {
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getPreferredProvider() {
        return preferredProvider;
    }

    public void setPreferredProvider(String preferredProvider) {
        this.preferredProvider = preferredProvider;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Boolean getUseMemory() {
        return useMemory;
    }

    public void setUseMemory(Boolean useMemory) {
        this.useMemory = useMemory;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }

    public List<Message> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<Message> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public int getMaxToolIterations() {
        return maxToolIterations;
    }

    public void setMaxToolIterations(int maxToolIterations) {
        this.maxToolIterations = maxToolIterations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentInferenceRequest request = new AgentInferenceRequest();

        public Builder systemPrompt(String systemPrompt) {
            request.setSystemPrompt(systemPrompt);
            return this;
        }

        public Builder userPrompt(String userPrompt) {
            request.setUserPrompt(userPrompt);
            return this;
        }

        public Builder preferredProvider(String preferredProvider) {
            request.setPreferredProvider(preferredProvider);
            return this;
        }

        public Builder temperature(Double temperature) {
            request.setTemperature(temperature);
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            request.setMaxTokens(maxTokens);
            return this;
        }

        public Builder model(String model) {
            request.setModel(model);
            return this;
        }

        public Builder additionalParams(Map<String, Object> additionalParams) {
            request.setAdditionalParams(additionalParams);
            return this;
        }

        public Builder agentId(String agentId) {
            request.setAgentId(agentId);
            return this;
        }

        public Builder useMemory(Boolean useMemory) {
            request.setUseMemory(useMemory);
            return this;
        }

        public Builder stream(Boolean stream) {
            request.setStream(stream);
            return this;
        }

        public Builder tools(List<ToolDefinition> tools) {
            request.setTools(tools);
            return this;
        }

        public Builder conversationHistory(List<Message> conversationHistory) {
            request.setConversationHistory(conversationHistory);
            return this;
        }

        public Builder maxToolIterations(int maxToolIterations) {
            request.setMaxToolIterations(maxToolIterations);
            return this;
        }

        public AgentInferenceRequest build() {
            return request;
        }
    }
}
