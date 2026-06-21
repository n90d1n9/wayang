package tech.kayys.wayang.agent.core.selector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic strategy selector that chooses the best orchestration strategy based on task characteristics.
 *
 * <p>This selector analyzes incoming requests and automatically selects the most appropriate
 * orchestration strategy based on:
 * <ul>
 *   <li>Task complexity (simple vs complex reasoning)</li>
 *   <li>Required capabilities (tool calling, multi-agent, etc.)</li>
 *   <li>Performance requirements (latency, cost)</li>
 *   <li>Historical success rates for different strategies</li>
 * </ul>
 *
 * <h2>Strategy Selection Heuristics:</h2>
 * <ul>
 *   <li><b>Simple Q&A:</b> Direct inference (no orchestration)</li>
 *   <li><b>Tool-required tasks:</b> ReAct or ToolCalling</li>
 *   <li><b>Complex reasoning:</b> Plan-and-Execute</li>
 *   <li><b>Quality-critical tasks:</b> Reflexion</li>
 *   <li><b>Multi-domain tasks:</b> Multi-Agent</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class AdaptiveStrategySelector {

    private static final Logger LOG = Logger.getLogger(AdaptiveStrategySelector.class);

    @Inject
    Instance<AgentOrchestrator> availableOrchestrators;

    private final Map<String, StrategyStats> strategyStats = new ConcurrentHashMap<>();
    private final Map<String, String> taskTypeCache = new ConcurrentHashMap<>();

    // Task type constants
    private static final String TASK_SIMPLE = "simple";
    private static final String TASK_TOOL_REQUIRED = "tool_required";
    private static final String TASK_COMPLEX = "complex";
    private static final String TASK_QUALITY_CRITICAL = "quality_critical";
    private static final String TASK_MULTI_DOMAIN = "multi_domain";

    /**
     * Select the best orchestrator for a given request.
     *
     * @param request the agent request
     * @return selected orchestrator
     */
    public AgentOrchestrator selectOrchestrator(AgentRequest request) {
        String taskType = analyzeTaskType(request);
        String strategyId = selectStrategyForTaskType(taskType, request);
        
        LOG.infof("Selected strategy '%s' for task type '%s'", strategyId, taskType);

        return findOrchestrator(strategyId)
            .orElseGet(() -> {
                LOG.warnf("Strategy %s not found, using default", strategyId);
                return availableOrchestrators.iterator().next();
            });
    }

    /**
     * Select strategy with explicit task type override.
     *
     * @param taskType explicit task type
     * @param request the agent request
     * @return selected strategy ID
     */
    public String selectStrategyForTaskType(String taskType, AgentRequest request) {
        return switch (taskType) {
            case TASK_SIMPLE -> "direct";
            case TASK_TOOL_REQUIRED -> hasToolCallingSupport() ? "tool-calling" : "react";
            case TASK_COMPLEX -> "plan-and-execute";
            case TASK_QUALITY_CRITICAL -> "reflexion";
            case TASK_MULTI_DOMAIN -> "multi-agent";
            default -> "react"; // Default fallback
        };
    }

    /**
     * Analyze the task type based on request characteristics.
     *
     * @param request the agent request
     * @return task type classification
     */
    public String analyzeTaskType(AgentRequest request) {
        // Check cache first
        String cacheKey = request.prompt().hashCode() + ":" + 
            (request.allowedSkills() != null ? request.allowedSkills().hashCode() : 0);
        
        String cached = taskTypeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String taskType = analyzeTaskTypeUncached(request);
        taskTypeCache.put(cacheKey, taskType);
        return taskType;
    }

    /**
     * Record execution result for a strategy.
     *
     * @param strategyId the strategy ID
     * @param successful whether execution was successful
     * @param durationMs execution duration in milliseconds
     * @param qualityScore optional quality score (0-1)
     */
    public void recordResult(String strategyId, boolean successful, long durationMs, Double qualityScore) {
        StrategyStats stats = strategyStats.computeIfAbsent(strategyId, k -> new StrategyStats());
        stats.record(successful, durationMs, qualityScore);
        
        LOG.debugf("Recorded result for strategy %s: success=%b, duration=%dms, quality=%.2f",
            strategyId, successful, durationMs, qualityScore != null ? qualityScore : 0);
    }

    /**
     * Get strategy statistics.
     *
     * @param strategyId the strategy ID
     * @return strategy statistics
     */
    public StrategyStats getStrategyStats(String strategyId) {
        return strategyStats.getOrDefault(strategyId, new StrategyStats());
    }

    /**
     * Get all available strategies with their statistics.
     *
     * @return map of strategy ID to stats
     */
    public Map<String, StrategyStats> getAllStrategyStats() {
        return new HashMap<>(strategyStats);
    }

    /**
     * Clear the task type cache.
     */
    public void clearTaskCache() {
        taskTypeCache.clear();
    }

    /**
     * Clear all statistics.
     */
    public void clearStats() {
        strategyStats.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Internal Methods
    // ═══════════════════════════════════════════════════════════════════════

    private String analyzeTaskTypeUncached(AgentRequest request) {
        String prompt = request.prompt().toLowerCase();
        
        // Check for multi-domain indicators
        if (isMultiDomainTask(prompt, request)) {
            return TASK_MULTI_DOMAIN;
        }

        // Check for quality-critical indicators
        if (isQualityCriticalTask(prompt, request)) {
            return TASK_QUALITY_CRITICAL;
        }

        // Check for tool requirements
        if (requiresTools(request)) {
            return TASK_TOOL_REQUIRED;
        }

        // Check for complexity indicators
        if (isComplexTask(prompt, request)) {
            return TASK_COMPLEX;
        }

        // Default to simple task
        return TASK_SIMPLE;
    }

    private boolean isMultiDomainTask(String prompt, AgentRequest request) {
        // Check if multiple skill domains are involved
        if (request.allowedSkills() != null && request.allowedSkills().size() > 2) {
            return true;
        }

        // Check for multi-domain keywords
        return prompt.contains("compare") || 
               prompt.contains("analyze multiple") ||
               prompt.contains("across different") ||
               prompt.contains("from various sources");
    }

    private boolean isQualityCriticalTask(String prompt, AgentRequest request) {
        // Check for quality-critical indicators
        return prompt.contains("important") ||
               prompt.contains("critical") ||
               prompt.contains("must be accurate") ||
               prompt.contains("verify") ||
               prompt.contains("validate") ||
               request.parameters() != null && 
                   Boolean.TRUE.equals(request.parameters().get("quality_critical"));
    }

    private boolean requiresTools(AgentRequest request) {
        // Check if tools/skills are explicitly required
        if (request.allowedSkills() != null && !request.allowedSkills().isEmpty()) {
            return true;
        }

        // Check for tool-requiring keywords
        String prompt = request.prompt().toLowerCase();
        return prompt.contains("calculate") ||
               prompt.contains("search") ||
               prompt.contains("execute") ||
               prompt.contains("run code") ||
               prompt.contains("query") ||
               prompt.contains("fetch");
    }

    private boolean isComplexTask(String prompt, AgentRequest request) {
        // Check for complexity indicators
        int wordCount = prompt.split("\\s+").length;
        if (wordCount > 50) {
            return true;
        }

        // Check for multi-step indicators
        return prompt.contains("first") && prompt.contains("then") ||
               prompt.contains("step") ||
               prompt.contains("plan") ||
               prompt.contains("break down") ||
               prompt.contains("multiple steps") ||
               prompt.contains("complex");
    }

    private boolean hasToolCallingSupport() {
        return availableOrchestrators.stream()
            .anyMatch(o -> "tool-calling".equals(o.strategyId()));
    }

    private Optional<AgentOrchestrator> findOrchestrator(String strategyId) {
        return availableOrchestrators.stream()
            .filter(o -> strategyId.equals(o.strategyId()))
            .findFirst();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Strategy Statistics
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Strategy execution statistics with exponential moving averages.
     */
    public static class StrategyStats {
        private int totalExecutions = 0;
        private int successfulExecutions = 0;
        private double avgDurationMs = 0;
        private double avgQualityScore = 0;
        private static final double ALPHA = 0.1; // EMA factor

        public synchronized void record(boolean successful, long durationMs, Double qualityScore) {
            totalExecutions++;
            if (successful) {
                successfulExecutions++;
            }

            // Exponential moving average for duration
            avgDurationMs = ALPHA * durationMs + (1 - ALPHA) * avgDurationMs;

            // Exponential moving average for quality (if provided)
            if (qualityScore != null) {
                avgQualityScore = ALPHA * qualityScore + (1 - ALPHA) * avgQualityScore;
            }
        }

        public int getTotalExecutions() {
            return totalExecutions;
        }

        public int getSuccessfulExecutions() {
            return successfulExecutions;
        }

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0;
        }

        public double getAvgDurationMs() {
            return avgDurationMs;
        }

        public double getAvgQualityScore() {
            return avgQualityScore;
        }

        public double getEffectivenessScore() {
            // Combined metric: success rate * quality / normalized duration
            double normalizedDuration = avgDurationMs > 0 ? 10000 / avgDurationMs : 1;
            return getSuccessRate() * avgQualityScore * normalizedDuration;
        }

        @Override
        public String toString() {
            return String.format("StrategyStats{executions=%d, successRate=%.2f, avgDuration=%.0fms, quality=%.2f}",
                totalExecutions, getSuccessRate(), avgDurationMs, avgQualityScore);
        }
    }
}
