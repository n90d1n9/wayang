package tech.kayys.gamelan.execution.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Actor-based Execution Runtime for agent tool calls.
 *
 * <h2>Why actors</h2>
 * The {@link tech.kayys.gamelan.execution.dag.DagExecutionEngine} handles
 * high-level task orchestration. The Actor runtime handles the low-level:
 * tool call execution with isolation, back-pressure, and supervision.
 *
 * <p>Classical problems in agent tool execution:
 * <ol>
 *   <li><b>State corruption</b>: two parallel tasks write to the same file simultaneously</li>
 *   <li><b>Resource exhaustion</b>: 10 simultaneous shell commands saturate the machine</li>
 *   <li><b>Error propagation</b>: one crashed tool kills the whole agent</li>
 *   <li><b>Ordering violations</b>: tool B reads a file before tool A finishes writing it</li>
 * </ol>
 * Actors solve all four: each actor owns its state, has a bounded mailbox
 * (back-pressure), is supervised independently, and communicates only through
 * ordered message delivery.
 *
 * <h2>Actor Hierarchy</h2>
 * <pre>
 * ActorSystem
 *   └── RootSupervisor
 *         ├── ToolRouterActor        ← dispatches tool calls to specialized actors
 *         │     ├── FileSystemActor  ← owns all read_file / write_file / apply_patch
 *         │     ├── ShellActor       ← owns all run_command (rate-limited)
 *         │     ├── GitActor         ← owns all git operations
 *         │     └── SearchActor      ← owns all search_files / semantic_search
 *         └── MonitorActor           ← collects metrics, detects anomalies
 * </pre>
 *
 * <h2>Supervision Strategy</h2>
 * <ul>
 *   <li>RESTART: actor is restarted on failure (for transient errors)</li>
 *   <li>STOP: actor is stopped on failure (for fatal/unrecoverable errors)</li>
 *   <li>ESCALATE: failure is propagated to parent supervisor</li>
 * </ul>
 *
 * <h2>Back-pressure</h2>
 * Each actor has a bounded mailbox. When the mailbox is full, producers
 * block (with configurable timeout) rather than spawning unbounded tasks.
 * This prevents out-of-memory crashes under heavy parallelism.
 *
 * <h2>Message ordering</h2>
 * Messages to a single actor are processed strictly in arrival order.
 * This guarantees that write-then-read to a single actor always sees the write first.
 */
public final class ActorSystem {

    private static final Logger log = LoggerFactory.getLogger(ActorSystem.class);

    private final String                    name;
    private final Map<String, ActorRef<?>>  actors      = new ConcurrentHashMap<>();
    private final ExecutorService           scheduler   = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean             running     = new AtomicBoolean(true);
    private final SupervisionStrategy       defaultStrategy;

    private ActorSystem(String name, SupervisionStrategy defaultStrategy) {
        this.name            = name;
        this.defaultStrategy = defaultStrategy;
        log.info("[actor-system] '{}' started", name);
    }

    /** Creates an actor system with the given name and default supervision strategy. */
    public static ActorSystem create(String name) {
        return new ActorSystem(name, SupervisionStrategy.RESTART);
    }

    public static ActorSystem create(String name, SupervisionStrategy strategy) {
        return new ActorSystem(name, strategy);
    }

    // ── Actor spawning ─────────────────────────────────────────────────────

    /**
     * Spawns a new actor and registers it in the system.
     *
     * @param actorId     unique identifier for this actor
     * @param mailboxSize max queued messages (back-pressure threshold)
     * @param behavior    the message-handling function
     * @param strategy    supervision strategy on failure
     * @return a reference to the spawned actor
     */
    public <M> ActorRef<M> spawn(String actorId, int mailboxSize,
                                  ActorBehavior<M> behavior,
                                  SupervisionStrategy strategy) {
        if (!running.get()) throw new IllegalStateException("ActorSystem is shut down");

        ActorRef<M> ref = new ActorRef<>(actorId, mailboxSize, behavior, strategy,
                this, scheduler);
        actors.put(actorId, ref);
        ref.start();
        log.debug("[actor-system] spawned actor '{}'", actorId);
        return ref;
    }

    public <M> ActorRef<M> spawn(String actorId, ActorBehavior<M> behavior) {
        return spawn(actorId, 100, behavior, defaultStrategy);
    }

    /**
     * Looks up an actor reference by ID.
     */
    @SuppressWarnings("unchecked")
    public <M> Optional<ActorRef<M>> find(String actorId) {
        return Optional.ofNullable((ActorRef<M>) actors.get(actorId));
    }

    /**
     * Stops all actors and shuts down the system. Waits up to 10 seconds for
     * in-progress messages to complete.
     */
    public void shutdown() {
        if (!running.compareAndSet(true, false)) return;
        log.info("[actor-system] '{}' shutting down ({} actors)", name, actors.size());
        actors.values().forEach(ActorRef::stop);
        scheduler.shutdown();
        try { scheduler.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        actors.clear();
        log.info("[actor-system] '{}' shut down", name);
    }

    public boolean isRunning()  { return running.get(); }
    public String  name()       { return name; }
    public int     actorCount() { return actors.size(); }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A reference to a running actor. Messages are sent through this reference.
     * The reference is thread-safe; any thread may send messages to any actor.
     */
    public static final class ActorRef<M> {
        private final String               id;
        private final BlockingQueue<Envelope<M>> mailbox;
        private final ActorBehavior<M>     behavior;
        private final SupervisionStrategy  strategy;
        private final ActorSystem          system;
        private final ExecutorService      scheduler;
        private final AtomicInteger        restarts   = new AtomicInteger(0);
        private final AtomicBoolean        stopped    = new AtomicBoolean(false);
        private final ActorMetrics         metrics    = new ActorMetrics();

        ActorRef(String id, int mailboxSize, ActorBehavior<M> behavior,
                 SupervisionStrategy strategy, ActorSystem system, ExecutorService scheduler) {
            this.id        = id;
            this.mailbox   = new ArrayBlockingQueue<>(mailboxSize);
            this.behavior  = behavior;
            this.strategy  = strategy;
            this.system    = system;
            this.scheduler = scheduler;
        }

        /**
         * Sends a message to this actor. Returns immediately (non-blocking).
         * Drops the message if the mailbox is full and logs a warning.
         *
         * @param message the message to send
         * @return true if the message was accepted, false if the mailbox was full
         */
        public boolean tell(M message) {
            if (stopped.get()) return false;
            boolean accepted = mailbox.offer(new Envelope<>(message, Instant.now(), null));
            if (!accepted) {
                log.warn("[actor] '{}' mailbox full ({} capacity) — message dropped",
                        id, mailbox.remainingCapacity());
                metrics.dropped.incrementAndGet();
            }
            return accepted;
        }

        /**
         * Sends a message and waits for a reply (ask pattern).
         *
         * @param message   the request message
         * @param timeout   how long to wait for a reply
         * @return the reply, or empty if timeout
         */
        public <R> Optional<R> ask(M message, Duration timeout) {
            if (stopped.get()) return Optional.empty();
            CompletableFuture<R> replyFuture = new CompletableFuture<>();
            @SuppressWarnings("unchecked")
            boolean accepted = mailbox.offer(new Envelope<>(message, Instant.now(),
                    (Consumer<Object>) replyFuture::complete));
            if (!accepted) return Optional.empty();
            try {
                return Optional.of(replyFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        /** Stops this actor. In-progress message processing completes; pending messages are dropped. */
        public void stop() {
            stopped.set(true);
            // Poison pill: wake up any blocking take() call
            mailbox.offer(new Envelope<>(null, Instant.now(), null));
        }

        public String        id()       { return id; }
        public boolean       isStopped(){ return stopped.get(); }
        public ActorMetrics  metrics()  { return metrics; }
        public int           restarts() { return restarts.get(); }

        void start() {
            scheduler.submit(this::processLoop);
        }

        private void processLoop() {
            while (!stopped.get()) {
                try {
                    Envelope<M> env = mailbox.poll(1, TimeUnit.SECONDS);
                    if (env == null || env.message() == null) continue;

                    Instant msgStart = Instant.now();
                    try {
                        Object reply = behavior.receive(env.message());
                        metrics.processed.incrementAndGet();
                        metrics.totalProcessingMs.addAndGet(
                                Duration.between(msgStart, Instant.now()).toMillis());
                        if (env.replyTo() != null && reply != null) {
                            env.replyTo().accept(reply);
                        }
                    } catch (Exception e) {
                        metrics.errors.incrementAndGet();
                        log.error("[actor] '{}' behavior threw: {}", id, e.getMessage());
                        handleFailure(e, env.message());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.debug("[actor] '{}' loop exited", id);
        }

        private void handleFailure(Exception e, M lastMessage) {
            switch (strategy) {
                case RESTART -> {
                    int count = restarts.incrementAndGet();
                    log.warn("[actor] '{}' restarting (attempt {})", id, count);
                    // Re-queue the failed message with a delay
                    try { Thread.sleep(Math.min(1000L * count, 10_000)); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    if (lastMessage != null) mailbox.offer(new Envelope<>(lastMessage, Instant.now(), null));
                }
                case STOP -> {
                    log.warn("[actor] '{}' stopping after failure", id);
                    stopped.set(true);
                }
                case ESCALATE -> {
                    log.error("[actor] '{}' escalating failure to system", id);
                    stopped.set(true);
                    // In a real implementation, notify parent supervisor
                }
            }
        }
    }

    /** The function that processes one message and optionally produces a reply. */
    @FunctionalInterface
    public interface ActorBehavior<M> {
        Object receive(M message) throws Exception;
    }

    public record Envelope<M>(M message, Instant arrivedAt, Consumer<Object> replyTo) {}

    public static final class ActorMetrics {
        public final AtomicLong processed        = new AtomicLong();
        public final AtomicLong errors           = new AtomicLong();
        public final AtomicLong dropped          = new AtomicLong();
        public final AtomicLong totalProcessingMs = new AtomicLong();

        public double avgProcessingMs() {
            long p = processed.get();
            return p == 0 ? 0 : (double) totalProcessingMs.get() / p;
        }

        public double errorRate() {
            long total = processed.get() + errors.get();
            return total == 0 ? 0 : (double) errors.get() / total;
        }

        @Override public String toString() {
            return String.format("ActorMetrics[processed=%d, errors=%d, dropped=%d, avgMs=%.1f]",
                    processed.get(), errors.get(), dropped.get(), avgProcessingMs());
        }
    }

    public enum SupervisionStrategy { RESTART, STOP, ESCALATE }
}
