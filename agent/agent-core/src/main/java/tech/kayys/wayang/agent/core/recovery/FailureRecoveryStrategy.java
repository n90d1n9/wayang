package tech.kayys.wayang.agent.core.recovery;

import tech.kayys.wayang.agent.core.Agent;
import tech.kayys.wayang.agent.core.AgentContext;
import tech.kayys.wayang.agent.core.AgentResponse;

import java.time.Duration;
import java.util.*;

/**
 * Strategy for recovering from agent failures in multi-agent workflows.
 * Enables resilient multi-agent systems with automatic retry and fallback capabilities.
 */
public interface FailureRecoveryStrategy {

    /**
     * Attempt recovery from agent failure.
     * 
     * @param agent The agent that failed
     * @param query The original query
     * @param context Execution context
     * @param lastError The error that occurred
     * @param attemptNumber Which attempt this is (1 = first, 2 = retry, etc.)
     * @return Optional containing recovery result, empty if recovery not possible
     */
    Optional<AgentResponse> recover(Agent agent, String query, AgentContext context,
                                   Throwable lastError, int attemptNumber);

    /**
     * Retry with exponential backoff.
     * Useful for transient failures (network timeouts, rate limits).
     */
    class RetryWithBackoffStrategy implements FailureRecoveryStrategy {
        private final int maxRetries;
        private final Duration initialDelay;
        private final double backoffMultiplier;

        public RetryWithBackoffStrategy(int maxRetries, Duration initialDelay, double backoffMultiplier) {
            this.maxRetries = maxRetries;
            this.initialDelay = initialDelay;
            this.backoffMultiplier = backoffMultiplier;
        }

        @Override
        public Optional<AgentResponse> recover(Agent agent, String query, AgentContext context,
                                              Throwable lastError, int attemptNumber) {
            if (attemptNumber > maxRetries) {
                return Optional.empty();
            }

            if (!isRetryable(lastError)) {
                return Optional.empty();
            }

            try {
                // Calculate backoff delay
                long delayMs = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1));
                Thread.sleep(delayMs);

                // Retry execution
                AgentResponse response = agent.execute(query, context);
                return Optional.of(response);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            } catch (Exception e) {
                // Further retries might still help
                return Optional.empty();
            }
        }

        private boolean isRetryable(Throwable error) {
            String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
            return message.contains("timeout") ||
                   message.contains("connection") ||
                   message.contains("temporary") ||
                   message.contains("rate limit") ||
                   error instanceof java.net.ConnectException ||
                   error instanceof java.net.SocketTimeoutException;
        }
    }

    /**
     * Fallback to different agent if primary fails.
     * Useful for specialized agents (e.g., if legal agent fails, try compliance agent).
     */
    class FallbackAgentStrategy implements FailureRecoveryStrategy {
        private final Agent fallbackAgent;

        public FallbackAgentStrategy(Agent fallbackAgent) {
            this.fallbackAgent = Objects.requireNonNull(fallbackAgent, "fallbackAgent");
        }

        @Override
        public Optional<AgentResponse> recover(Agent agent, String query, AgentContext context,
                                              Throwable lastError, int attemptNumber) {
            // Only use fallback on first attempt
            if (attemptNumber > 1) {
                return Optional.empty();
            }

            try {
                AgentResponse response = fallbackAgent.execute(query, context);
                return Optional.of(response);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    /**
     * Circuit breaker pattern for graceful degradation.
     * Stops retrying after threshold and returns cached/default result.
     */
    class CircuitBreakerStrategy implements FailureRecoveryStrategy {
        private final int failureThreshold;
        private final Duration timeout;
        private final String defaultFallbackAnswer;

        private int failureCount = 0;
        private long lastFailureTime = 0;

        public CircuitBreakerStrategy(int failureThreshold, Duration timeout, String defaultFallbackAnswer) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
            this.defaultFallbackAnswer = defaultFallbackAnswer;
        }

        @Override
        public Optional<AgentResponse> recover(Agent agent, String query, AgentContext context,
                                              Throwable lastError, int attemptNumber) {
            long now = System.currentTimeMillis();

            // Check if circuit can be reset
            if (failureCount >= failureThreshold) {
                if (now - lastFailureTime > timeout.toMillis()) {
                    // Reset circuit
                    failureCount = 0;
                } else {
                    // Circuit is open, use fallback
                    return Optional.of(createFallbackResponse(agent, defaultFallbackAnswer));
                }
            }

            // Try execution
            try {
                AgentResponse response = agent.execute(query, context);
                failureCount = 0; // Reset on success
                return Optional.of(response);
            } catch (Exception e) {
                failureCount++;
                lastFailureTime = now;
                return Optional.empty();
            }
        }

        private AgentResponse createFallbackResponse(Agent agent, String fallbackAnswer) {
            return AgentResponse.builder()
                    .agentName(agent.getName())
                    .finalAnswer(fallbackAnswer)
                    .metadata(Map.of("circuit_breaker_fallback", true))
                    .build();
        }
    }

    /**
     * Composite strategy that tries multiple recovery approaches in sequence.
     */
    class CompositeRecoveryStrategy implements FailureRecoveryStrategy {
        private final List<FailureRecoveryStrategy> strategies;

        public CompositeRecoveryStrategy(FailureRecoveryStrategy... strategies) {
            this.strategies = List.of(strategies);
        }

        @Override
        public Optional<AgentResponse> recover(Agent agent, String query, AgentContext context,
                                              Throwable lastError, int attemptNumber) {
            for (FailureRecoveryStrategy strategy : strategies) {
                Optional<AgentResponse> result = strategy.recover(agent, query, context, lastError, attemptNumber);
                if (result.isPresent()) {
                    return result;
                }
            }
            return Optional.empty();
        }
    }

    /**
     * No recovery - fail fast.
     */
    class NoRecoveryStrategy implements FailureRecoveryStrategy {
        @Override
        public Optional<AgentResponse> recover(Agent agent, String query, AgentContext context,
                                              Throwable lastError, int attemptNumber) {
            return Optional.empty();
        }
    }
}
