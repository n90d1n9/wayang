package tech.kayys.wayang.agent.core.audit;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
public class AgentAuditLogger implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(AgentAuditLogger.class);

    private final AuditSink sink;
    private final boolean async;
    private final ExecutorService executor;

    private AgentAuditLogger(AuditSink sink, boolean async) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.async = async;
        this.executor = async ? Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "agent-audit-logger");
            thread.setDaemon(true);
            return thread;
        }) : null;
    }

    /**
     * Create file-based audit logger.
     */
    public static AgentAuditLogger fileBased(Path logFile) throws IOException {
        Objects.requireNonNull(logFile, "logFile");
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return new AgentAuditLogger(new FileAuditSink(logFile), false);
    }

    /**
     * Create async file-based audit logger.
     */
    public static AgentAuditLogger fileBasedAsync(Path logFile) throws IOException {
        Objects.requireNonNull(logFile, "logFile");
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return new AgentAuditLogger(new FileAuditSink(logFile), true);
    }

    /**
     * Create memory-based audit logger (for testing).
     */
    public static AgentAuditLogger memoryBased() {
        return new AgentAuditLogger(new MemoryAuditSink(), false);
    }

    /**
     * Create async memory-based audit logger.
     */
    public static AgentAuditLogger memoryBasedAsync() {
        return new AgentAuditLogger(new MemoryAuditSink(), true);
    }

    /**
     * Create audit logger backed by a custom sink.
     */
    public static AgentAuditLogger sinkBased(AuditSink sink) {
        return new AgentAuditLogger(sink, false);
    }

    /**
     * Create async audit logger backed by a custom sink.
     */
    public static AgentAuditLogger sinkBasedAsync(AuditSink sink) {
        return new AgentAuditLogger(sink, true);
    }

    // ── Agent Execution ─────────────────────────────────────────────────────

    /**
     * Log agent execution.
     */
    public void logAgentExecution(String agentId, String strategy, String prompt, long durationMs, boolean success) {
        AuditEvent event = new AuditEvent(
            "agent.execution",
            agentId,
            auditAttributes(
                "strategy", strategy,
                "prompt", truncate(prompt, 100),
                "duration_ms", positiveDuration(durationMs),
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
            auditAttributes(
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
            auditAttributes(
                "model", model,
                "duration_ms", positiveDuration(durationMs),
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
            auditAttributes(
                "input", truncate(input, 200),
                "duration_ms", positiveDuration(durationMs),
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
            auditAttributes(
                "operation", operation,
                "key", key,
                "duration_ms", positiveDuration(durationMs)
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
            auditAttributes(
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
            "security." + normalize(eventType, "unknown"),
            tenantId,
            auditAttributes("details", truncate(details, 500))
        );
        record(event);
    }

    /**
     * Return memory-sink events when this logger uses an in-memory sink.
     */
    public List<AuditEvent> getEvents() {
        if (sink instanceof MemoryAuditSink memoryAuditSink) {
            return memoryAuditSink.getEvents();
        }
        return List.of();
    }

    /**
     * Clear memory-sink events when this logger uses an in-memory sink.
     */
    public void clearEvents() {
        if (sink instanceof MemoryAuditSink memoryAuditSink) {
            memoryAuditSink.clear();
        }
    }

    /**
     * Wait until all previously submitted async records are persisted.
     */
    public void flush() {
        if (!async) {
            return;
        }
        try {
            Future<?> marker = executor.submit(() -> {
            });
            marker.get();
        } catch (Exception error) {
            LOG.errorf(error, "Failed to flush audit logger");
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        flush();
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private void record(AuditEvent event) {
        if (async) {
            executor.submit(() -> safeRecord(event));
        } else {
            safeRecord(event);
        }
    }

    private void safeRecord(AuditEvent event) {
        try {
            sink.record(event);
        } catch (RuntimeException error) {
            LOG.errorf("Failed to record audit event: %s (%s)", event, error.getMessage());
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    private static long positiveDuration(long durationMs) {
        return Math.max(0L, durationMs);
    }

    private static Map<String, Object> auditAttributes(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null && !String.valueOf(key).isBlank()) {
                attributes.put(String.valueOf(key).trim(), snapshotValue(value));
            }
        }
        return attributes.isEmpty() ? Map.of() : Map.copyOf(attributes);
    }

    private static Map<String, Object> copyAttributes(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null && !key.isBlank()) {
                copied.put(key.trim(), snapshotValue(value));
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }

    private static Object snapshotValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copied = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (key != null && item != null && !String.valueOf(key).isBlank()) {
                    copied.put(String.valueOf(key).trim(), snapshotValue(item));
                }
            });
            return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
        }
        if (value instanceof List<?> list) {
            return List.copyOf(list.stream()
                    .filter(Objects::nonNull)
                    .map(AgentAuditLogger::snapshotValue)
                    .toList());
        }
        return value;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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

        public AuditEvent {
            timestamp = timestamp == null ? Instant.now() : timestamp;
            eventType = normalize(eventType, "unknown");
            component = normalize(component, "unknown");
            attributes = copyAttributes(attributes);
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
            this.logFile = Objects.requireNonNull(logFile, "logFile");
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
