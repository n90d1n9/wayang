package tech.kayys.gamelan.resilience.circuit;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Agent Resilience Kit — circuit breaker, retry, and bulkhead for tool calls.
 *
 * <h2>Why agents need resilience primitives</h2>
 * Agent tool calls are I/O operations against external systems — filesystems,
 * shells, git, search APIs, LLM endpoints. These fail in ways that matter:
 * <ul>
 *   <li>Transient failures (network blip, shell timeout) should be retried</li>
 *   <li>Persistent failures (service down) should open a circuit breaker to
 *       stop hammering the failing system and waste tokens explaining errors</li>
 *   <li>Slow calls should have per-tool bulkheads to prevent one slow tool
 *       from blocking all parallel agent branches</li>
 * </ul>
 *
 * <h2>CircuitBreaker states</h2>
 * <pre>
 * CLOSED ──(failure_rate >= threshold)──▶ OPEN
 *   ▲                                       │
 *   └──(success)──── HALF_OPEN ◀──(timeout)─┘
 * </pre>
 *
 * <h2>Retry with exponential backoff + jitter</h2>
 * Jitter prevents the "thundering herd" problem where all retrying clients
 * synchronize their retry attempts.
 *
 * <h2>Bulkhead</h2>
 * Limits concurrent executions per tool category. When the bulkhead is full,
 * the caller waits (up to timeout) rather than spawning unbounded work.
 * This matches the actor-model semantics of {@code ToolActorRouter}.
 */
@ApplicationScoped
public class AgentResilienceKit {

    private static final Logger log = LoggerFactory.getLogger(AgentResilienceKit.class);

    // Per-resource circuit breakers (keyed by tool name or endpoint)
    private final Map<String, CircuitBreaker>  breakers = new ConcurrentHashMap<>();
    // Per-category bulkheads (keyed by category: "file", "shell", "network")
    private final Map<String, Bulkhead>        bulkheads = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Executes a supplier with circuit-breaker protection.
     * Throws {@link CircuitOpenException} when the circuit is OPEN.
     */
    public <T> T withCircuitBreaker(String resourceId, Supplier<T> action) {
        CircuitBreaker breaker = breakers.computeIfAbsent(resourceId,
                k -> new CircuitBreaker(k, CircuitBreakerConfig.defaults()));
        return breaker.execute(action);
    }

    /**
     * Executes with retry using exponential backoff and jitter.
     *
     * @param maxAttempts   total attempts (1 = no retry)
     * @param baseDelayMs   base delay between retries
     * @param retryOn       predicate to decide if an exception is retryable
     */
    public <T> T withRetry(String operationName, int maxAttempts, long baseDelayMs,
                            Predicate<Exception> retryOn, Supplier<T> action) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = action.get();
                if (attempt > 1) {
                    log.info("[resilience] '{}' succeeded on attempt {}", operationName, attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxAttempts || !retryOn.test(e)) {
                    log.warn("[resilience] '{}' failed permanently after {} attempts: {}",
                            operationName, attempt, e.getMessage());
                    throw new RuntimeException("Failed after " + attempt + " attempts: " + operationName, e);
                }
                long delay = exponentialBackoffWithJitter(baseDelayMs, attempt);
                log.warn("[resilience] '{}' attempt {}/{} failed, retrying in {}ms: {}",
                        operationName, attempt, maxAttempts, delay, e.getMessage());
                try { Thread.sleep(delay); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        throw new RuntimeException("Retry loop exited unexpectedly", lastException);
    }

    /**
     * Executes within a bulkhead — limits concurrent calls to the given category.
     *
     * @param category       "file", "shell", "network", etc.
     * @param maxConcurrent  max simultaneous executions
     * @param timeoutMs      how long to wait for a slot
     */
    public <T> T withBulkhead(String category, int maxConcurrent,
                               long timeoutMs, Supplier<T> action) {
        Bulkhead bh = bulkheads.computeIfAbsent(category,
                k -> new Bulkhead(k, maxConcurrent));
        return bh.execute(action, timeoutMs);
    }

    /**
     * Combines circuit breaker + retry + bulkhead in one call.
     * This is the recommended entry point for all tool-call wrappers.
     */
    public <T> T protect(String toolName, String category, int maxConcurrent,
                          int maxRetries, Supplier<T> action) {
        return withBulkhead(category, maxConcurrent, 10_000, () ->
                withCircuitBreaker(toolName, () ->
                        withRetry(toolName, maxRetries, 200,
                                e -> isTransient(e), action)));
    }

    /**
     * Returns the current state of all circuit breakers.
     */
    public Map<String, CircuitBreaker.State> circuitStates() {
        Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
        breakers.forEach((k, v) -> states.put(k, v.state()));
        return Collections.unmodifiableMap(states);
    }

    /**
     * Returns usage stats for all bulkheads.
     */
    public Map<String, BulkheadStats> bulkheadStats() {
        Map<String, BulkheadStats> stats = new LinkedHashMap<>();
        bulkheads.forEach((k, v) -> stats.put(k, v.stats()));
        return Collections.unmodifiableMap(stats);
    }

    /**
     * Manually opens a circuit breaker (useful for testing or planned maintenance).
     */
    public void forceOpen(String resourceId) {
        breakers.computeIfAbsent(resourceId,
                k -> new CircuitBreaker(k, CircuitBreakerConfig.defaults()))
                .forceOpen();
    }

    /**
     * Resets a circuit breaker back to CLOSED state.
     */
    public void reset(String resourceId) {
        CircuitBreaker cb = breakers.get(resourceId);
        if (cb != null) cb.reset();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private long exponentialBackoffWithJitter(long baseMs, int attempt) {
        long exp = Math.min(baseMs * (1L << (attempt - 1)), 30_000); // cap at 30s
        long jitter = ThreadLocalRandom.current().nextLong(0, exp / 2 + 1);
        return exp + jitter;
    }

    private boolean isTransient(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("timeout") || msg.contains("connection") ||
               msg.contains("temporary") || msg.contains("unavailable") ||
               e instanceof TimeoutException || e instanceof java.io.IOException;
    }

    // ── CircuitBreaker ─────────────────────────────────────────────────────

    public static final class CircuitBreaker {
        private final String               resourceId;
        private final CircuitBreakerConfig config;

        private volatile State             state        = State.CLOSED;
        private volatile Instant           openedAt;
        private final AtomicInteger        failures     = new AtomicInteger();
        private final AtomicInteger        successes    = new AtomicInteger();
        private final AtomicInteger        totalCalls   = new AtomicInteger();
        private final AtomicInteger        rejections   = new AtomicInteger();

        CircuitBreaker(String resourceId, CircuitBreakerConfig config) {
            this.resourceId = resourceId;
            this.config     = config;
        }

        public <T> T execute(Supplier<T> action) {
            State current = currentState();

            if (current == State.OPEN) {
                rejections.incrementAndGet();
                throw new CircuitOpenException(resourceId);
            }

            totalCalls.incrementAndGet();
            try {
                T result = action.get();
                onSuccess();
                return result;
            } catch (CircuitOpenException e) {
                throw e;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }

        private State currentState() {
            if (state == State.OPEN && openedAt != null) {
                Duration elapsed = Duration.between(openedAt, Instant.now());
                if (elapsed.getSeconds() >= config.openDurationSeconds()) {
                    log.info("[circuit-breaker] '{}' transitioning OPEN→HALF_OPEN", resourceId);
                    state = State.HALF_OPEN;
                    failures.set(0);
                }
            }
            return state;
        }

        private void onSuccess() {
            successes.incrementAndGet();
            if (state == State.HALF_OPEN) {
                if (successes.get() >= config.halfOpenSuccessThreshold()) {
                    log.info("[circuit-breaker] '{}' CLOSED (recovered)", resourceId);
                    state = State.CLOSED;
                    failures.set(0);
                    successes.set(0);
                }
            } else if (state == State.CLOSED) {
                // Decay failure count on success
                failures.updateAndGet(f -> Math.max(0, f - 1));
            }
        }

        private void onFailure() {
            int f = failures.incrementAndGet();
            if (state == State.HALF_OPEN) {
                log.warn("[circuit-breaker] '{}' HALF_OPEN→OPEN (probe failed)", resourceId);
                state    = State.OPEN;
                openedAt = Instant.now();
            } else if (state == State.CLOSED &&
                       failureRate() >= config.failureRateThreshold()) {
                log.warn("[circuit-breaker] '{}' CLOSED→OPEN (failure rate {:.0f}%)",
                        resourceId, failureRate() * 100);
                state    = State.OPEN;
                openedAt = Instant.now();
            }
        }

        private double failureRate() {
            int total = totalCalls.get();
            return total == 0 ? 0 : (double) failures.get() / total;
        }

        public void forceOpen() { state = State.OPEN; openedAt = Instant.now(); }
        public void reset()     { state = State.CLOSED; failures.set(0); successes.set(0); }

        public State state()      { return currentState(); }
        public int rejections()   { return rejections.get(); }
        public int totalCalls()   { return totalCalls.get(); }
        public double failRate()  { return failureRate(); }

        public enum State { CLOSED, OPEN, HALF_OPEN }
    }

    public record CircuitBreakerConfig(
            double failureRateThreshold,      // 0.0–1.0: open when failure rate >= this
            int    openDurationSeconds,        // seconds to wait before trying HALF_OPEN
            int    halfOpenSuccessThreshold    // successes needed in HALF_OPEN to CLOSE
    ) {
        public static CircuitBreakerConfig defaults() {
            return new CircuitBreakerConfig(0.5, 30, 3);
        }
        public static CircuitBreakerConfig sensitive() {
            return new CircuitBreakerConfig(0.3, 60, 5);
        }
        public static CircuitBreakerConfig lenient() {
            return new CircuitBreakerConfig(0.7, 10, 1);
        }
    }

    public static final class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String resourceId) {
            super("Circuit breaker OPEN for resource: " + resourceId);
        }
    }

    // ── Bulkhead ──────────────────────────────────────────────────────────

    public static final class Bulkhead {
        private final String    category;
        private final Semaphore semaphore;
        private final AtomicLong rejected = new AtomicLong();
        private final AtomicLong acquired = new AtomicLong();

        Bulkhead(String category, int maxConcurrent) {
            this.category  = category;
            this.semaphore = new Semaphore(maxConcurrent, true);
        }

        public <T> T execute(Supplier<T> action, long timeoutMs) {
            try {
                if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                    rejected.incrementAndGet();
                    throw new BulkheadFullException(category);
                }
                acquired.incrementAndGet();
                try { return action.get(); }
                finally { semaphore.release(); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Bulkhead interrupted: " + category, e);
            }
        }

        public BulkheadStats stats() {
            return new BulkheadStats(category, semaphore.availablePermits(),
                    semaphore.getQueueLength(), acquired.get(), rejected.get());
        }
    }

    public record BulkheadStats(
            String category,
            int    availableSlots,
            int    queuedWaiters,
            long   totalAcquired,
            long   totalRejected
    ) {
        public String summary() {
            return String.format("Bulkhead[%s]: %d slots free, %d queued, %d rejected",
                    category, availableSlots, queuedWaiters, totalRejected);
        }
    }

    public static final class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String category) {
            super("Bulkhead full for category: " + category);
        }
    }
}
