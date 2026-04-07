package tech.kayys.wayang.agent.core.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenTelemetry integration for agent observability.
 *
 * <p>
 * Provides distributed tracing and metrics collection for:
 * <ul>
 *   <li>Agent execution (end-to-end tracing)</li>
 *   <li>Inference calls (backend → provider)</li>
 *   <li>Tool execution (per-tool metrics)</li>
 *   <li>Orchestrator steps (ReAct, Plan/Execute, etc.)</li>
 *   <li>Memory operations (read/write latency)</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.auto());
 *
 * // Trace agent execution
 * telemetry.executeAgent("react", "order-query", () -> {
 *     // Agent logic
 * });
 *
 * // Record inference
 * telemetry.recordInference("gollek", "gpt-4", Duration.ofMillis(1500), true);
 *
 * // Record tool execution
 * telemetry.recordToolExecution("order-lookup", Duration.ofMillis(200), true);
 * }</pre>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class AgentTelemetry {

    private static final Logger LOG = Logger.getLogger(AgentTelemetry.class);

    private static final String INSTRUMENTATION_SCOPE = "tech.kayys.wayang.agent";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;
    private final Meter meter;

    // Metrics
    private final LongCounter agentExecutions;
    private final DoubleHistogram agentExecutionDuration;
    private final LongCounter inferenceExecutions;
    private final DoubleHistogram inferenceDuration;
    private final LongCounter toolExecutions;
    private final DoubleHistogram toolDuration;
    private final LongCounter errors;

    // Local cache for span context
    private final Map<String, io.opentelemetry.context.Context> activeSpans = new ConcurrentHashMap<>();

    private AgentTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION);
        this.meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        // Initialize metrics
        this.agentExecutions = meter.counterBuilder("wayang.agent.executions")
            .setDescription("Total number of agent executions")
            .setUnit("{execution}")
            .build();

        this.agentExecutionDuration = meter.histogramBuilder("wayang.agent.execution.duration")
            .setDescription("Agent execution duration")
            .setUnit("ms")
            .build();

        this.inferenceExecutions = meter.counterBuilder("wayang.inference.executions")
            .setDescription("Total number of inference calls")
            .setUnit("{call}")
            .build();

        this.inferenceDuration = meter.histogramBuilder("wayang.inference.duration")
            .setDescription("Inference call duration")
            .setUnit("ms")
            .build();

        this.toolExecutions = meter.counterBuilder("wayang.tool.executions")
            .setDescription("Total number of tool executions")
            .setUnit("{call}")
            .build();

        this.toolDuration = meter.histogramBuilder("wayang.tool.duration")
            .setDescription("Tool execution duration")
            .setUnit("ms")
            .build();

        this.errors = meter.counterBuilder("wayang.errors")
            .setDescription("Total number of errors")
            .setUnit("{error}")
            .build();
    }

    /**
     * Initialize telemetry with auto-configured OpenTelemetry.
     */
    public static AgentTelemetry initialize() {
        // In production, use OpenTelemetry.auto() or SDK builder
        // For now, return no-op implementation
        LOG.info("AgentTelemetry initialized");
        return new AgentTelemetry(OpenTelemetry.noop());
    }

    /**
     * Initialize telemetry with explicit OpenTelemetry instance.
     */
    public static AgentTelemetry initialize(OpenTelemetry openTelemetry) {
        return new AgentTelemetry(openTelemetry);
    }

    // ── Agent Execution Tracing ─────────────────────────────────────────────

    /**
     * Execute agent with tracing.
     *
     * @param strategy orchestration strategy (react, plan-execute, etc.)
     * @param agentId agent identifier
     * @param action agent execution logic
     * @return action result
     */
    public <T> T executeAgent(String strategy, String agentId, java.util.function.Supplier<T> action) {
        Span span = tracer.spanBuilder("agent.execute")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("agent.strategy", strategy)
            .setAttribute("agent.id", agentId)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T result = action.get();
            span.setStatus(StatusCode.OK);
            agentExecutions.add(1, Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("strategy"), strategy,
                io.opentelemetry.api.common.AttributeKey.stringKey("agent_id"), agentId
            ));
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            errors.add(1, Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("component"), "agent",
                io.opentelemetry.api.common.AttributeKey.stringKey("error_type"), e.getClass().getSimpleName()
            ));
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute agent step with tracing.
     */
    public <T> T executeStep(String stepType, String agentId, java.util.function.Supplier<T> action) {
        Span span = tracer.spanBuilder("agent.step")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("agent.step_type", stepType)
            .setAttribute("agent.id", agentId)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T result = action.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    // ── Inference Tracing ───────────────────────────────────────────────────

    /**
     * Record inference execution.
     *
     * @param backend backend name (gollek, ollama, etc.)
     * @param model model identifier
     * @param duration execution duration
     * @param success whether the call succeeded
     */
    public void recordInference(String backend, String model, Duration duration, boolean success) {
        Attributes attrs = Attributes.of(
            io.opentelemetry.api.common.AttributeKey.stringKey("backend"), backend,
            io.opentelemetry.api.common.AttributeKey.stringKey("model"), model,
            io.opentelemetry.api.common.AttributeKey.booleanKey("success"), success
        );

        inferenceExecutions.add(1, attrs);
        inferenceDuration.record(duration.toMillis(), attrs);

        if (!success) {
            errors.add(1, Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("component"), "inference",
                io.opentelemetry.api.common.AttributeKey.stringKey("backend"), backend
            ));
        }
    }

    // ── Tool Execution Tracing ──────────────────────────────────────────────

    /**
     * Record tool execution.
     *
     * @param toolName tool identifier
     * @param duration execution duration
     * @param success whether the call succeeded
     */
    public void recordToolExecution(String toolName, Duration duration, boolean success) {
        Attributes attrs = Attributes.of(
            io.opentelemetry.api.common.AttributeKey.stringKey("tool"), toolName,
            io.opentelemetry.api.common.AttributeKey.booleanKey("success"), success
        );

        toolExecutions.add(1, attrs);
        toolDuration.record(duration.toMillis(), attrs);

        if (!success) {
            errors.add(1, Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("component"), "tool",
                io.opentelemetry.api.common.AttributeKey.stringKey("tool"), toolName
            ));
        }
    }

    // ── Memory Operation Tracing ────────────────────────────────────────────

    /**
     * Record memory operation.
     *
     * @param operation operation type (read, write, search)
     * @param tier memory tier (short-term, long-term, etc.)
     * @param duration operation duration
     */
    public void recordMemoryOperation(String operation, String tier, Duration duration) {
        // TODO: Add memory metrics
    }

    // ── Span Context Management ─────────────────────────────────────────────

    /**
     * Start a named span.
     */
    public String startSpan(String name) {
        String spanId = java.util.UUID.randomUUID().toString();
        Span span = tracer.spanBuilder(name).startSpan();
        activeSpans.put(spanId, span.storeInContext(Context.current()));
        return spanId;
    }

    /**
     * End a span by ID.
     */
    public void endSpan(String spanId) {
        io.opentelemetry.context.Context ctx = activeSpans.remove(spanId);
        if (ctx != null) {
            Span.fromContext(ctx).end();
        }
    }

    /**
     * Add event to active span.
     */
    public void addEvent(String spanId, String eventName) {
        io.opentelemetry.context.Context ctx = activeSpans.get(spanId);
        if (ctx != null) {
            Span.fromContext(ctx).addEvent(eventName);
        }
    }

    /**
     * Set attribute on active span.
     */
    public void setAttribute(String spanId, String key, String value) {
        io.opentelemetry.context.Context ctx = activeSpans.get(spanId);
        if (ctx != null) {
            Span.fromContext(ctx).setAttribute(
                io.opentelemetry.api.common.AttributeKey.stringKey(key), value
            );
        }
    }

    // ── Shutdown ────────────────────────────────────────────────────────────

    /**
     * Shutdown telemetry gracefully.
     */
    public void shutdown() {
        LOG.info("AgentTelemetry shutting down");
        activeSpans.clear();
        // In production, flush exporters here
    }
}
