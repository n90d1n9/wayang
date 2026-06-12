package tech.kayys.gamelan.control;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.agent.orchestration.AgentEventListener;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Human-in-the-Loop (HITL) Control Plane.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>Interruptibility</b>: Pause, modify, or resume agent execution at any point</li>
 *   <li><b>Approval Gates</b>: Require human confirmation for high-risk operations</li>
 *   <li><b>Plan Injection</b>: Human can modify the agent's planned next step</li>
 *   <li><b>Explainability</b>: "Why did you choose this?" interface</li>
 *   <li><b>Override</b>: Human can override any agent decision</li>
 * </ul>
 *
 * <h2>Approval Gate Configuration</h2>
 * Gates can be configured at three levels:
 * <ul>
 *   <li>TOOL level: specific tools always require approval (e.g., run_command)</li>
 *   <li>CONTENT level: approval when output matches patterns (e.g., "rm -rf", "DROP TABLE")</li>
 *   <li>COST level: approval when estimated cost exceeds budget threshold</li>
 * </ul>
 *
 * <h2>Non-blocking Design</h2>
 * The control plane uses a CompletableFuture-based design so the agent loop
 * is suspended (not killed) during approval waits. The human can:
 * <ol>
 *   <li>APPROVE: continue as planned</li>
 *   <li>MODIFY: change the proposed action before execution</li>
 *   <li>REJECT: skip the action (agent sees an error result)</li>
 *   <li>ABORT: terminate the entire run</li>
 * </ol>
 */
@ApplicationScoped
public class AgentControlPlane {

    private static final Logger log = LoggerFactory.getLogger(AgentControlPlane.class);

    private static final long DEFAULT_APPROVAL_TIMEOUT_S = 300; // 5 minutes

    // ── Gate registry ──────────────────────────────────────────────────────

    private final List<ApprovalGate>    gates           = new CopyOnWriteArrayList<>();
    private final Map<String, ApprovalDecision> pending = new ConcurrentHashMap<>();

    // Control signals
    private volatile boolean pauseRequested  = false;
    private volatile boolean abortRequested  = false;
    private final    Object  pauseLock       = new Object();

    // Callbacks for UI/CLI integration
    private Consumer<ApprovalRequest> approvalHandler = this::defaultApprovalHandler;
    private Consumer<String>          pauseHandler    = msg -> log.info("[hitl] {}", msg);

    // ── Gate configuration ─────────────────────────────────────────────────

    /** Register an approval gate that triggers on specific tool names. */
    public void requireApprovalForTools(String... toolNames) {
        gates.add(new ApprovalGate(
                ApprovalGate.GateType.TOOL,
                Set.of(toolNames),
                List.of(),
                0));
        log.info("[hitl] approval gate registered for tools: {}", Arrays.toString(toolNames));
    }

    /** Register a content-pattern gate (regex match on proposed action). */
    public void requireApprovalForPattern(String... patterns) {
        gates.add(new ApprovalGate(
                ApprovalGate.GateType.CONTENT,
                Set.of(),
                List.of(patterns),
                0));
    }

    /** Register a cost gate (triggers when estimated tokens exceed threshold). */
    public void requireApprovalForCost(int tokenThreshold) {
        gates.add(new ApprovalGate(
                ApprovalGate.GateType.COST,
                Set.of(), List.of(), tokenThreshold));
    }

    /** Remove all gates (disable HITL). */
    public void clearGates() { gates.clear(); }

    /** Set a custom approval handler (e.g., for UI integration). */
    public void setApprovalHandler(Consumer<ApprovalRequest> handler) {
        this.approvalHandler = handler;
    }

    // ── Control signals ────────────────────────────────────────────────────

    /**
     * Requests that the agent pause after the current step completes.
     * The agent will block until {@link #resume()} is called.
     */
    public void pause() {
        pauseRequested = true;
        pauseHandler.accept("Pause requested — agent will pause after current step");
        log.info("[hitl] pause requested");
    }

    /**
     * Resumes a paused agent.
     */
    public synchronized void resume() {
        pauseRequested = false;
        synchronized (pauseLock) { pauseLock.notifyAll(); }
        log.info("[hitl] resumed");
    }

    /**
     * Requests immediate abort. The next tool call check will see this
     * and terminate the run cleanly.
     */
    public void abort() {
        abortRequested = true;
        resume(); // unblock any paused state
        log.warn("[hitl] ABORT requested");
    }

    public boolean isAbortRequested() { return abortRequested; }
    public boolean isPaused()         { return pauseRequested; }

    // ── Gate evaluation ────────────────────────────────────────────────────

    /**
     * Evaluates a proposed tool call against all configured gates.
     * Blocks if approval is required.
     *
     * @param call          the proposed tool call
     * @param context       human-readable context ("why this tool is being called")
     * @param estimatedCost estimated token cost of this action
     * @return the decision (APPROVE / MODIFY / REJECT / ABORT)
     */
    public GateDecision evaluate(ToolCall call, String context, int estimatedCost) {
        // Check abort first
        if (abortRequested) return GateDecision.abort("User requested abort");

        // Wait if paused
        if (pauseRequested) {
            waitForResume();
            if (abortRequested) return GateDecision.abort("Aborted during pause");
        }

        // Evaluate gates
        for (ApprovalGate gate : gates) {
            if (gate.triggers(call, estimatedCost)) {
                log.info("[hitl] gate triggered for tool '{}' by gate type {}",
                        call.name(), gate.type());
                return requestApproval(call, context, gate);
            }
        }

        return GateDecision.approve();
    }

    /**
     * Injects a modified plan step. The agent will execute the injected
     * action instead of its original plan.
     */
    public Optional<String> getInjectedStep(int iteration) {
        ApprovalDecision decision = pending.get("inject-" + iteration);
        if (decision != null && decision.type() == ApprovalDecision.Type.MODIFY) {
            return Optional.ofNullable(decision.modifiedAction());
        }
        return Optional.empty();
    }

    /**
     * Submits an approval decision for a pending request.
     */
    public void submitDecision(String requestId, ApprovalDecision decision) {
        pending.put(requestId, decision);
        log.info("[hitl] decision submitted for {}: {}", requestId, decision.type());
    }

    // ── Explainability ─────────────────────────────────────────────────────

    /**
     * Generates an explanation for a tool selection decision.
     */
    public String explain(String toolName, String taskContext, String reasoning) {
        return String.format("""
                [Gamelan Decision Explanation]
                Tool selected: %s
                Task context:  %s
                Reasoning:     %s
                Timestamp:     %s
                """, toolName, taskContext, reasoning, Instant.now());
    }

    // ── Approval flow ──────────────────────────────────────────────────────

    private GateDecision requestApproval(ToolCall call, String context, ApprovalGate gate) {
        String requestId = UUID.randomUUID().toString();
        ApprovalRequest req = new ApprovalRequest(
                requestId, call, context, gate.type(), Instant.now());

        // Trigger the handler (CLI, UI, webhook, etc.)
        approvalHandler.accept(req);

        // Wait for decision with timeout
        long timeoutMs = DEFAULT_APPROVAL_TIMEOUT_S * 1000;
        long deadline  = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            ApprovalDecision decision = pending.get(requestId);
            if (decision != null) {
                pending.remove(requestId);
                return switch (decision.type()) {
                    case APPROVE -> GateDecision.approve();
                    case MODIFY  -> GateDecision.modify(decision.modifiedAction());
                    case REJECT  -> GateDecision.reject("Human rejected action");
                    case ABORT   -> GateDecision.abort("Human aborted run");
                };
            }
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return GateDecision.abort("Interrupted during approval wait");
            }
        }

        log.warn("[hitl] approval timeout for {}, defaulting to REJECT", requestId);
        return GateDecision.reject("Approval timeout — action rejected for safety");
    }

    private void waitForResume() {
        synchronized (pauseLock) {
            while (pauseRequested && !abortRequested) {
                try { pauseLock.wait(1000); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /** CLI-based approval handler (blocks on System.in). */
    private void defaultApprovalHandler(ApprovalRequest req) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║  ⚠  APPROVAL REQUIRED                            ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("  Tool:    %s%n", req.call().name());
        System.out.printf("  Params:  %s%n", req.call().parameters());
        System.out.printf("  Context: %s%n", req.context());
        System.out.println();
        System.out.print("  [A]pprove / [R]eject / [M]odify / [Q]uit: ");
        System.out.flush();

        try (Scanner sc = new Scanner(System.in)) {
            String input = sc.nextLine().trim().toLowerCase();
            ApprovalDecision decision = switch (input) {
                case "a", "approve" -> new ApprovalDecision(ApprovalDecision.Type.APPROVE, null);
                case "r", "reject"  -> new ApprovalDecision(ApprovalDecision.Type.REJECT, null);
                case "q", "quit"    -> new ApprovalDecision(ApprovalDecision.Type.ABORT, null);
                default             -> new ApprovalDecision(ApprovalDecision.Type.APPROVE, null);
            };
            pending.put(req.requestId(), decision);
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ApprovalGate(
            GateType     type,
            Set<String>  toolNames,
            List<String> contentPatterns,
            int          costThreshold
    ) {
        public enum GateType { TOOL, CONTENT, COST }

        boolean triggers(ToolCall call, int cost) {
            return switch (type) {
                case TOOL    -> toolNames.contains(call.name());
                case COST    -> cost >= costThreshold;
                case CONTENT -> {
                    String params = call.parameters().toString().toLowerCase();
                    yield contentPatterns.stream()
                            .anyMatch(p -> params.matches(".*" + p.toLowerCase() + ".*"));
                }
            };
        }
    }

    public record ApprovalRequest(
            String   requestId,
            ToolCall call,
            String   context,
            ApprovalGate.GateType gateType,
            Instant  requestedAt
    ) {}

    public record ApprovalDecision(Type type, String modifiedAction) {
        public enum Type { APPROVE, MODIFY, REJECT, ABORT }
    }

    public record GateDecision(Action action, String reason, String modifiedAction) {
        public enum Action { APPROVE, MODIFY, REJECT, ABORT }

        static GateDecision approve()          { return new GateDecision(Action.APPROVE, null, null); }
        static GateDecision reject(String r)   { return new GateDecision(Action.REJECT,  r, null); }
        static GateDecision abort(String r)    { return new GateDecision(Action.ABORT,   r, null); }
        static GateDecision modify(String a)   { return new GateDecision(Action.MODIFY,  null, a); }

        public boolean isApproved()  { return action == Action.APPROVE || action == Action.MODIFY; }
        public boolean isTerminal()  { return action == Action.ABORT; }
    }
}
