package tech.kayys.wayang.agent;

import tech.kayys.wayang.json.JsonValue;
import tech.kayys.wayang.tools.spi.ToolResult;
import java.util.function.Consumer;

/**
 * Callbacks the UI layer implements to render agent activity as it streams in.
 *
 * <p>All methods are invoked on the agent's worker thread; implementations that
 * touch shared UI state must hop back to the render thread themselves.
 *
 * <p>Uses the official {@code tech.kayys.wayang.tools.spi.ToolResult} so any
 * UI or consumer is decoupled from internal tool implementations.
 */
public interface WayangAgentListener {

    void onTextDelta(String text);

    void onToolCallStart(String id, String name);

    /** Full tool input has been parsed; about to check permission and execute. */
    void onToolCallReady(String id, String name, JsonValue input);

    /**
     * A tool needs explicit approval. Call responder.accept(decision)
     * (from any thread) to unblock the agent loop.
     */
    void onToolPermissionNeeded(String id, String name, JsonValue input,
                                 Consumer<PermissionDecision> responder);

    /** The official ToolResult from the wayang.tools.spi SPI. */
    void onToolResult(String id, String name, ToolResult result);

    void onUsage(int inputTokens, int outputTokens);

    /** The whole agent turn (including any tool-use sub-steps) has finished. */
    void onDone(String stopReason);

    void onError(String message);

    /**
     * A chunk of the model's chain-of-thought reasoning text.
     * Default: no-op — only UI layers that render thinking override this.
     */
    default void onThinkingDelta(String text) {}

    /**
     * The model's thinking block for this turn is complete.
     * Default: no-op.
     */
    default void onThinkingEnd() {}
}
