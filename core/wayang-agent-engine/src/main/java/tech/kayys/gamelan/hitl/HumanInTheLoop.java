package tech.kayys.gamelan.hitl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.governance.AuditLog;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Human-in-the-Loop (HITL) control plane (Section XI).
 *
 * <h2>Why this is critical for production</h2>
 * Autonomous agents that can modify files, run shell commands, and call APIs
 * MUST have approval gates for high-risk operations. Without HITL:
 * <ul>
 *   <li>A misunderstood task can delete files or corrupt data</li>
 *   <li>Prompt injection can trick the agent into harmful actions</li>
 *   <li>Financial or infrastructure mutations can happen without human sign-off</li>
 * </ul>
 *
 * <h2>Gate types</h2>
 * <ul>
 *   <li>{@link GateType#ALWAYS} — always require approval (financial mutations, system ops)</li>
 *   <li>{@link GateType#HIGH_RISK} — require approval only for destructive ops (delete, overwrite)</li>
 *   <li>{@link GateType#OPTIONAL} — ask but allow bypass with --auto-approve</li>
 *   <li>{@link GateType#DISABLED} — no gate (default for safe read-only ops)</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * Called by {@link tech.kayys.gamelan.tool.ToolExecutor} before executing
 * any tool that has a registered gate. The gate blocks the current thread
 * via stdin prompt and times out after {@code APPROVAL_TIMEOUT_S} seconds
 * (defaults to 60 — the human has one minute to decide).
 *
 * <h2>Auto-approve mode</h2>
 * When {@code --auto-approve} is set (CI/CD environments), all gates are
 * bypassed with a logged decision. Gates of type {@code ALWAYS} still fire
 * but log a warning rather than blocking.
 */
@ApplicationScoped
public class HumanInTheLoop {

    private static final Logger log = LoggerFactory.getLogger(HumanInTheLoop.class);
    private static final int APPROVAL_TIMEOUT_S = 60;

    @Inject AuditLog auditLog;

    /** Whether to bypass all gates (CI mode). */
    private volatile boolean autoApprove = false;

    /** Registered gates keyed by tool name. */
    private final Map<String, GateType> toolGates = new HashMap<>(Map.of(
            "run_command",  GateType.HIGH_RISK,
            "write_file",   GateType.HIGH_RISK,
            "apply_patch",  GateType.HIGH_RISK,
            "http_post",    GateType.HIGH_RISK,
            "http_put",     GateType.HIGH_RISK,
            "http_delete",  GateType.HIGH_RISK
    ));

    // ── Public API ─────────────────────────────────────────────────────────

    /** Enable auto-approve mode (for CI or when user sets --auto-approve). */
    public void setAutoApprove(boolean value) {
        autoApprove = value;
        if (value) log.warn("[hitl] AUTO-APPROVE MODE: all gates bypassed");
    }

    public boolean isAutoApprove() { return autoApprove; }

    /**
     * Registers a gate for a tool name.
     *
     * @param toolName the tool to gate
     * @param gate     the gate type
     */
    public void register(String toolName, GateType gate) {
        toolGates.put(toolName, gate);
    }

    /**
     * Checks whether execution of the given tool with given params requires
     * human approval, and if so, blocks until approved or denied.
     *
     * @param sessionId   current session ID (for audit)
     * @param toolName    tool about to be invoked
     * @param description human-readable description of what will happen
     * @return true if execution should proceed, false if denied
     */
    public boolean requestApproval(String sessionId, String toolName, String description) {
        GateType gate = toolGates.getOrDefault(toolName, GateType.DISABLED);

        if (gate == GateType.DISABLED) return true;

        if (autoApprove) {
            if (gate == GateType.ALWAYS) {
                log.warn("[hitl] AUTO-APPROVE bypassing ALWAYS gate for: {}", toolName);
            }
            auditLog.logHumanDecision(sessionId, toolName, true, "auto-approved");
            return true;
        }

        // Print the approval request to the user
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ⚠  APPROVAL REQUIRED                                               │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.printf ("│  Tool: %-63s│%n", toolName);
        // Word-wrap description
        String[] words = description.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > 63) {
                System.out.printf("│  %-63s│%n", line.toString().strip());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) {
            System.out.printf("│  %-63s│%n", line.toString().strip());
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");
        System.out.print("  Approve? [y/N/skip] (60s timeout): ");
        System.out.flush();

        // Wait for input with timeout
        try {
            CompletableFuture<String> inputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    Scanner sc = new Scanner(System.in);
                    return sc.hasNextLine() ? sc.nextLine().strip().toLowerCase() : "n";
                } catch (Exception e) { return "n"; }
            });

            String answer = inputFuture.get(APPROVAL_TIMEOUT_S, TimeUnit.SECONDS);
            boolean approved = answer.equals("y") || answer.equals("yes");
            boolean skipped  = answer.equals("skip") || answer.equals("s");

            System.out.println(approved ? "  ✓ Approved" : (skipped ? "  → Skipped" : "  ✗ Denied"));

            auditLog.logHumanDecision(sessionId, toolName, approved || skipped,
                    skipped ? "user skipped" : (approved ? "user approved" : "user denied"));

            return approved || skipped;
        } catch (TimeoutException e) {
            System.out.println("\n  ✗ Timed out — denied by default");
            auditLog.logHumanDecision(sessionId, toolName, false, "timed out after " + APPROVAL_TIMEOUT_S + "s");
            return false;
        } catch (Exception e) {
            log.warn("[hitl] approval error: {} — denying", e.getMessage());
            return false;
        }
    }

    /** Checks if a tool has an active gate without blocking. */
    public boolean hasGate(String toolName) {
        return toolGates.getOrDefault(toolName, GateType.DISABLED) != GateType.DISABLED;
    }

    /** Returns all registered gates (for display in /help or config). */
    public Map<String, GateType> registeredGates() {
        return Collections.unmodifiableMap(toolGates);
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public enum GateType {
        /** Always require human approval, even in auto-approve mode (with warning). */
        ALWAYS,
        /** Require approval for destructive or state-changing operations. */
        HIGH_RISK,
        /** Optional gate — ask but allow bypass. */
        OPTIONAL,
        /** No gate. */
        DISABLED
    }
}
