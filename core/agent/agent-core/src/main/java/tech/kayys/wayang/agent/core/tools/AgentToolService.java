package tech.kayys.wayang.agent.core.tools;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.tools.spi.*;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool Execution Service with Memory Integration
 *
 * Bridges the tool registry with agent memory to provide:
 * - Tool discovery and selection
 * - Execution with memory context
 * - Result tracking and learning
 * - Performance metrics
 * - Tool usage patterns
 *
 * Usage:
 * {@code
 * @Inject
 * AgentToolService toolService;
 *
 * // Get available tools for agent
 * List<ToolDefinition> tools = toolService.getToolsForAgent(agentId);
 *
 * // Execute tool with memory context
 * ToolResult result = toolService.executeTool(agentId, toolId, params);
 *
 * // Get tool performance metrics
 * ToolMetrics metrics = toolService.getToolMetrics(agentId, toolId);
 * }
 */
@ApplicationScoped
public class AgentToolService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentToolService.class);

    @Inject
    ToolRegistry toolRegistry;

    @Inject
    AgentMemoryService agentMemoryService;

    /**
     * Get all available tools as LLM-compatible definitions
     *
     * @return List of tool definitions for LLM consumption
     */
    public Uni<List<Map<String, Object>>> getAvailableTools() {
        return Uni.createFrom().item(() -> {
            try {
                return toolRegistry.getToolDefinitions(); // Changed from toToolDefinitions
            } catch (Exception e) {
                LOG.warn("Failed to get tool definitions: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Get tools for specific agent (with memory context)
     *
     * @param agentId The agent ID
     * @return Tools most relevant to this agent's history
     */
    public Uni<List<ToolDefinition>> getToolsForAgent(String agentId) {
        return Uni.combine()
                .all()
                .unis(
                        Uni.createFrom().item(() -> getAllTools()),
                        agentMemoryService.getMemoryStats(agentId)
                )
                .asTuple()
                .map(tuple -> {
                    List<ToolDefinition> allTools = tuple.getItem1();
                    var stats = tuple.getItem2();

                    // Filter based on agent's history
                    return allTools.stream()
                            .sorted((a, b) -> {
                                // Tools used more recently in agent's history get priority
                                Integer aUsage = getToolUsageCount(agentId, a.id());
                                Integer bUsage = getToolUsageCount(agentId, b.id());
                                return bUsage.compareTo(aUsage);
                            })
                            .collect(Collectors.toList());
                });
    }

    /**
     * Execute a tool with memory context
     *
     * @param agentId    The agent ID
     * @param toolId     The tool ID to execute
     * @param parameters Tool parameters
     * @return Reactive tool result
     */
    public Uni<ToolResult> executeTool(
            String agentId,
            String toolId,
            Map<String, Object> parameters) {

        return executeTool(agentId, toolId, parameters, new ToolContext(
                toolId,
                parameters,
                null,
                Collections.emptyMap(),
                null,
                false,
                Map.of("agentId", agentId)
        ));
    }

    /**
     * Execute tool with full context
     *
     * @param agentId The agent ID
     * @param toolId  The tool ID
     * @param params  Parameters
     * @param context Full tool context
     * @return Reactive tool result
     */
    public Uni<ToolResult> executeTool(
            String agentId,
            String toolId,
            Map<String, Object> params,
            ToolContext context) {

        LOG.info("Executing tool {} for agent {}", toolId, agentId);

        // Update context with agent ID
        ToolContext enrichedContext = new ToolContext(
                context.toolId(),
                context.inputs(),
                context.workingDirectory(),
                context.environment(),
                context.timeout(),
                context.dryRun(),
                Map.of("agentId", agentId)
        );

        return Uni.createFrom().item(() -> toolRegistry.getTool(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId))
                .execute(params != null ? params : Map.of(), enrichedContext))
                // Store execution in memory
                .flatMap(result -> {
                    return storeToolExecution(agentId, toolId, params, result)
                            .map(__ -> result);
                })
                .onFailure().invoke(ex -> {
                    LOG.error("Tool execution failed: {} - {}", toolId, ex.getMessage());
                });
    }

    /**
     * Store tool execution in agent memory
     */
    private Uni<Void> storeToolExecution(
            String agentId,
            String toolId,
            Map<String, Object> params,
            ToolResult result) {

        String content = "Tool: " + toolId +
                " | Params: " + params +
                " | Success: " + result.success() +
                " | Result: " + (result.data() != null ? result.data().toString() : "null");

        MemoryEntry entry = new MemoryEntry(
                UUID.randomUUID().toString(),
                content,
                Instant.now(),
                Map.of(
                        "agentId", agentId,
                        "type", "tool-execution",
                        "toolId", toolId,
                        "success", String.valueOf(result.success()),
                        "source", "agent-tool-execution"
                )
        );

        return agentMemoryService.vectorAgentMemory()
                .store(agentId, entry)
                .onFailure().recoverWithItem((Void) null);
    }

    /**
     * Get tool execution metrics
     *
     * @param agentId The agent ID
     * @param toolId  The tool ID
     * @return Reactive metrics record
     */
    public Uni<ToolMetrics> getToolMetrics(String agentId, String toolId) {
        return agentMemoryService.getMemoryStats(agentId)
                .map(stats -> {
                    // In production, fetch real metrics from metrics service
                    // For now, calculate from memory
                    return new ToolMetrics(
                            agentId,
                            toolId,
                            getToolUsageCount(agentId, toolId),
                            getToolSuccessRate(agentId, toolId),
                            getAverageToolExecutionTime(agentId, toolId),
                            Instant.now()
                    );
                });
    }

    /**
     * Get tools most likely to help with task
     *
     * @param agentId The agent ID
     * @param task    The task description
     * @return Recommended tools
     */
    public Uni<List<ToolDefinition>> recommendTools(String agentId, String task) {
        return getToolsForAgent(agentId)
                .map(tools -> tools.stream()
                        .filter(tool -> isRelevantToTask(tool, task))
                        .limit(5)
                        .collect(Collectors.toList()));
    }

    /**
     * Chain tool executions based on memory and task
     *
     * @param agentId The agent ID
     * @param toolIds Sequence of tool IDs
     * @param params  Initial parameters (passed through chain)
     * @return Final result after all tools execute
     */
    public Uni<ToolResult> chainTools(
            String agentId,
            List<String> toolIds,
            Map<String, Object> params) {

        if (toolIds.isEmpty()) {
            return Uni.createFrom().item(ToolResult.error("No tools in chain"));
        }

        return executeTool(agentId, toolIds.get(0), params)
                .flatMap(result -> {
                    if (toolIds.size() == 1) {
                        return Uni.createFrom().item(result);
                    }

                    // Chain remaining tools
                    Map<String, Object> nextParams = new HashMap<>(params);
                    if (result.success() && result.data() != null) {
                        nextParams.put("previous_output", result.data());
                    }

                    return chainTools(agentId, toolIds.subList(1, toolIds.size()), nextParams);
                });
    }

    /**
     * Analyze tool execution pattern for agent
     *
     * @param agentId The agent ID
     * @return Tool usage analysis
     */
    public Uni<ToolUsagePattern> analyzeToolUsagePattern(String agentId) {
        return agentMemoryService.getSessionMemories(agentId, null, 100)
                .map(memories -> {
                    Map<String, Integer> toolUsage = new HashMap<>();
                    Map<String, Integer> toolSuccess = new HashMap<>();

                    for (MemoryEntry memory : memories) {
                        if (memory.getMetadata().get("type").equals("tool-execution")) {
                            String toolId = (String) memory.getMetadata().get("toolId");
                            boolean success = Boolean.parseBoolean(
                                    (String) memory.getMetadata().get("success")
                            );

                            toolUsage.merge(toolId, 1, Integer::sum);
                            if (success) {
                                toolSuccess.merge(toolId, 1, Integer::sum);
                            }
                        }
                    }

                    return new ToolUsagePattern(
                            agentId,
                            toolUsage,
                            toolSuccess,
                            Instant.now()
                    );
                });
    }

    /**
     * Get tools that are frequently successful
     *
     * @param agentId The agent ID
     * @return High-confidence tools
     */
    public Uni<List<String>> getHighConfidenceTools(String agentId) {
        return analyzeToolUsagePattern(agentId)
                .map(pattern -> pattern.toolSuccess()
                        .entrySet()
                        .stream()
                        .filter(entry -> {
                            int total = pattern.toolUsage().getOrDefault(entry.getKey(), 0);
                            return total > 0 && (double) entry.getValue() / total > 0.8;
                        })
                        .map(Map.Entry::getKey)
                        .limit(10)
                        .collect(Collectors.toList()));
    }

    /**
     * Enable/disable tool for agent
     *
     * @param toolId  The tool ID
     * @param enabled Whether to enable the tool
     */
    public Uni<Void> setToolEnabled(String toolId, boolean enabled) {
        return Uni.createFrom().voidItem()
                .invoke(__ -> {
                    if (enabled) {
                        toolRegistry.enable(toolId);
                    } else {
                        toolRegistry.disable(toolId);
                    }
                });
    }

    // Helper methods

    private List<ToolDefinition> getAllTools() {
        return toolRegistry.getToolDefinitions().stream() // Changed from toToolDefinitions
                .map(map -> new ToolDefinition(
                        (String) map.get("id"),
                        (String) map.get("name"),
                        (String) map.get("description")
                ))
                .collect(Collectors.toList());
    }

    private int getToolUsageCount(String agentId, String toolId) {
        // In production, query metrics service
        // For now, return 0 (would be computed from memory)
        return 0;
    }

    private double getToolSuccessRate(String agentId, String toolId) {
        // In production, query metrics service
        return 0.0;
    }

    private long getAverageToolExecutionTime(String agentId, String toolId) {
        // In production, query metrics service
        return 0;
    }

    private boolean isRelevantToTask(ToolDefinition tool, String task) {
        // Simple relevance check: description contains task keywords
        return tool.description().toLowerCase().contains(task.toLowerCase());
    }

    /**
     * Tool Definition record
     */
    public record ToolDefinition(
            String id,
            String name,
            String description) {
    }

    /**
     * Tool Metrics record
     */
    public record ToolMetrics(
            String agentId,
            String toolId,
            int executionCount,
            double successRate,
            long averageExecutionTimeMs,
            Instant recordedAt) {

        public boolean isHighConfidence() {
            return executionCount >= 3 && successRate >= 0.8;
        }

        public boolean isLowConfidence() {
            return executionCount < 3 || successRate < 0.5;
        }
    }

    /**
     * Tool Usage Pattern record
     */
    public record ToolUsagePattern(
            String agentId,
            Map<String, Integer> toolUsage,
            Map<String, Integer> toolSuccess,
            Instant analyzedAt) {

        public List<String> getMostUsedTools(int limit) {
            return toolUsage.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        public double getSuccessRateForTool(String toolId) {
            int total = toolUsage.getOrDefault(toolId, 0);
            if (total == 0) return 0.0;
            int successful = toolSuccess.getOrDefault(toolId, 0);
            return (double) successful / total;
        }
    }
}
