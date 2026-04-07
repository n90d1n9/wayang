package tech.kayys.wayang.agent.core.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.client.GollekAgentClient;
import tech.kayys.wayang.agent.registry.AgentProviderRegistry;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;
import tech.kayys.gollek.spi.tool.ToolDefinition;

import java.time.Duration;
import java.util.*;

/**
 * Specialized inference service for agent workloads.
 *
 * <p>This service provides agent-optimized inference operations with:
 * <ul>
 *   <li>Reasoning-optimized default parameters</li>
 *   <li>Built-in system prompt templates for different orchestrators</li>
 *   <li>Tool calling with automatic schema generation</li>
 *   <li>Context management and compression</li>
 *   <li>Multi-turn conversation support</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Smart defaults:</b> Temperature, max_tokens, and other parameters tuned for agent reasoning</li>
 *   <li><b>System prompts:</b> Pre-built system prompts for ReAct, Plan-and-Execute, Reflexion strategies</li>
 *   <li><b>Tool integration:</b> Automatic conversion of agent skills to tool definitions</li>
 *   <li><b>Context compression:</b> Automatic summarization of long conversation histories</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class AgenticInferenceService {

    private static final Logger LOG = Logger.getLogger(AgenticInferenceService.class);

    @Inject
    GollekAgentClient agentClient;

    @Inject
    AgentProviderRegistry providerRegistry;

    // Default parameters for agent reasoning
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 1024;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    // System prompt templates for different orchestration strategies
    private static final String REACT_SYSTEM_PROMPT = """
        You are an intelligent agent that solves problems step-by-step using available tools.

        Use this format EXACTLY:

        Thought: [Your reasoning about what to do next]
        Action: [exact_tool_id]
        Action Input: {"key": "value"}
        Observation: [result will be filled in]

        Repeat Thought/Action/Action Input until you have enough information, then:

        Final Answer: [Your complete answer to the original question]

        Rules:
        - Always start with a Thought
        - Action must be exactly one of the available tool IDs
        - Action Input must be valid JSON
        - Never make up observations — wait for actual results
        - When you have enough information, use Final Answer
        """;

    private static final String PLAN_AND_EXECUTE_SYSTEM_PROMPT = """
        You are a strategic planning agent. Break down complex tasks into clear, executable steps.

        Your output should follow this structure:
        1. **Plan**: List the steps needed to complete the task
        2. **Execute**: Carry out each step systematically
        3. **Synthesize**: Combine results into a final answer

        Be methodical and thorough. Ensure each step is specific and actionable.
        """;

    private static final String REFLEXION_SYSTEM_PROMPT = """
        You are a self-improving reasoning assistant. You produce answers,
        evaluate them critically, reflect on improvements, and revise until
        the answer meets quality criteria. Be direct and rigorous in evaluation.
        """;

    /**
     * Execute a simple inference request optimized for agent reasoning.
     *
     * @param prompt the user prompt
     * @param modelId the model ID to use
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> infer(String prompt, String modelId) {
        InferenceRequest request = createBaseRequest(prompt, modelId)
            .build();
        
        return agentClient.infer(request);
    }

    /**
     * Execute inference with system prompt for a specific orchestration strategy.
     *
     * @param prompt the user prompt
     * @param modelId the model ID to use
     * @param strategy the orchestration strategy (react, plan-and-execute, reflexion)
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> inferWithStrategy(
            String prompt,
            String modelId,
            String strategy) {
        
        String systemPrompt = getSystemPromptForStrategy(strategy);
        
        InferenceRequest request = createBaseRequest(prompt, modelId)
            .message(0, Message.system(systemPrompt))
            .build();
        
        return agentClient.infer(request);
    }

    /**
     * Execute inference with tool calling support.
     *
     * @param prompt the user prompt
     * @param modelId the model ID to use
     * @param tools list of tool definitions
     * @return Uni containing the inference response (may include tool calls)
     */
    public Uni<InferenceResponse> inferWithTools(
            String prompt,
            String modelId,
            List<ToolDefinition> tools) {
        
        InferenceRequest request = createBaseRequest(prompt, modelId)
            .tools(tools)
            .toolChoice("auto")
            .build();
        
        return agentClient.inferWithTools(request, tools);
    }

    /**
     * Execute streaming inference with real-time token generation.
     *
     * @param prompt the user prompt
     * @param modelId the model ID to use
     * @return Multi emitting streaming chunks
     */
    public Multi<StreamingInferenceChunk> stream(String prompt, String modelId) {
        InferenceRequest request = createBaseRequest(prompt, modelId)
            .streaming(true)
            .build();
        
        return agentClient.stream(request);
    }

    /**
     * Execute inference with conversation history.
     *
     * @param messages list of conversation messages
     * @param modelId the model ID to use
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> inferWithHistory(
            List<Message> messages,
            String modelId) {
        
        InferenceRequest request = InferenceRequest.builder()
            .model(modelId)
            .messages(messages)
            .temperature(DEFAULT_TEMPERATURE)
            .maxTokens(DEFAULT_MAX_TOKENS)
            .timeout(DEFAULT_TIMEOUT)
            .build();
        
        return agentClient.infer(request);
    }

    /**
     * Execute inference with retry and timeout.
     *
     * @param prompt the user prompt
     * @param modelId the model ID to use
     * @param maxRetries maximum number of retry attempts
     * @param timeout timeout for the operation
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> inferWithRetry(
            String prompt,
            String modelId,
            int maxRetries,
            Duration timeout) {
        
        InferenceRequest request = createBaseRequest(prompt, modelId)
            .build();
        
        return agentClient.inferWithRetry(request, maxRetries, timeout);
    }

    /**
     * Select best provider for agent workloads and execute inference.
     *
     * @param prompt the user prompt
     * @param requireToolCalling whether tool calling is required
     * @param preferLocal whether to prefer local providers
     * @return Uni containing the inference response
     */
    public Uni<InferenceResponse> inferWithProviderSelection(
            String prompt,
            boolean requireToolCalling,
            boolean preferLocal) {
        
        String providerId = providerRegistry.selectBestProvider(
            requireToolCalling, false, preferLocal);
        
        if (providerId == null) {
            return Uni.createFrom().failure(
                new IllegalStateException("No suitable provider available")
            );
        }
        
        InferenceRequest request = createBaseRequest(prompt, "default")
            .preferredProvider(providerId)
            .build();
        
        return agentClient.infer(request);
    }

    /**
     * Compress conversation history by summarizing old messages.
     *
     * @param messages list of conversation messages
     * @param maxMessages maximum number of messages to keep
     * @return Uni containing compressed message list
     */
    public Uni<List<Message>> compressHistory(List<Message> messages, int maxMessages) {
        if (messages.size() <= maxMessages) {
            return Uni.createFrom().item(messages);
        }

        // Keep the system message (if present) and the most recent messages
        List<Message> compressed = new ArrayList<>();
        
        // Keep system message first
        if (!messages.isEmpty() && messages.get(0).getRole() == Message.Role.SYSTEM) {
            compressed.add(messages.get(0));
        }
        
        // Calculate how many recent messages to keep
        int messagesToKeep = maxMessages - compressed.size();
        int startIndex = Math.max(1, messages.size() - messagesToKeep);
        
        // Add summary of old messages if there are any
        if (startIndex > 1) {
            String summary = "[Previous conversation summary: " + 
                (startIndex - 1) + " messages omitted]";
            compressed.add(Message.system(summary));
        }
        
        // Add recent messages
        compressed.addAll(messages.subList(startIndex, messages.size()));
        
        LOG.debugf("Compressed history from %d to %d messages", 
            messages.size(), compressed.size());
        
        return Uni.createFrom().item(compressed);
    }

    /**
     * Get system prompt for a specific orchestration strategy.
     *
     * @param strategy the strategy ID
     * @return system prompt string
     */
    public String getSystemPromptForStrategy(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "react" -> REACT_SYSTEM_PROMPT;
            case "plan-and-execute", "plan_execute" -> PLAN_AND_EXECUTE_SYSTEM_PROMPT;
            case "reflexion" -> REFLEXION_SYSTEM_PROMPT;
            default -> "You are a helpful AI assistant.";
        };
    }

    /**
     * Create a tool definition from a skill descriptor.
     *
     * @param skillId the skill ID
     * @param description the skill description
     * @param parameters the skill input parameters schema
     * @return tool definition
     */
    public ToolDefinition createToolDefinition(
            String skillId,
            String description,
            Map<String, Object> parameters) {
        
        return ToolDefinition.builder()
            .name(skillId)
            .type(ToolDefinition.Type.FUNCTION)
            .description(description)
            .parameters(parameters)
            .strict(false)
            .build();
    }

    /**
     * Create tool definitions from a list of skills.
     *
     * @param skills map of skill ID to (description, parameters)
     * @return list of tool definitions
     */
    public List<ToolDefinition> createToolDefinitions(Map<String, SkillDescriptor> skills) {
        return skills.entrySet().stream()
            .map(entry -> createToolDefinition(
                entry.getKey(),
                entry.getValue().description(),
                entry.getValue().parameters()
            ))
            .toList();
    }

    /**
     * Create a base inference request builder with agent-optimized defaults.
     */
    private InferenceRequest.Builder createBaseRequest(String prompt, String modelId) {
        return InferenceRequest.builder()
            .model(modelId)
            .message(Message.user(prompt))
            .temperature(DEFAULT_TEMPERATURE)
            .maxTokens(DEFAULT_MAX_TOKENS)
            .timeout(DEFAULT_TIMEOUT);
    }

    /**
     * Skill descriptor for tool definition creation.
     */
    public record SkillDescriptor(String description, Map<String, Object> parameters) {}
}
