package tech.kayys.gamelan.agent.orchestration;

/**
 * Observer interface for the single-agent ReAct loop.
 *
 * <p>Implement this to receive callbacks at every significant boundary
 * during agent execution — useful for progress tracking, logging,
 * UI integration, and AG-UI event generation.
 *
 * <p>All methods have no-op defaults so implementors only override what
 * they need. The {@link #NOOP} constant provides a pre-built no-op.
 *
 * <h2>Callback order</h2>
 * <pre>
 * onRunStart
 *   onIterationStart(0)
 *     onTextChunk × N
 *     onToolStart, onToolEnd × M
 *   onIterationEnd(0)
 *   onIterationStart(1) ...
 * onComplete | onError
 * </pre>
 */
public interface AgentEventListener {

    /** Agent loop is starting. */
    default void onRunStart(String task, String model) {}

    /** A reasoning iteration begins. */
    default void onIterationStart(int iteration, int maxIterations) {}

    /** A reasoning iteration ends with a stop reason. */
    default void onIterationEnd(int iteration, String stopReason) {}

    /**
     * A token chunk arrived from the LLM stream.
     * Called for every streaming delta — may be called thousands of times per turn.
     */
    default void onTextChunk(String chunk) {}

    /** A tool call is about to be executed. */
    default void onToolStart(String toolName, String inputSummary) {}

    /** A tool call completed. */
    default void onToolEnd(String toolName, String resultSummary,
                            boolean error, long durationMs) {}

    /** Agent completed successfully. */
    default void onComplete(String finalAnswer, int totalIterations) {}

    /** An unrecoverable error occurred. */
    default void onError(String errorMessage, int iteration) {}

    /** Pre-built no-op listener. */
    AgentEventListener NOOP = new AgentEventListener() {};

    /**
     * Builds a listener that logs to SLF4J at DEBUG level.
     * Useful during development and for structured log analysis.
     */
    static AgentEventListener logging(org.slf4j.Logger log) {
        return new AgentEventListener() {
            @Override public void onRunStart(String task, String model) {
                log.debug("[agent] run start model={} task={}", model,
                        task.length() > 60 ? task.substring(0, 60) + "…" : task);
            }
            @Override public void onIterationStart(int iter, int max) {
                log.debug("[agent] iteration {}/{}", iter + 1, max);
            }
            @Override public void onIterationEnd(int iter, String stop) {
                log.debug("[agent] iteration {} done reason={}", iter + 1, stop);
            }
            @Override public void onToolStart(String name, String input) {
                log.debug("[agent] tool start: {}", name);
            }
            @Override public void onToolEnd(String name, String result, boolean err, long ms) {
                log.debug("[agent] tool end: {} error={} ms={}", name, err, ms);
            }
            @Override public void onComplete(String answer, int iters) {
                log.debug("[agent] complete after {} iterations", iters);
            }
            @Override public void onError(String msg, int iter) {
                log.debug("[agent] error at iter {}: {}", iter, msg);
            }
        };
    }
}
