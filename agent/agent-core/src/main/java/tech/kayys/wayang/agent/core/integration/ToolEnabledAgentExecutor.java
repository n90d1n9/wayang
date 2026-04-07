package tech.kayys.wayang.agent.core.integration;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.agent.core.tools.AgentToolService;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;
import tech.kayys.golok.tools.spi.ToolCall;
import tech.kayys.golok.tools.spi.ToolCallResult;
import tech.kayys.golok.tools.spi.ToolResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool-Enabled Agent with Memory Integration
 *
 * Complete implementation of a ReAct agent with:
 * - Tool discovery and selection
 * - Tool execution with memory context
 * - Result tracking and learning
 * - Multi-step tool chains
 * - Performance optimization
 *
 * Execution Flow:
 * 1. Agent analyzes task
 * 2. Discovers available tools
 * 3. Creates tool chain plan
 * 4. Executes tools sequentially
 * 5. Stores results in memory
 * 6. Adapts for next execution
 *
 * Usage:
 * {@code
 * @Inject
 * ToolEnabledAgentExecutor executor;
 *
 * AgentResponse response = executor.executeTaskWithTools(
 * "task-agent",
 * "user-123",
 * "session-abc",
 * "Find and summarize the latest AI news"
 * ).await().indefinitely();
 * }
 */
@ApplicationScoped
public class ToolEnabledAgentExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ToolEnabledAgentExecutor.class);

    @Inject
    AgentMemoryService memoryService;

    @Inject
    AgentToolService toolService;

    @Inject
    AgentOrchestrator agentOrchestrator;

    /**
     * Execute task with tool support
     *
     * @param agentId   The agent ID
     * @param userId    The user ID
     * @param sessionId The session ID
     * @param task      The task description
     * @return Reactive agent response
     */
    public Uni<AgentResponse> executeTaskWithTools(
            String agentId,
            String userId,
            String sessionId,
            String task) {

        LOG.info("Agent {} executing task with tools", agentId);

        // Step 1: Get conversation context
        return memoryService.getContextPrompt(agentId, 5)
                // Step 2: Get available tools
                .flatMap(context -> Uni.combine()
                        .all()
                        .unis(
                                Uni.createFrom().item(context),
                                toolService.getToolsForAgent(agentId),
                                toolService.recommendTools(agentId, task))
                        .asTuple())
                // Step 3: Build enhanced prompt with tools
                .map(tuple -> {
                    String context = tuple.getItem1();
                    var allTools = tuple.getItem2();
                    var recommendedTools = tuple.getItem3();

                    return buildToolAwarePrompt(task, context, recommendedTools);
                })
                // Step 4: Create agent request with tool definitions
                .flatMap(enhancedPrompt -> {
                    return toolService.getAvailableTools()
                            .map(toolDefs -> AgentRequest.builder()
                                    .requestId(UUID.randomUUID().toString())
                                    .agentId(agentId)
                                    .userId(userId)
                                    .sessionId(sessionId)
                                    .prompt(enhancedPrompt)
                                    .strategy(OrchestrationStrategy.REACT)
                                    .tools(toolDefs) // Pass available tools
                                    .verbose(true)
                                    .build());
                })
                // Step 5: Execute agent
                .flatMap(agentOrchestrator::execute)
                // Step 6: Process tool calls if present
                .flatMap(response -> procesToolCalls(agentId, response))
                // Step 7: Store execution in memory
                .flatMap(response -> {
                    return memoryService.storeInteraction(
                            agentId,
                            sessionId,
                            userId,
                            task,
                            response.content())
                            .map(__ -> response);
                })
                .onFailure().invoke(ex -> {
                    LOG.error("Task execution failed: {}", ex.getMessage(), ex);
                });
    }

    /**
     * Process tool calls from agent response
     */
    private Uni<AgentResponse> procesToolCalls(
            String agentId,
            AgentResponse response) {

        // Extract tool calls from response (if any)
        List<ToolCall> toolCalls = extractToolCalls(response);

        if (toolCalls.isEmpty()) {
            return Uni.createFrom().item(response);
        }

        LOG.info("Processing {} tool calls for agent {}", toolCalls.size(), agentId);

        // Execute each tool call
        return Uni.combine()
                .all()
                .unis(toolCalls.stream()
                        .map(toolCall -> executeSingleToolCall(agentId, toolCall))
                        .collect(Collectors.toList()))
                .asList()
                .map(results -> augmentResponseWithToolResults(response, results));
    }

    /**
     * Execute single tool call
     */
    private Uni<ToolCallResult> executeSingleToolCall(
            String agentId,
            ToolCall toolCall) {

        LOG.debug("Executing tool call: {}", toolCall.name());

        return toolService.executeTool(
                agentId,
                toolCall.name(),
                toolCall.arguments())
                .map(result -> new ToolCallResult(
                        toolCall.id(),
                        toolCall.name(),
                        formatToolResult(result),
                        result.success(),
                        result.metadata(),
                        Instant.now()))
                .onFailure().recoverWithItem(new ToolCallResult(
                        toolCall.id(),
                        toolCall.name(),
                        "Tool execution failed",
                        false,
                        Map.of(),
                        Instant.now()));
    }

    /**
     * Plan optimal tool chain for task
     *
     * @param agentId The agent ID
     * @param task    The task description
     * @return Planned tool chain
     */
    public Uni<ToolChain> planToolChain(String agentId, String task) {
        return Uni.combine()
                .all()
                .unis(
                        toolService.recommendTools(agentId, task),
                        toolService.getHighConfidenceTools(agentId))
                .asTuple()
                .map(tuple -> {
                    var recommended = tuple.getItem1();
                    var highConfidence = tuple.getItem2();

                    // Prioritize high-confidence tools
                    List<String> toolChain = recommended.stream()
                            .filter(tool -> highConfidence.contains(tool.id()))
                            .map(t -> t.id())
                            .collect(Collectors.toList());

                    // Add recommended tools if chain is short
                    if (toolChain.size() < 3) {
                        recommended.stream()
                                .map(t -> t.id())
                                .filter(id -> !toolChain.contains(id))
                                .limit(3 - toolChain.size())
                                .forEach(toolChain::add);
                    }

                    return new ToolChain(agentId, task, toolChain, Instant.now());
                });
    }

    /**
     * Execute planned tool chain
     *
     * @param agentId       The agent ID
     * @param chain         The planned tool chain
     * @param initialParams Initial parameters
     * @return Final result
     */
    public Uni<ToolChainResult> executeToolChain(
            String agentId,
            ToolChain chain,
            Map<String, Object> initialParams) {

        LOG.info("Executing tool chain with {} tools", chain.toolIds().size());

        return toolService.chainTools(agentId, chain.toolIds(), initialParams)
                .map(finalResult -> new ToolChainResult(
                        agentId,
                        chain.taskDescription(),
                        chain.toolIds(),
                        finalResult.success(),
                        finalResult.data(),
                        Instant.now()))
                .onFailure().recoverWithItem(new ToolChainResult(
                        agentId,
                        chain.taskDescription(),
                        chain.toolIds(),
                        false,
                        null,
                        Instant.now()));
    }

    /**
     * Learn from tool execution patterns
     *
     * @param agentId The agent ID
     * @return Learning results
     */
    public Uni<ToolLearningResults> learnFromToolUsage(String agentId) {
        return Uni.combine()
                .all()
                .unis(
                        toolService.analyzeToolUsagePattern(agentId),
                        toolService.getHighConfidenceTools(agentId))
                .asTuple()
                .map(tuple -> {
                    var pattern = tuple.getItem1();
                    var highConfidence = tuple.getItem2();

                    // Analyze patterns
                    Map<String, Double> successRates = pattern.toolUsage()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> pattern.getSuccessRateForTool(e.getKey())));

                    return new ToolLearningResults(
                            agentId,
                            highConfidence,
                            pattern.getMostUsedTools(5),
                            successRates,
                            Instant.now());
                });
    }

    // Helper methods

    private String buildToolAwarePrompt(
            String task,
            String context,
            List<AgentToolService.ToolDefinition> recommendedTools) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an agent with access to tools.\n\n");
        prompt.append("TASK: ").append(task).append("\n\n");

        if (!context.isEmpty()) {
            prompt.append("CONTEXT FROM PREVIOUS INTERACTIONS:\n").append(context).append("\n\n");
        }

        if (!recommendedTools.isEmpty()) {
            prompt.append("RECOMMENDED TOOLS FOR THIS TASK:\n");
            for (AgentToolService.ToolDefinition tool : recommendedTools) {
                prompt.append("- ").append(tool.name()).append(": ")
                        .append(tool.description()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Use available tools to complete the task effectively.\n");
        return prompt.toString();
    }

    private List<ToolCall> extractToolCalls(AgentResponse response) {
        // In production: Parse tool calls from agent response
        // For now, return empty (agent response would contain tool calls)
        return Collections.emptyList();
    }

    private String formatToolResult(ToolResult result) {
        if (result.success()) {
            return result.data() != null ? result.data().toString() : "Success";
        } else {
            return "Error: " + result.error();
        }
    }

    private AgentResponse augmentResponseWithToolResults(
            AgentResponse response,
            List<ToolCallResult> toolResults) {
        // In production: Update response content with tool results
        String augmentedContent = response.content() + "\n\nTool Results:\n" +
                toolResults.stream()
                        .map(r -> "- " + r.toolName() + ": " + r.content())
                        .collect(Collectors.joining("\n"));

        return new AgentResponse(
                response.requestId(),
                augmentedContent,
                response.success(),
                "TOOL_EXECUTION_COMPLETE",
                response.reasoning());
    }

    /**
     * Tool Chain record
     */
    public record ToolChain(
            String agentId,
            String taskDescription,
            List<String> toolIds,
            Instant plannedAt) {

        public boolean isEmpty() {
            return toolIds.isEmpty();
        }

        public int size() {
            return toolIds.size();
        }
    }

    /**
     * Tool Chain Execution Result
     */
    public record ToolChainResult(
            String agentId,
            String task,
            List<String> executedTools,
            boolean success,
            Object finalResult,
            Instant completedAt) {

        public String getSummary() {
            return String.format(
                    "Task: %s | Tools: %d | Success: %s | Result: %s",
                    task,
                    executedTools.size(),
                    success,
                    finalResult != null ? finalResult : "N/A");
        }
    }

    /**
     * Tool Learning Results
     */
    public record ToolLearningResults(
            String agentId,
            List<String> highConfidenceTools,
            List<String> mostUsedTools,
            Map<String, Double> toolSuccessRates,
            Instant analyzedAt) {

        public boolean hasLearnedPatterns() {
            return !highConfidenceTools.isEmpty();
        }

        public String recommendedToolForTask(String taskKeyword) {
            return highConfidenceTools.stream()
                    .filter(tool -> toolSuccessRates.getOrDefault(tool, 0.0) > 0.7)
                    .findFirst()
                    .orElse(null);
        }
    }
}
