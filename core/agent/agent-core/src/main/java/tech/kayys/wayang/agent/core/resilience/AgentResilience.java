package tech.kayys.wayang.agent.core.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resilience patterns for agent execution using Resilience4j.
 *
 * <p>
 * Provides circuit breaker and retry patterns for:
 * <ul>
 *   <li>Inference calls (backend/provider failures)</li>
 *   <li>Tool execution (external service failures)</li>
 *   <li>Memory operations (storage failures)</li>
 *   <li>Workflow operations (Gamelan failures)</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * AgentResilience resilience = AgentResilience.defaults();
 *
 * // Execute with circuit breaker
 * InferenceResponse response = resilience.executeWithCircuitBreaker(
 *     "inference-gollek",
 *     () -> backend.infer(request)
 * );
 *
 * // Execute with retry
 * InferenceResponse response = resilience.executeWithRetry(
 *     "inference-gpt4",
 *     3,  // max attempts
 *     () -> backend.infer(request)
 * );
 * }</pre>
 *
 * @author Wayang Team
 * @version 0.1.0
 * @since 2026-04-06
 */
public class AgentResilience {

    private static final Logger LOG = Logger.getLogger(AgentResilience.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, Retry> retries = new ConcurrentHashMap<>();

    private AgentResilience(CircuitBreakerRegistry cbRegistry, RetryRegistry retryRegistry) {
        this.circuitBreakerRegistry = cbRegistry;
        this.retryRegistry = retryRegistry;
    }

    /**
     * Create with default configuration.
     */
    public static AgentResilience defaults() {
        // Circuit breaker defaults
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);

        // Retry defaults
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(Exception.class)
            .build();

        RetryRegistry retryReg = RetryRegistry.of(retryConfig);

        return new AgentResilience(cbRegistry, retryReg);
    }

    /**
     * Execute with circuit breaker protection.
     *
     * @param name circuit breaker name
     * @param action action to execute
     * @return action result
     */
    public <T> T executeWithCircuitBreaker(String name, Supplier<T> action) {
        CircuitBreaker cb = circuitBreakers.computeIfAbsent(
            name,
            n -> circuitBreakerRegistry.circuitBreaker(n)
        );

        try {
            return CircuitBreaker.decorateSupplier(cb, action).get();
        } catch (Exception e) {
            LOG.warnf("Circuit breaker '%s' rejected call: %s", name, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute with retry protection.
     *
     * @param name retry name
     * @param maxAttempts maximum retry attempts
     * @param action action to execute
     * @return action result
     */
    public <T> T executeWithRetry(String name, int maxAttempts, Supplier<T> action) {
        Retry retry = retries.computeIfAbsent(
            name + "-" + maxAttempts,
            n -> {
                RetryConfig config = RetryConfig.custom()
                    .maxAttempts(maxAttempts)
                    .waitDuration(Duration.ofMillis(500))
                    .retryExceptions(Exception.class)
                    .build();
                return retryRegistry.retry(n, config);
            }
        );

        try {
            return Retry.decorateSupplier(retry, action).get();
        } catch (Exception e) {
            LOG.warnf("Retry '%s' exhausted after %d attempts: %s", name, maxAttempts, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute with both circuit breaker and retry.
     *
     * @param name component name
     * @param maxAttempts maximum retry attempts
     * @param action action to execute
     * @return action result
     */
    public <T> T executeWithResilience(String name, int maxAttempts, Supplier<T> action) {
        Retry retry = retries.computeIfAbsent(
            name + "-" + maxAttempts,
            n -> RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .build()).retry(n)
        );

        CircuitBreaker cb = circuitBreakers.computeIfAbsent(
            name,
            n -> circuitBreakerRegistry.circuitBreaker(n)
        );

        try {
            Supplier<T> decorated = Retry.decorateSupplier(retry, action);
            return CircuitBreaker.decorateSupplier(cb, decorated).get();
        } catch (Exception e) {
            LOG.warnf("Resilience '%s' failed: %s", name, e.getMessage());
            throw e;
        }
    }

    /**
     * Get circuit breaker state.
     */
    public CircuitBreaker.State getCircuitBreakerState(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        return cb != null ? cb.getState() : CircuitBreaker.State.CLOSED;
    }

    /**
     * Get metrics for circuit breaker.
     */
    public String getCircuitBreakerMetrics(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        if (cb == null) return "Not found";

        CircuitBreaker.Metrics metrics = cb.getMetrics();
        return String.format(
            "State: %s, Failure Rate: %.1f%%, Slow Call Rate: %.1f%%, Buffered Calls: %d",
            cb.getState(),
            metrics.getFailureRate(),
            metrics.getSlowCallRate(),
            metrics.getNumberOfBufferedCalls()
        );
    }

    /**
     * Reset circuit breaker.
     */
    public void resetCircuitBreaker(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        if (cb != null) {
            cb.reset();
            LOG.infof("Circuit breaker '%s' reset", name);
        }
    }
}
