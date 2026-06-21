package tech.kayys.gamelan.agent.orchestration;

import tech.kayys.gamelan.tool.ToolResult;

import java.time.Duration;
import java.util.List;

/**
 * Unified result type returned by all three orchestrator tiers.
 *
 * <p>Regardless of whether the task went through a direct model call, a
 * single-agent ReAct loop, or a multi-agent orchestration, the caller
 * always receives an {@code OrchestratorResult}.
 *
 * @param answer       final text answer
 * @param strategy     which tier/strategy produced this result
 * @param steps        number of LLM calls or reasoning steps taken
 * @param toolResults  all tool executions that happened
 * @param success      false if an unrecoverable error occurred
 * @param error        error message (non-null only when success=false)
 * @param elapsed      wall-clock time for the whole orchestration
 */
public record OrchestratorResult(
        String           answer,
        String           strategy,
        int              steps,
        List<ToolResult> toolResults,
        boolean          success,
        String           error,
        Duration         elapsed
) {
    /** True if the answer is non-empty and no error occurred. */
    public boolean isUsable() {
        return success && answer != null && !answer.isBlank();
    }

    public static OrchestratorResult ok(String answer, String strategy,
                                        int steps, List<ToolResult> tools, Duration elapsed) {
        return new OrchestratorResult(answer, strategy, steps, tools, true, null, elapsed);
    }

    public static OrchestratorResult failure(String strategy, String error, Duration elapsed) {
        return new OrchestratorResult("", strategy, 0, List.of(), false, error, elapsed);
    }

    @Override public String toString() {
        return String.format("OrchestratorResult[strategy=%s, steps=%d, success=%b, elapsed=%dms]",
                strategy, steps, success, elapsed.toMillis());
    }
}
