package tech.kayys.wayang.agent.core.core;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.wayang.agent.spi.*;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Pure Java agent client — no Quarkus runtime dependencies.
 *
 * <p>
 * This is the primary entry point for executing agents. It provides a clean,
 * backend-agnostic API that works in any Java environment (plain Java, Quarkus,
 * Spring, etc.).
 * </p>
 *
 * <h3>Usage — Standalone (No Quarkus):</h3>
 * <pre>{@code
 * // Initialize backends
 * BackendRegistry.initialize();
 *
 * // Create client
 * AgentClient client = AgentClient.builder()
 *     .inferenceBackend(BackendRegistry.getDefaultInferenceBackend())
 *     .workflowBackend(BackendRegistry.getDefaultWorkflowBackend())
 *     .build();
 *
 * // Execute agent
 * AgentRequest request = AgentRequest.builder()
 *     .prompt("Summarize the latest AI research")
 *     .strategy(OrchestrationStrategy.REACT)
 *     .build();
 *
 * AgentResponse response = client.execute(request)
 *     .await().atMost(Duration.ofSeconds(30));
 * }</pre>
 *
 * <h3>Usage — Quarkus (CDI):</h3>
 * <pre>{@code
 * @Inject AgentClient agentClient;  // Auto-wired by CDI producer
 *
 * public Uni<AgentResponse> run(String prompt) {
 *     return agentClient.execute(AgentRequest.builder()
 *         .prompt(prompt)
 *         .strategy(OrchestrationStrategy.REACT)
 *         .build());
 * }
 * }</pre>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Backend-agnostic — works with any inference/workflow backend</li>
 *   <li>No framework coupling — pure Java + Mutiny only</li>
 *   <li>Builder pattern — clear, explicit construction</li>
 *   <li>Auto-resolution — defaults to BackendRegistry if not specified</li>
 *   <li>Thread-safe — can be shared across threads</li>
 * </ul>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class AgentClient {

    private static final Logger LOG = Logger.getLogger(AgentClient.class);

    private final InferenceBackend inferenceBackend;
    private final WorkflowBackend workflowBackend;
    private final AgentConfig config;

    /**
     * Create agent client with explicit backends.
     *
     * @param builder builder with configuration
     */
    private AgentClient(Builder builder) {
        this.inferenceBackend = Objects.requireNonNull(builder.inferenceBackend,
            "inferenceBackend is required. Call BackendRegistry.initialize() or provide explicitly.");
        this.workflowBackend = builder.workflowBackend;  // Optional
        this.config = builder.config != null ? builder.config : AgentConfig.defaults();
    }

    /**
     * Execute agent request using configured orchestrator strategy.
     *
     * @param request agent execution request
     * @return Uni containing agent response
     */
    public Uni<AgentResponse> execute(AgentRequest request) {
        LOG.debugf("Executing agent request: strategy=%s, tenant=%s",
            request.strategy(), request.tenantId());

        try {
            // Get orchestrator for the requested strategy
            AgentOrchestrator orchestrator = resolveOrchestrator(request);

            // Execute via orchestrator
            return orchestrator.execute(request)
                .onItem().invoke(response ->
                    LOG.debugf("Agent execution completed: strategy=%s, steps=%d",
                        request.strategy(), response.totalSteps())
                )
                .onFailure().invoke(err ->
                    LOG.errorf(err, "Agent execution failed: strategy=%s", request.strategy())
                );
        } catch (Exception e) {
            LOG.errorf(e, "Failed to resolve orchestrator for strategy: %s", request.strategy());
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Get the inference backend in use.
     *
     * @return inference backend instance
     */
    public InferenceBackend inferenceBackend() {
        return inferenceBackend;
    }

    /**
     * Get the workflow backend in use (may be null).
     *
     * @return workflow backend instance or null
     */
    public WorkflowBackend workflowBackend() {
        return workflowBackend;
    }

    /**
     * Get agent configuration.
     *
     * @return configuration
     */
    public AgentConfig config() {
        return config;
    }

    /**
     * Create a new builder for agent client.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // ── Orchestrator Resolution ──────────────────────────────────────────

    /**
     * Resolve orchestrator for the requested strategy.
     * Uses AgentOrchestrator registry if available, falls back to defaults.
     */
    private AgentOrchestrator resolveOrchestrator(AgentRequest request) {
        String strategyId = request.strategy().id;

        // Try to get orchestrator from BackendRegistry or default implementations
        // This is a simplified implementation - in production, you'd have an
        // OrchestratorRegistry that discovers and caches orchestrator instances
        return createDefaultOrchestrator(strategyId);
    }

    /**
     * Create default orchestrator for strategy ID.
     * In production, this would delegate to an OrchestratorRegistry.
     */
    private AgentOrchestrator createDefaultOrchestrator(String strategyId) {
        return switch (strategyId.toLowerCase()) {
            case "react" -> new tech.kayys.wayang.agent.core.orchestrator.ReActOrchestrator();
            case "plan_and_execute", "plan-and-execute" ->
                new tech.kayys.wayang.agent.core.orchestrator.PlanAndExecuteOrchestrator();
            case "reflexion" ->
                new tech.kayys.wayang.agent.core.orchestrator.ReflexionOrchestrator();
            default ->
                new tech.kayys.wayang.agent.core.orchestrator.ReActOrchestrator();  // Default to ReAct
        };
    }

    // ── Builder ──────────────────────────────────────────────────────────

    /**
     * Builder for AgentClient.
     *
     * <p>Provides a fluent API for constructing agent clients with explicit
     * or auto-resolved backend dependencies.</p>
     */
    public static final class Builder {
        private InferenceBackend inferenceBackend;
        private WorkflowBackend workflowBackend;
        private AgentConfig config;

        private Builder() {
            // Private constructor — use AgentClient.builder()
        }

        /**
         * Set inference backend explicitly.
         *
         * @param backend inference backend instance
         * @return builder for chaining
         */
        public Builder inferenceBackend(InferenceBackend backend) {
            this.inferenceBackend = backend;
            return this;
        }

        /**
         * Set workflow backend explicitly.
         *
         * @param backend workflow backend instance
         * @return builder for chaining
         */
        public Builder workflowBackend(WorkflowBackend backend) {
            this.workflowBackend = backend;
            return this;
        }

        /**
         * Set agent configuration.
         *
         * @param config configuration
         * @return builder for chaining
         */
        public Builder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Auto-resolve inference backend from BackendRegistry.
         * Convenience method — equivalent to
         * {@code inferenceBackend(BackendRegistry.getDefaultInferenceBackend())}.
         *
         * @return builder for chaining
         */
        public Builder defaultInferenceBackend() {
            this.inferenceBackend = BackendRegistry.getDefaultInferenceBackend();
            return this;
        }

        /**
         * Auto-resolve workflow backend from BackendRegistry.
         * Convenience method — equivalent to
         * {@code workflowBackend(BackendRegistry.getDefaultWorkflowBackend())}.
         *
         * @return builder for chaining
         */
        public Builder defaultWorkflowBackend() {
            this.workflowBackend = BackendRegistry.getDefaultWorkflowBackend();
            return this;
        }

        /**
         * Build the agent client.
         *
         * <p>If inferenceBackend is not set, attempts to resolve from BackendRegistry.
         * If BackendRegistry is not initialized, throws IllegalStateException.</p>
         *
         * @return configured agent client
         * @throws IllegalStateException if inferenceBackend cannot be resolved
         */
        public AgentClient build() {
            // Auto-resolve from BackendRegistry if not provided
            if (inferenceBackend == null) {
                try {
                    inferenceBackend = BackendRegistry.getDefaultInferenceBackend();
                    LOG.debug("Auto-resolved inference backend from BackendRegistry");
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(
                        "Cannot resolve inference backend: BackendRegistry not initialized. " +
                        "Call BackendRegistry.initialize() or provide inferenceBackend explicitly.",
                        e
                    );
                }
            }

            // Workflow backend is optional — auto-resolve if available
            if (workflowBackend == null) {
                try {
                    workflowBackend = BackendRegistry.getDefaultWorkflowBackend();
                    LOG.debug("Auto-resolved workflow backend from BackendRegistry");
                } catch (IllegalStateException e) {
                    LOG.debug("Workflow backend not available from BackendRegistry");
                }
            }

            return new AgentClient(this);
        }
    }
}
