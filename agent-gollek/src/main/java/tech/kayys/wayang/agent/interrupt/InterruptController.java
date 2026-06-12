package tech.kayys.gamelan.agent.interrupt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/**
 * InterruptController — cooperative cancellation with per-query interrupt tokens.
 *
 * <h2>From the OPENDEV paper (§2.2.3 — Cross-cutting concerns)</h2>
 * Interrupt tokens propagate cancellation requests from the UI to the agent thread, polled
 * at six phase boundaries within each iteration:
 * <ol>
 *   <li>Before thinking</li>
 *   <li>After thinking</li>
 *   <li>Before action</li>
 *   <li>During tool execution</li>
 *   <li>At iteration boundaries</li>
 *   <li>Between tool calls in a batch</li>
 * </ol>
 *
 * <h2>Three race conditions addressed (from paper §2.2.3)</h2>
 * <ol>
 *   <li><b>Modal priority</b>: ask-user dialogs and plan approval take priority over agent
 *       interrupts. When a modal is active, Ctrl+C cancels the dialog — not the agent thread.
 *       This prevents orphaned UI state (dangling spinners, stale futures).</li>
 *   <li><b>Process group killing</b>: subprocess creation uses process groups
 *       ({@code start_new_session=True}) so that os.killpg reliably terminates child processes.
 *       Replicated here as a process-group kill strategy for {@code run_command}.</li>
 *   <li><b>One-shot guard</b>: prevents duplicate interrupt messages from rapid Ctrl+C presses.
 *       Only the first interrupt within a debounce window is forwarded.</li>
 * </ol>
 *
 * <h2>Thread injection queue</h2>
 * A thread-safe injection queue allows users to send follow-up messages while the agent is
 * mid-execution. Messages are drained at iteration boundaries and checked before completion,
 * ensuring no user input is silently dropped (paper §2.2.3).
 */
@ApplicationScoped
public class InterruptController {

    private static final Logger log = LoggerFactory.getLogger(InterruptController.class);

    private static final long   DEBOUNCE_MS   = 300; // one-shot guard window
    private static final int    QUEUE_CAPACITY = 10;  // paper: maxsize=10

    @Inject AgentTelemetry telemetry;

    // Per-query interrupt token (replaced on each new query)
    private final AtomicBoolean    cancelled       = new AtomicBoolean(false);
    private final AtomicLong       cancelledAt     = new AtomicLong(0);
    // Modal guard: when a modal dialog is active, UI Ctrl+C goes to modal, not agent
    private final AtomicBoolean    modalActive     = new AtomicBoolean(false);
    private volatile String        modalKind       = "";
    // One-shot guard: debounce rapid Ctrl+C presses
    private final AtomicLong       lastInterruptMs = new AtomicLong(0);
    // Message injection queue (paper: bounded at 10)
    private final LinkedBlockingQueue<InjectedMessage> injectionQueue =
            new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    // Registered cancel listeners (e.g., to kill in-flight HTTP requests)
    private final List<Runnable> cancelListeners = new CopyOnWriteArrayList<>();

    // ── Interrupt API ──────────────────────────────────────────────────────

    /**
     * Signals an interrupt request (e.g., user pressed Ctrl+C).
     * Respects modal priority and one-shot guard.
     *
     * @return the action taken
     */
    public InterruptAction signal() {
        long now = System.currentTimeMillis();

        // One-shot guard: ignore rapid repeated presses
        long last = lastInterruptMs.get();
        if (now - last < DEBOUNCE_MS) {
            log.debug("[interrupt] debounced ({}ms since last)", now - last);
            telemetry.count("interrupt.debounced");
            return InterruptAction.DEBOUNCED;
        }
        lastInterruptMs.set(now);

        // Modal priority: if a modal is active, cancel the modal not the agent
        if (modalActive.get()) {
            log.info("[interrupt] modal '{}' active — cancelling dialog, not agent", modalKind);
            telemetry.count("interrupt.modal_dismissed");
            modalActive.set(false);
            modalKind = "";
            return InterruptAction.MODAL_DISMISSED;
        }

        // Cancel the agent
        boolean wasRunning = !cancelled.getAndSet(true);
        if (wasRunning) {
            cancelledAt.set(now);
            telemetry.count("interrupt.agent_cancelled");
            log.info("[interrupt] agent cancelled at {}", Instant.now());
            // Notify all listeners (e.g., kill in-flight HTTP requests, subagents)
            cancelListeners.forEach(listener -> {
                try { listener.run(); } catch (Exception e) {
                    log.debug("[interrupt] listener error: {}", e.getMessage());
                }
            });
            return InterruptAction.AGENT_CANCELLED;
        }
        return InterruptAction.ALREADY_CANCELLED;
    }

    /**
     * Checks whether cancellation has been requested.
     * Call at each of the six phase boundaries.
     */
    public boolean isCancelled() { return cancelled.get(); }

    /**
     * Checks cancellation and throws if cancelled.
     * Convenience method for phase boundaries that want exception-based flow.
     */
    public void checkCancelled() throws CancelledException {
        if (cancelled.get()) throw new CancelledException("Agent cancelled by user");
    }

    /**
     * Resets the interrupt token for a new query.
     * Call at the start of each user query processing.
     */
    public void reset() {
        cancelled.set(false);
        cancelledAt.set(0);
        injectionQueue.clear();
        log.debug("[interrupt] reset for new query");
    }

    // ── Modal management ───────────────────────────────────────────────────

    /**
     * Marks a modal dialog as active. While active, Ctrl+C dismisses the dialog
     * instead of cancelling the agent thread.
     *
     * @param kind  human-readable modal type (e.g., "ask_user", "plan_approval")
     */
    public void enterModal(String kind) {
        modalActive.set(true);
        modalKind = kind;
        log.debug("[interrupt] modal entered: {}", kind);
    }

    /**
     * Marks the modal as dismissed/completed.
     */
    public void exitModal() {
        modalActive.set(false);
        modalKind = "";
        log.debug("[interrupt] modal exited");
    }

    public boolean isModalActive() { return modalActive.get(); }
    public String  activeModalKind() { return modalKind; }

    // ── Message injection queue ────────────────────────────────────────────

    /**
     * Injects a follow-up message into the agent's execution while it is mid-iteration.
     * The agent drains these at iteration boundaries (paper §2.2.3).
     *
     * @param content  the message content
     * @param priority NORMAL or HIGH (HIGH messages are prepended conceptually)
     * @return true if accepted, false if queue is full
     */
    public boolean inject(String content, MessagePriority priority) {
        boolean accepted = injectionQueue.offer(
                new InjectedMessage(content, priority, Instant.now()));
        if (accepted) {
            telemetry.count("interrupt.message.injected");
            log.debug("[interrupt] message injected (queue size={})", injectionQueue.size());
        } else {
            telemetry.count("interrupt.message.queue_full");
            log.warn("[interrupt] injection queue full — message dropped");
        }
        return accepted;
    }

    /**
     * Drains all pending injected messages. Call at iteration boundaries.
     *
     * @return list of pending messages, sorted HIGH priority first
     */
    public List<InjectedMessage> drainInjected() {
        List<InjectedMessage> batch = new ArrayList<>();
        injectionQueue.drainTo(batch);
        // Sort HIGH priority to front
        batch.sort(Comparator.comparing(m -> m.priority() == MessagePriority.HIGH ? 0 : 1));
        if (!batch.isEmpty()) {
            log.debug("[interrupt] drained {} injected messages", batch.size());
            telemetry.count("interrupt.message.drained");
        }
        return batch;
    }

    /** Returns true if there are pending injected messages. */
    public boolean hasPendingMessages() { return !injectionQueue.isEmpty(); }

    // ── Cancel listeners ───────────────────────────────────────────────────

    /**
     * Registers a listener to be called when the agent is cancelled.
     * Use to kill in-flight HTTP requests, subagent threads, background processes.
     */
    public void addCancelListener(Runnable listener) {
        cancelListeners.add(listener);
    }

    public void removeCancelListener(Runnable listener) {
        cancelListeners.remove(listener);
    }

    /**
     * Executes a supplier and returns the result, or empty if cancelled before completion.
     * Useful for wrapping LLM API calls with cancellation support.
     */
    public <T> Optional<T> withCancellation(Supplier<T> work) {
        if (isCancelled()) return Optional.empty();
        try {
            T result = work.get();
            return isCancelled() ? Optional.empty() : Optional.ofNullable(result);
        } catch (CancelledException e) {
            return Optional.empty();
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum InterruptAction {
        AGENT_CANCELLED, MODAL_DISMISSED, DEBOUNCED, ALREADY_CANCELLED
    }

    public enum MessagePriority { NORMAL, HIGH }

    public record InjectedMessage(
            String          content,
            MessagePriority priority,
            Instant         timestamp
    ) {}

    public static final class CancelledException extends RuntimeException {
        public CancelledException(String msg) { super(msg); }
    }
}
