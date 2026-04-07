package tech.kayys.wayang.agent.core.audit;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Audit logger for agent actions.
 *
 * <p>
 * Records all agent actions for compliance, debugging, and analysis:
 * <ul>
 *   <li>Agent execution (start, end, result)</li>
 *   <li>Inference calls (backend, model, latency)</li>
 *   <li>Tool execution (tool name, inputs, outputs)</li>
 *   <li>Memory operations (read, write, search)</li>
 *   <li>Errors and failures</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * AgentAuditLogger audit = AgentAuditLogger.fileBased(Path.of("audit.log"));
 *
 * audit.logAgentExecution("agent-123", "react", "order-query", 1500, true);
 * audit.logInferenceCall("gollek", "gpt-4", 1200, true);
 * audit.logToolExecution("order-lookup", "{\"orderId\": \"123\"}", 200, true);
 * audit.logError("agent-123", "TimeoutException", "Inference timed out");
 * }</pre>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class AgentAuditLogger {

    private static final Logger LOG = Logger.getLogger(AgentAuditLogger.class);

    private final AuditSink sink;
    private final boolean async;

    private AgentAuditLogger(AuditSink sink, boolean async) {
        this.sink = sink;
        this.async = async;
    }

    /**
     * Create file-based audit logger.
     */
    public static AgentAuditLogger fileBased(Path logFile) throws IOException {
        Files.createDirectories(logFile.getParent());
        return new AgentAuditLogger(new FileAuditSink(logFile), false);
    }

    /**
     * Create memory-based audit logger (for testing).
     */
    public static AgentAuditLogger memoryBased() {
        return new AgentAuditLogger(new MemoryAuditSink(), false);
    }

    // ── Agent Execution ─────────────────────────────────────────────────────

    /**
     * Log agent execution.
     */
    public void logAgentExecution(String agentId, String strategy, String prompt, long durationMs, boolean success) {
        AuditEvent event = new AuditEvent(
            "agent.execution",
            agentId,
            Map.of(
                "strategy", strategy,
                "prompt", truncate(prompt, 100),
                "duration_ms", durationMs,
                "success", success
            )
        );
        record(event);
    }

    /**
     * Log agent step.
     */
    public void logAgentStep(String agentId, String stepType, String details) {
        AuditEvent event = new AuditEvent(
            "agent.step",
            agentId,
            Map.of(
                "step_type", stepType,
                "details", truncate(details, 200)
            )
        );
        record(event);
    }

    // ── Inference ───────────────────────────────────────────────────────────

    /**
     * Log inference call.
     */
    public void logInferenceCall(String backend, String model, long durationMs, boolean success) {
        AuditEvent event = new AuditEvent(
            "inference.call",
            backend,
            Map.of(
                "model", model,
                "duration_ms", durationMs,
                "success", success
            )
        );
        record(event);
    }

    // ── Tool Execution ──────────────────────────────────────────────────────

    /**
     * Log tool execution.
     */
    public void logToolExecution(String toolName, String input, long durationMs, boolean success) {
        AuditEvent event = new AuditEvent(
            "tool.execution",
            toolName,
            Map.of(
                "input", truncate(input, 200),
                "duration_ms", durationMs,
                "success", success
            )
        );
        record(event);
    }

    // ── Memory Operations ───────────────────────────────────────────────────

    /**
     * Log memory operation.
     */
    public void logMemoryOperation(String operation, String tier, String key, long durationMs) {
        AuditEvent event = new AuditEvent(
            "memory.operation",
            tier,
            Map.of(
                "operation", operation,
                "key", key,
                "duration_ms", durationMs
            )
        );
        record(event);
    }

    // ── Errors ──────────────────────────────────────────────────────────────

    /**
     * Log error.
     */
    public void logError(String component, String errorType, String message) {
        AuditEvent event = new AuditEvent(
            "error",
            component,
            Map.of(
                "error_type", errorType,
                "message", truncate(message, 500)
            )
        );
        record(event);
    }

    // ── Security ────────────────────────────────────────────────────────────

    /**
     * Log security event.
     */
    public void logSecurityEvent(String eventType, String details, String tenantId) {
        AuditEvent event = new AuditEvent(
            "security." + eventType,
            tenantId,
            Map.of("details", truncate(details, 500))
        );
        record(event);
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private void record(AuditEvent event) {
        if (async) {
            // TODO: Implement async recording
            sink.record(event);
        } else {
            sink.record(event);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    // ── Audit Event Record ──────────────────────────────────────────────────

    /**
     * Audit event record.
     */
    public record AuditEvent(
            Instant timestamp,
            String eventType,
            String component,
            Map<String, Object> attributes) {

        public AuditEvent(String eventType, String component, Map<String, Object> attributes) {
            this(Instant.now(), eventType, component, attributes);
        }

        @Override
        public String toString() {
            return String.format("[%s] %s.%s %s",
                timestamp, component, eventType, attributes);
        }
    }

    // ── Audit Sink Interface ────────────────────────────────────────────────

    /**
     * Audit sink interface.
     */
    public interface AuditSink {
        void record(AuditEvent event);
    }

    /**
     * File-based audit sink.
     */
    private static class FileAuditSink implements AuditSink {
        private final Path logFile;

        FileAuditSink(Path logFile) {
            this.logFile = logFile;
        }

        @Override
        public void record(AuditEvent event) {
            try {
                String line = event.toString() + "\n";
                Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                LOG.errorf(e, "Failed to write audit event: %s", event);
            }
        }
    }

    /**
     * Memory-based audit sink (for testing).
     */
    public static class MemoryAuditSink implements AuditSink {
        private final Queue<AuditEvent> events = new ConcurrentLinkedQueue<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }

        public List<AuditEvent> getEvents() {
            return new ArrayList<>(events);
        }

        public void clear() {
            events.clear();
        }
    }
}
