package tech.kayys.wayang.agent.spi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.InferenceTypes.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core SPI for agent orchestrators with enhanced Gollek integration.
 *
 * <p>
 * An orchestrator implements a reasoning strategy (ReAct, Plan-and-Execute,
 * Chain-of-Thought, Reflexion, etc.) that governs how an agent iterates through its
 * reasoning loop, calls skills/tools, and terminates.
 * </p>
 *
 * <h2>Extension Points:</h2>
 * <p>
 * Implementors can override default methods to customize:
 * <ul>
 *   <li>Tool calling behavior ({@link #supportsToolCalling()}, {@link #onToolCall})</li>
 *   <li>Streaming behavior ({@link #supportsStreaming()})</li>
 *   <li>Checkpoint/resume ({@link #supportsCheckpoint()}, {@link #saveState}, {@link #restoreState})</li>
 *   <li>Multi-agent coordination ({@link #supportsMultiAgent()})</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 2.0.0
 * @since 2026-03-28
 */
public interface AgentOrchestrator {

    /**
     * Unique orchestrator strategy identifier (e.g. {@code "react"},
     * {@code "plan-and-execute"}, {@code "reflexion"}).
     *
     * @return strategy ID
     */
    String strategyId();

    /**
     * Execute agent request to completion (blocking-style via Uni).
     *
     * @param request the agent request
     * @return reactive response upon completion or failure
     */
    Uni<AgentResponse> execute(AgentRequest request);

    /**
     * Execute agent request as a streaming event stream.
     * Emits {@link AgentEvent}s in real-time: thoughts, tool calls, observations,
     * final answer.
     *
     * @param request the agent request
     * @return event stream
     */
    Multi<AgentEvent> stream(AgentRequest request);

    /**
     * Execute a single reasoning step given the current agent state.
     * Used for step-by-step debugging or external orchestration control.
     *
     * @param state current agent execution state
     * @return updated state after the step
     */
    Uni<AgentState> step(AgentState state);

    /**
     * Determine whether the agent has reached a terminal state
     * (final answer found, error, max steps exceeded).
     *
     * @param state current agent state
     * @return true if the agent should stop
     */
    boolean isTerminal(AgentState state);

    /**
     * Get current orchestration strategy description for LLM system prompt
     * injection.
     *
     * @return system prompt fragment
     */
    default String getSystemPromptFragment() {
        return "";
    }

    /**
     * List supported features of this orchestrator.
     *
     * @return list of feature names
     */
    default List<String> supportedFeatures() {
        return List.of();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Gollek-Specific Extensions (v2.0)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if this orchestrator supports native tool calling.
     * When true, the orchestrator can handle {@link InferenceResponse#getToolCalls}
     * directly from the model.
     *
     * @return true if tool calling is supported
     */
    default boolean supportsToolCalling() {
        return false;
    }

    /**
     * Called when the model generates a tool call. Override this method to
     * implement custom tool execution logic.
     *
     * @param state current agent state
     * @param toolName name of the tool to call
     * @param arguments tool arguments
     * @return Uni containing the tool execution result
     */
    default Uni<String> onToolCall(AgentState state, String toolName, Map<String, Object> arguments) {
        return Uni.createFrom().failure(
            new UnsupportedOperationException("Tool calling not supported by this orchestrator")
        );
    }

    /**
     * Check if this orchestrator supports streaming responses.
     *
     * @return true if streaming is supported
     */
    default boolean supportsStreaming() {
        return true;
    }

    /**
     * Check if this orchestrator supports checkpoint/resume functionality.
     * When true, the orchestrator can save and restore execution state.
     *
     * @return true if checkpoint is supported
     */
    default boolean supportsCheckpoint() {
        return false;
    }

    /**
     * Save the current execution state for later resumption.
     *
     * @param state current agent state
     * @param checkpointId unique identifier for this checkpoint
     * @return Uni that completes when state is saved
     */
    default Uni<Void> saveState(AgentState state, String checkpointId) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Restore execution state from a checkpoint.
     *
     * @param checkpointId unique identifier for the checkpoint
     * @return Uni containing the restored state
     */
    default Uni<AgentState> restoreState(String checkpointId) {
        return Uni.createFrom().failure(
            new UnsupportedOperationException("Checkpoint restore not supported by this orchestrator")
        );
    }

    /**
     * Check if this orchestrator supports multi-agent coordination.
     *
     * @return true if multi-agent is supported
     */
    default boolean supportsMultiAgent() {
        return false;
    }

    /**
     * Get the preferred provider ID for this orchestrator (optional).
     * Some orchestrators may prefer specific providers (e.g., cloud for complex reasoning).
     *
     * @return optional preferred provider ID
     */
    default Optional<String> getPreferredProvider() {
        return Optional.empty();
    }

    /**
     * Get recommended inference parameters for this orchestrator.
     *
     * @return map of parameter name to value
     */
    default Map<String, Object> getRecommendedParameters() {
        return Map.of(
            "temperature", 0.7,
            "max_tokens", 1024
        );
    }

    /**
     * Get the timeout for a single inference step.
     *
     * @return timeout duration
     */
    default Duration getStepTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * Get the maximum number of reasoning steps allowed.
     *
     * @return max steps
     */
    default int getMaxSteps() {
        return 10;
    }

    /**
     * Called when the agent starts execution. Override for custom initialization.
     *
     * @param request the agent request
     * @param state initial agent state
     */
    default void onStart(AgentRequest request, AgentState state) {
        // No-op by default
    }

    /**
     * Called when the agent completes execution (successfully or with error).
     * Override for custom cleanup or logging.
     *
     * @param request the agent request
     * @param state final agent state
     * @param response the agent response
     */
    default void onComplete(AgentRequest request, AgentState state, AgentResponse response) {
        // No-op by default
    }

    /**
     * Called when an error occurs during execution. Override for custom error handling.
     *
     * @param request the agent request
     * @param state current agent state
     * @param error the error that occurred
     */
    default void onError(AgentRequest request, AgentState state, Throwable error) {
        // No-op by default
    }

    /**
     * Get tool definitions for this orchestrator (if it supports tool calling).
     *
     * @param request the current agent request
     * @return list of tool definitions
     */
    default List<ToolDefinition> getToolDefinitions(AgentRequest request) {
        return List.of();
    }

    /**
     * Build an inference request for this orchestrator.
     * Override to customize request construction.
     *
     * @param request the agent request
     * @param state current agent state
     * @return inference request builder
     */
    default tech.kayys.wayang.agent.spi.InferenceRequest.Builder buildInferenceRequest(
            AgentRequest request,
            AgentState state) {

        return tech.kayys.wayang.agent.spi.InferenceRequest.builder()
            .model(request.modelId() != null ? request.modelId() : "default")
            .temperature(0.7)
            .maxTokens(1024)
            .timeout(getStepTimeout());
    }
}
