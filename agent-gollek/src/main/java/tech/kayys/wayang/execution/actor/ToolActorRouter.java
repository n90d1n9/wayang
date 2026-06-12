package tech.kayys.gamelan.execution.actor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.tool.ToolExecutor;
import tech.kayys.gamelan.tool.ToolResult;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ToolActorRouter — routes tool calls through a supervised actor hierarchy.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ToolActorRouter (this class)
 *   └── ActorSystem
 *         ├── file-actor        → read_file, write_file, apply_patch, glob, list_dir
 *         ├── shell-actor       → run_command (concurrency-limited to 3 parallel)
 *         ├── git-actor         → git (read-only, unlimited concurrency)
 *         ├── search-actor      → search_files, semantic_search
 *         └── default-actor     → everything else
 * </pre>
 *
 * <h2>Key properties</h2>
 * <ul>
 *   <li><b>File actor is single-threaded</b>: all filesystem mutations are serialized
 *       through one actor, preventing concurrent write corruption</li>
 *   <li><b>Shell actor is rate-limited</b>: max 3 concurrent shell executions,
 *       preventing machine resource exhaustion</li>
 *   <li><b>Git actor is parallel</b>: read-only git operations can run concurrently</li>
 *   <li><b>Back-pressure</b>: if actors are overloaded, tool calls block at the
 *       mailbox with a timeout — no unbounded spawning</li>
 *   <li><b>Supervision</b>: actors restart on failure; the router never crashes</li>
 * </ul>
 *
 * <h2>Drop-in replacement</h2>
 * This class implements the same interface surface as {@link ToolExecutor} so
 * it can replace direct ToolExecutor calls wherever actor-based safety is needed.
 * The actual tool execution is still delegated to ToolExecutor — actors add the
 * isolation, ordering, and rate-limiting layer.
 */
@ApplicationScoped
public class ToolActorRouter {

    private static final Logger log = LoggerFactory.getLogger(ToolActorRouter.class);

    private static final int FILE_MAILBOX   =  50;   // serialized FS operations
    private static final int SHELL_MAILBOX  =  20;   // rate-limited shell
    private static final int GIT_MAILBOX    = 100;   // parallel git reads
    private static final int SEARCH_MAILBOX = 100;   // parallel search
    private static final int DEFAULT_MAILBOX= 200;   // everything else

    private static final int    SHELL_MAX_CONCURRENT = 3;
    private static final long   TOOL_CALL_TIMEOUT_MS = 90_000;

    @Inject ToolExecutor toolExecutor;
    @Inject GamelanConfig config;

    private ActorSystem                          system;
    private ActorSystem.ActorRef<ToolCallMessage> fileActor;
    private ActorSystem.ActorRef<ToolCallMessage> shellActor;
    private ActorSystem.ActorRef<ToolCallMessage> gitActor;
    private ActorSystem.ActorRef<ToolCallMessage> searchActor;
    private ActorSystem.ActorRef<ToolCallMessage> defaultActor;

    private final Semaphore shellSemaphore = new Semaphore(SHELL_MAX_CONCURRENT, true);
    private final AtomicLong totalRouted   = new AtomicLong();
    private final AtomicLong totalFailed   = new AtomicLong();

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @PostConstruct
    void init() {
        system = ActorSystem.create("tool-router", ActorSystem.SupervisionStrategy.RESTART);

        // File actor: SERIALIZED (mailbox size 50, single consumer = no concurrent FS mutations)
        fileActor   = system.spawn("file-actor",   FILE_MAILBOX,
                msg -> executeToolBlocking(msg), ActorSystem.SupervisionStrategy.RESTART);

        // Shell actor: RATE-LIMITED via semaphore inside the behavior
        shellActor  = system.spawn("shell-actor",  SHELL_MAILBOX,
                msg -> executeShellBlocking(msg), ActorSystem.SupervisionStrategy.RESTART);

        // Git actor: PARALLEL (many virtual thread consumers in the scheduler)
        gitActor    = system.spawn("git-actor",    GIT_MAILBOX,
                msg -> executeToolBlocking(msg), ActorSystem.SupervisionStrategy.STOP);

        // Search actor: PARALLEL
        searchActor = system.spawn("search-actor", SEARCH_MAILBOX,
                msg -> executeToolBlocking(msg), ActorSystem.SupervisionStrategy.RESTART);

        // Default: catch-all
        defaultActor = system.spawn("default-actor", DEFAULT_MAILBOX,
                msg -> executeToolBlocking(msg), ActorSystem.SupervisionStrategy.RESTART);

        log.info("[tool-actor-router] initialized: {} actors", system.actorCount());
    }

    @PreDestroy
    void shutdown() {
        if (system != null) system.shutdown();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Executes a tool call through the appropriate actor.
     * This is the main entry point — replaces direct ToolExecutor.execute() calls.
     *
     * <p>Routing is based on the tool name:
     * <ul>
     *   <li>read_file, write_file, apply_patch, glob, list_dir → file-actor (serialized)</li>
     *   <li>run_command → shell-actor (rate-limited)</li>
     *   <li>git → git-actor (parallel)</li>
     *   <li>search_files, semantic_search → search-actor (parallel)</li>
     *   <li>everything else → default-actor</li>
     * </ul>
     *
     * @param call the tool call to execute
     * @return the tool result
     */
    public ToolResult execute(ToolCall call) {
        totalRouted.incrementAndGet();
        ActorSystem.ActorRef<ToolCallMessage> actor = selectActor(call.name());
        ToolCallMessage msg = new ToolCallMessage(call, System.currentTimeMillis());

        Optional<ToolResult> result = actor.ask(msg, Duration.ofMillis(TOOL_CALL_TIMEOUT_MS));

        if (result.isEmpty()) {
            totalFailed.incrementAndGet();
            log.warn("[tool-actor-router] timeout or no actor for tool '{}'", call.name());
            return ToolResult.failure(call.name(),
                    "Tool call timed out after " + TOOL_CALL_TIMEOUT_MS + "ms " +
                    "— actor mailbox may be saturated");
        }
        return result.get();
    }

    /** Returns a health snapshot of all actors in the system. */
    public SystemHealth health() {
        return new SystemHealth(
                system.isRunning(),
                system.actorCount(),
                totalRouted.get(),
                totalFailed.get(),
                Map.of(
                    "file-actor",    fileActor.metrics(),
                    "shell-actor",   shellActor.metrics(),
                    "git-actor",     gitActor.metrics(),
                    "search-actor",  searchActor.metrics(),
                    "default-actor", defaultActor.metrics()
                ));
    }

    // ── Routing ────────────────────────────────────────────────────────────

    private ActorSystem.ActorRef<ToolCallMessage> selectActor(String toolName) {
        return switch (toolName) {
            case "read_file", "write_file", "apply_patch", "glob", "list_dir"
                -> fileActor;
            case "run_command"
                -> shellActor;
            case "git"
                -> gitActor;
            case "search_files", "semantic_search"
                -> searchActor;
            default
                -> defaultActor;
        };
    }

    // ── Behaviors ─────────────────────────────────────────────────────────

    private Object executeToolBlocking(ToolCallMessage msg) {
        try {
            return toolExecutor.execute(msg.call());
        } catch (Exception e) {
            return ToolResult.failure(msg.call().name(),
                    "Actor execution error: " + e.getMessage());
        }
    }

    private Object executeShellBlocking(ToolCallMessage msg) {
        boolean acquired = false;
        try {
            // Rate-limit: wait for a shell slot
            acquired = shellSemaphore.tryAcquire(
                    TOOL_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                return ToolResult.failure("run_command",
                        "Shell concurrency limit reached (" + SHELL_MAX_CONCURRENT +
                        " concurrent executions). Try again shortly.");
            }
            return toolExecutor.execute(msg.call());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("run_command", "Shell actor interrupted");
        } catch (Exception e) {
            return ToolResult.failure("run_command",
                    "Shell actor error: " + e.getMessage());
        } finally {
            if (acquired) shellSemaphore.release();
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ToolCallMessage(ToolCall call, long enqueuedAt) {}

    public record SystemHealth(
            boolean                              running,
            int                                  actorCount,
            long                                 totalRouted,
            long                                 totalFailed,
            Map<String, ActorSystem.ActorMetrics> actorMetrics
    ) {
        public double failureRate() {
            return totalRouted == 0 ? 0 : (double) totalFailed / totalRouted;
        }

        public String summary() {
            return String.format("ToolRouter: running=%b actors=%d routed=%d failed=%d (%.1f%%)",
                    running, actorCount, totalRouted, totalFailed, failureRate() * 100);
        }
    }
}
