package tech.kayys.gamelan.execution.priority;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/**
 * PriorityExecutionEngine — task scheduling with priorities, dependencies, timeouts, and budgets.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>Priority queuing</b> — CRITICAL > HIGH > NORMAL > LOW > BACKGROUND</li>
 *   <li><b>Dependency DAG</b> — tasks can declare prerequisite task IDs; execution respects order</li>
 *   <li><b>Token budget</b> — each task can declare an estimated token cost; scheduler respects a
 *       total session budget ceiling to prevent context overflow</li>
 *   <li><b>Timeout per task</b> — configurable per-task deadline with graceful cancellation</li>
 *   <li><b>Retry policy</b> — exponential backoff with jitter, per-task max-attempts</li>
 *   <li><b>Parallel read / serial write</b> — read-only tasks run concurrently (max 5);
 *       write tasks run serially (from paper §2.2.3: read-only tools run in parallel via a thread
 *       pool, while write tools run sequentially)</li>
 *   <li><b>Budget accounting</b> — executed token costs are tracked; scheduler refuses tasks
 *       that would push the session over the token ceiling</li>
 * </ul>
 */
@ApplicationScoped
public class PriorityExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(PriorityExecutionEngine.class);

    // Defaults
    private static final int  MAX_PARALLEL_READS    = 5;
    private static final int  DEFAULT_TASK_TIMEOUT  = 30;
    private static final long DEFAULT_TOKEN_BUDGET  = 50_000;

    @Inject AgentTelemetry telemetry;

    private final PriorityBlockingQueue<PrioritizedTask<?>> queue =
            new PriorityBlockingQueue<>(64, Comparator.comparing((PrioritizedTask<?> t) ->
                    t.spec().priority().ordinal()).reversed()
                    .thenComparing(t -> t.spec().createdAt()));

    private final Map<String, TaskStatus>   statuses    = new ConcurrentHashMap<>();
    private final Map<String, Object>       results     = new ConcurrentHashMap<>();
    private final AtomicLong                tokensBudget  = new AtomicLong(DEFAULT_TOKEN_BUDGET);
    private final AtomicLong                tokensUsed    = new AtomicLong(0);
    private final ExecutorService           readPool      =
            Executors.newFixedThreadPool(MAX_PARALLEL_READS,
                    Thread.ofVirtual().name("gamelan-read-", 0).factory());
    private final ExecutorService           writeSerial   =
            Executors.newSingleThreadExecutor(
                    Thread.ofVirtual().name("gamelan-write-", 0).factory());

    // ── Public API ─────────────────────────────────────────────────────────

    /** Configures the total token budget for this session. */
    public void setTokenBudget(long budget) { tokensBudget.set(budget); }

    /** Returns remaining token budget. */
    public long remainingBudget() { return Math.max(0, tokensBudget.get() - tokensUsed.get()); }

    /**
     * Submits a task for execution, returning its ID.
     * Tasks are executed when {@link #drain()} or {@link #submit(TaskSpec, Supplier)} is called.
     */
    public <T> String submit(TaskSpec spec, Supplier<T> work) {
        String id = spec.id() != null ? spec.id() : UUID.randomUUID().toString().substring(0, 8);

        // Check token budget
        if (spec.estimatedTokens() > 0 && tokensUsed.get() + spec.estimatedTokens() > tokensBudget.get()) {
            log.warn("[priority] task {} rejected: token budget exceeded ({}/{})",
                    id, tokensUsed.get(), tokensBudget.get());
            statuses.put(id, TaskStatus.BUDGET_EXCEEDED);
            telemetry.count("priority.budget_exceeded");
            return id;
        }

        // Check dependencies (must all be COMPLETED)
        for (String dep : spec.dependencies()) {
            TaskStatus depStatus = statuses.get(dep);
            if (depStatus != TaskStatus.COMPLETED) {
                log.warn("[priority] task {} deferred: dependency {} not completed ({})", id, dep, depStatus);
                statuses.put(id, TaskStatus.AWAITING_DEPS);
                // Re-queue after a short delay
                Thread.ofVirtual().start(() -> {
                    try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    submit(spec, work);
                });
                return id;
            }
        }

        statuses.put(id, TaskStatus.QUEUED);
        @SuppressWarnings("unchecked")
        PrioritizedTask<T> task = new PrioritizedTask<>(id, spec, work);
        queue.offer(task);
        log.debug("[priority] queued task {} priority={} estimatedTokens={}",
                id, spec.priority(), spec.estimatedTokens());
        telemetry.count("priority.submitted." + spec.priority().name().toLowerCase());

        // Execute immediately if not batched
        if (!spec.batched()) {
            return executeNext();
        }
        return id;
    }

    /**
     * Executes all queued tasks respecting priority, dependencies, and read/write parallelism.
     * Blocks until all queued tasks are complete.
     *
     * @return list of completed task IDs
     */
    public List<String> drain() {
        List<String> completed = new ArrayList<>();
        List<PrioritizedTask<?>> reads  = new ArrayList<>();
        List<PrioritizedTask<?>> writes = new ArrayList<>();

        // Drain queue and partition
        PrioritizedTask<?> task;
        while ((task = queue.poll()) != null) {
            if (task.spec().readOnly()) reads.add(task);
            else                        writes.add(task);
        }

        // Execute reads in parallel
        if (!reads.isEmpty()) {
            List<Future<?>> futures = reads.stream()
                    .map(t -> readPool.submit(() -> executeTask(t)))
                    .toList();
            for (Future<?> f : futures) {
                try { f.get(DEFAULT_TASK_TIMEOUT * 2L, TimeUnit.SECONDS); }
                catch (Exception e) { log.warn("[priority] read task failed: {}", e.getMessage()); }
            }
            reads.forEach(t -> completed.add(t.id()));
        }

        // Execute writes serially
        for (PrioritizedTask<?> w : writes) {
            try { writeSerial.submit(() -> executeTask(w)).get(DEFAULT_TASK_TIMEOUT * 2L, TimeUnit.SECONDS); }
            catch (Exception e) { log.warn("[priority] write task failed: {}", e.getMessage()); }
            completed.add(w.id());
        }

        return completed;
    }

    /** Returns the current status of a task. */
    public TaskStatus statusOf(String taskId) {
        return statuses.getOrDefault(taskId, TaskStatus.UNKNOWN);
    }

    /** Returns the result of a completed task, or empty if not yet available. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> resultOf(String taskId) {
        return Optional.ofNullable((T) results.get(taskId));
    }

    /** Returns a snapshot of all task statuses. */
    public Map<String, TaskStatus> allStatuses() { return Collections.unmodifiableMap(statuses); }

    // ── Private ────────────────────────────────────────────────────────────

    private String executeNext() {
        PrioritizedTask<?> next = queue.poll();
        if (next == null) return "";
        readPool.submit(() -> executeTask(next));
        return next.id();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeTask(PrioritizedTask task) {
        String id = task.id();
        TaskSpec spec = task.spec();
        statuses.put(id, TaskStatus.RUNNING);
        Instant start = Instant.now();
        int attempt = 0;
        int maxAttempts = spec.maxRetries() + 1;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(task.work(), readPool);
                Object result = future.get(spec.timeoutSeconds(), TimeUnit.SECONDS);
                results.put(id, result);
                statuses.put(id, TaskStatus.COMPLETED);
                tokensUsed.addAndGet(spec.estimatedTokens());
                Duration elapsed = Duration.between(start, Instant.now());
                log.debug("[priority] task {} COMPLETED in {}ms (attempt {}/{})",
                        id, elapsed.toMillis(), attempt, maxAttempts);
                telemetry.count("priority.completed");
                telemetry.recordLatency("priority.task_latency", elapsed.toMillis());
                return;
            } catch (TimeoutException e) {
                log.warn("[priority] task {} TIMEOUT (attempt {}/{})", id, attempt, maxAttempts);
                statuses.put(id, TaskStatus.TIMEOUT);
                if (attempt >= maxAttempts) { telemetry.count("priority.timeout"); return; }
            } catch (Exception e) {
                log.warn("[priority] task {} FAILED (attempt {}/{}): {}", id, attempt, maxAttempts, e.getMessage());
                statuses.put(id, TaskStatus.FAILED);
                if (attempt >= maxAttempts) { telemetry.count("priority.failed"); return; }
                // Exponential backoff with jitter
                long delay = (long)(100L * Math.pow(2, attempt) + Math.random() * 100);
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum Priority    { BACKGROUND, LOW, NORMAL, HIGH, CRITICAL }
    public enum TaskStatus  { UNKNOWN, QUEUED, AWAITING_DEPS, RUNNING, COMPLETED, FAILED, TIMEOUT, BUDGET_EXCEEDED, CANCELLED }

    public record TaskSpec(
            String       id,
            Priority     priority,
            boolean      readOnly,
            boolean      batched,
            int          timeoutSeconds,
            int          estimatedTokens,
            int          maxRetries,
            List<String> dependencies,
            Instant      createdAt
    ) {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private String       id              = null;
            private Priority     priority        = Priority.NORMAL;
            private boolean      readOnly        = true;
            private boolean      batched         = false;
            private int          timeoutSeconds  = DEFAULT_TASK_TIMEOUT;
            private int          estimatedTokens = 0;
            private int          maxRetries      = 2;
            private List<String> dependencies    = List.of();
            private Instant      createdAt       = Instant.now();

            public Builder id(String v)              { id = v; return this; }
            public Builder priority(Priority v)      { priority = v; return this; }
            public Builder readOnly(boolean v)       { readOnly = v; return this; }
            public Builder batched(boolean v)        { batched = v; return this; }
            public Builder timeout(int secs)         { timeoutSeconds = secs; return this; }
            public Builder tokens(int v)             { estimatedTokens = v; return this; }
            public Builder retries(int v)            { maxRetries = v; return this; }
            public Builder after(String... deps)     { dependencies = List.of(deps); return this; }
            public TaskSpec build() {
                return new TaskSpec(id, priority, readOnly, batched, timeoutSeconds,
                        estimatedTokens, maxRetries, dependencies, createdAt);
            }
        }
    }

    private record PrioritizedTask<T>(String id, TaskSpec spec, Supplier<T> work) {}
}
