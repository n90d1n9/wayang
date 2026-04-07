package tech.kayys.wayang.agent.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Metrics collector for agent observability with OpenTelemetry and Micrometer integration.
 *
 * <p>This collector tracks:
 * <ul>
 *   <li>Agent execution counts and success rates</li>
 *   <li>Per-step latency and token usage</li>
 *   <li>Tool execution metrics</li>
 *   <li>Provider performance</li>
 *   <li>Tenant-specific metrics</li>
 * </ul>
 *
 * <h2>Metrics Exported:</h2>
 * <ul>
 *   <li>{@code agent.executions.total} - Counter of agent executions</li>
 *   <li>{@code agent.executions.duration} - Timer for execution duration</li>
 *   <li>{@code agent.steps.total} - Counter of reasoning steps</li>
 *   <li>{@code agent.steps.latency} - Timer for step latency</li>
 *   <li>{@code agent.tokens.used} - Counter for token consumption</li>
 *   <li>{@code agent.tools.executed} - Counter for tool executions</li>
 *   <li>{@code agent.provider.latency} - Timer for provider response times</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class AgentMetricsCollector {

    private static final Logger LOG = Logger.getLogger(AgentMetricsCollector.class);

    @Inject
    MeterRegistry meterRegistry;

    // Counters
    private Counter executionsTotal;
    private Counter executionsSuccess;
    private Counter executionsFailure;
    private Counter stepsTotal;
    private Counter toolsExecuted;
    private Counter tokensUsed;

    // Timers
    private Timer executionDuration;
    private Timer stepLatency;
    private Timer providerLatency;
    private Timer toolExecutionLatency;

    // Distribution summaries
    private DistributionSummary tokensPerExecution;
    private DistributionSummary stepsPerExecution;

    // Gauges for real-time monitoring
    private final Map<String, Number> gaugeValues = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        LOG.info("Initializing AgentMetricsCollector");

        // Initialize counters
        executionsTotal = meterRegistry.counter("agent.executions.total");
        executionsSuccess = meterRegistry.counter("agent.executions.success");
        executionsFailure = meterRegistry.counter("agent.executions.failure");
        stepsTotal = meterRegistry.counter("agent.steps.total");
        toolsExecuted = meterRegistry.counter("agent.tools.executed");
        tokensUsed = meterRegistry.counter("agent.tokens.used");

        // Initialize timers
        executionDuration = meterRegistry.timer("agent.executions.duration");
        stepLatency = meterRegistry.timer("agent.steps.latency");
        providerLatency = meterRegistry.timer("agent.provider.latency");
        toolExecutionLatency = meterRegistry.timer("agent.tools.latency");

        // Initialize distribution summaries
        tokensPerExecution = meterRegistry.summary("agent.tokens.per_execution");
        stepsPerExecution = meterRegistry.summary("agent.steps.per_execution");

        // Initialize gauges
        gaugeValues.put("active_runs", 0);
        Gauge.builder("agent.active_runs", gaugeValues, g -> g.get("active_runs").doubleValue())
            .register(meterRegistry);
    }

    /**
     * Record the start of an agent execution.
     *
     * @param runId unique run identifier
     * @param tenantId tenant identifier
     * @param strategy orchestration strategy ID
     */
    public void recordExecutionStart(String runId, String tenantId, String strategy) {
        executionsTotal.increment();
        incrementGauge("active_runs");
        
        LOG.debugf("Agent execution started: runId=%s, tenant=%s, strategy=%s",
            runId, tenantId, strategy);
    }

    /**
     * Record the completion of an agent execution.
     *
     * @param response the agent response
     * @param tenantId tenant identifier
     */
    public void recordExecutionComplete(AgentResponse response, String tenantId) {
        decrementGauge("active_runs");
        
        if (response.successful()) {
            executionsSuccess.increment();
        } else {
            executionsFailure.increment();
        }

        executionDuration.record(response.durationMs(), TimeUnit.MILLISECONDS);
        stepsPerExecution.record(response.totalSteps());

        LOG.debugf("Agent execution completed: runId=%s, success=%b, duration=%dms, steps=%d",
            response.runId(), response.successful(), response.durationMs(), response.totalSteps());
    }

    /**
     * Record a reasoning step.
     *
     * @param runId unique run identifier
     * @param stepNumber step number
     * @param durationMs step duration in milliseconds
     * @param tenantId tenant identifier
     */
    public void recordStep(String runId, int stepNumber, long durationMs, String tenantId) {
        stepsTotal.increment();
        stepLatency.record(durationMs, TimeUnit.MILLISECONDS);
        
        LOG.debugf("Step recorded: runId=%s, step=%d, duration=%dms",
            runId, stepNumber, durationMs);
    }

    /**
     * Record tool execution.
     *
     * @param toolName name of the tool
     * @param durationMs execution duration in milliseconds
     * @param successful whether execution was successful
     * @param tenantId tenant identifier
     */
    public void recordToolExecution(String toolName, long durationMs, boolean successful, String tenantId) {
        toolsExecuted.increment();
        toolExecutionLatency.record(durationMs, TimeUnit.MILLISECONDS);
        
        LOG.debugf("Tool execution recorded: tool=%s, duration=%dms, success=%b",
            toolName, durationMs, successful);
    }

    /**
     * Record token usage.
     *
     * @param inputTokens number of input tokens
     * @param outputTokens number of output tokens
     * @param tenantId tenant identifier
     */
    public void recordTokenUsage(int inputTokens, int outputTokens, String tenantId) {
        int totalTokens = inputTokens + outputTokens;
        tokensUsed.increment(totalTokens);
        tokensPerExecution.record(totalTokens);
        
        LOG.debugf("Token usage recorded: input=%d, output=%d, total=%d",
            inputTokens, outputTokens, totalTokens);
    }

    /**
     * Record provider latency.
     *
     * @param providerId provider identifier
     * @param durationMs latency in milliseconds
     * @param successful whether the request was successful
     */
    public void recordProviderLatency(String providerId, long durationMs, boolean successful) {
        providerLatency.record(durationMs, TimeUnit.MILLISECONDS);
        
        LOG.debugf("Provider latency recorded: provider=%s, duration=%dms, success=%b",
            providerId, durationMs, successful);
    }

    /**
     * Record an agent event for detailed tracing.
     *
     * @param event the agent event
     * @param tenantId tenant identifier
     */
    public void recordEvent(AgentEvent event, String tenantId) {
        LOG.debugf("Agent event recorded: type=%s, runId=%s",
            event.type(), event.runId());
    }

    /**
     * Record an error during agent execution.
     *
     * @param errorType type of error
     * @param tenantId tenant identifier
     */
    public void recordError(String errorType, String tenantId) {
        LOG.warnf("Error recorded: type=%s, tenant=%s", errorType, tenantId);
    }

    /**
     * Get current metrics summary.
     *
     * @return map of metric name to value
     */
    public Map<String, Object> getMetricsSummary() {
        return Map.of(
            "executions_total", executionsTotal.count(),
            "executions_success", executionsSuccess.count(),
            "executions_failure", executionsFailure.count(),
            "success_rate", calculateSuccessRate(),
            "avg_execution_duration_ms", executionDuration.totalTime(TimeUnit.MILLISECONDS) / 
                Math.max(1, executionsTotal.count()),
            "avg_step_latency_ms", stepLatency.totalTime(TimeUnit.MILLISECONDS) /
                Math.max(1, stepsTotal.count()),
            "total_tokens", tokensUsed.count(),
            "active_runs", gaugeValues.get("active_runs")
        );
    }

    /**
     * Get tenant-specific metrics.
     *
     * @param tenantId tenant identifier
     * @return tenant metrics (currently global, can be extended for per-tenant tracking)
     */
    public Map<String, Object> getTenantMetrics(String tenantId) {
        // Currently returns global metrics - can be extended with tenant tags
        return getMetricsSummary();
    }

    /**
     * Reset all metrics.
     */
    public void reset() {
        gaugeValues.put("active_runs", 0);
        LOG.info("Metrics reset");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    private void incrementGauge(String name) {
        gaugeValues.compute(name, (k, v) -> 
            v == null ? 1 : v.intValue() + 1);
    }

    private void decrementGauge(String name) {
        gaugeValues.compute(name, (k, v) -> 
            v == null ? 0 : Math.max(0, v.intValue() - 1));
    }

    private double calculateSuccessRate() {
        double total = executionsTotal.count();
        if (total == 0) return 0;
        return executionsSuccess.count() / total;
    }
}
