package tech.kayys.gamelan.tool;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.governance.AuditLog;
import tech.kayys.gamelan.hitl.HumanInTheLoop;

import java.util.*;

/**
 * Dispatches tool calls to their registered {@link ToolHandler} implementations.
 *
 * <h2>Section XI — HITL integration</h2>
 * Before executing any tool with a registered approval gate,
 * {@link HumanInTheLoop#requestApproval} is called. If the user denies,
 * a {@code ToolResult.failure} is returned without executing the tool.
 * The decision is recorded in the {@link AuditLog}.
 *
 * <h2>Section V — Audit integration</h2>
 * Every tool call and result is logged to the cryptographic audit trail.
 *
 * <h2>Routing</h2>
 * All CDI beans implementing {@link ToolHandler} are auto-discovered via
 * {@code @Any Instance<ToolHandler>} and registered under every name from
 * {@link ToolHandler#toolNames()}. The routing table is immutable after
 * {@link PostConstruct}.
 */
@ApplicationScoped
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    @Inject @Any Instance<ToolHandler> handlers;
    @Inject HumanInTheLoop             hitl;
    @Inject AuditLog                   auditLog;

    private Map<String, ToolHandler> routing;

    @PostConstruct
    void init() {
        Map<String, ToolHandler> table = new LinkedHashMap<>();
        for (ToolHandler h : handlers) {
            for (String name : h.toolNames()) {
                if (table.containsKey(name)) {
                    log.warn("Duplicate tool '{}': {} wins over {}",
                            name, table.get(name).getClass().getSimpleName(),
                            h.getClass().getSimpleName());
                } else {
                    table.put(name, h);
                    log.debug("Registered tool: {} → {}", name, h.getClass().getSimpleName());
                }
            }
        }
        routing = Collections.unmodifiableMap(table);
        log.info("ToolExecutor: {} tools ({})",
                routing.size(), String.join(", ", routing.keySet()));
    }

    /**
     * Executes a tool call with HITL gate check and full audit logging.
     *
     * <p>Never throws — all exceptions are wrapped into {@link ToolResult#failure}.
     */
    public ToolResult execute(ToolCall call) {
        return execute(call, "unknown-session");
    }

    /**
     * Executes a tool call with a known session ID for audit correlation.
     */
    public ToolResult execute(ToolCall call, String sessionId) {
        if (call == null) return ToolResult.failure("unknown", "Null tool call");

        ToolHandler handler = routing.get(call.name());
        if (handler == null) {
            return ToolResult.failure(call.name(),
                    "Unknown tool '" + call.name() + "'. Available: "
                    + String.join(", ", routing.keySet().stream().sorted().toList()));
        }

        // ── Section XI: HITL gate check ───────────────────────────────────
        if (hitl.hasGate(call.name())) {
            String description = describeCall(call);
            boolean approved = hitl.requestApproval(sessionId, call.name(), description);
            if (!approved) {
                log.info("[hitl] tool '{}' denied by user", call.name());
                return ToolResult.failure(call.name(),
                        "Execution denied by human-in-the-loop gate.");
            }
        }

        // ── Section V: Audit tool invocation ──────────────────────────────
        Map<String, Object> auditParams = new LinkedHashMap<>();
        call.parameters().entrySet().stream()
                .limit(5) // cap audit size
                .forEach(e -> auditParams.put(e.getKey(), truncate(e.getValue(), 200)));
        auditLog.logToolCall(sessionId, call.name(), auditParams);

        // ── Execute ───────────────────────────────────────────────────────
        long start = System.currentTimeMillis();
        try {
            ToolResult result = handler.execute(call);
            long ms = System.currentTimeMillis() - start;

            // Audit the result
            auditLog.logToolResult(sessionId, call.name(), result.isSuccess(),
                    truncate(result.output(), 300));

            log.debug("Tool '{}' → exit={} chars={} ms={}",
                    call.name(), result.exitCode(), result.output().length(), ms);
            return result;
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            auditLog.logToolResult(sessionId, call.name(), false,
                    "Exception: " + e.getMessage());
            log.error("Tool '{}' threw: {}", call.name(), e.getMessage(), e);
            return ToolResult.failure(call.name(),
                    "Internal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Returns all registered tool names (sorted). */
    public List<String> toolNames() {
        return routing.keySet().stream().sorted().toList();
    }

    private String describeCall(ToolCall call) {
        if (call.parameters().isEmpty()) return "Tool: " + call.name();
        String params = call.parameters().entrySet().stream()
                .limit(3)
                .map(e -> e.getKey() + "=" + truncate(e.getValue(), 60))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
        return call.name() + "(" + params + ")";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
