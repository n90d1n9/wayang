package tech.kayys.gamelan.context.reminders;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * SystemReminderEngine — event-driven behavioral guidance injected at the point of decision.
 *
 * <h2>From the OPENDEV paper (§2.3.4)</h2>
 * System prompt influence decays as conversations grow. Instructions that reliably govern the
 * agent's first few turns are routinely violated after 30 or more tool calls, when the instruction
 * is far from the model's attention window and buried under dozens of tool results. The root cause
 * is simple: the system prompt sits at the very beginning of the conversation. As the conversation
 * grows longer, the model's attention shifts toward recent messages and away from that initial
 * block of instructions.
 *
 * <p>OPENDEV's solution: short, single-purpose messages injected <em>exactly when the agent needs them</em>,
 * right before the decision point where it would otherwise go wrong.
 *
 * <h2>Key design decisions from the paper</h2>
 * <ul>
 *   <li><b>role: user, not role: system</b> — user-role reminders appear at maximum recency.
 *       The model treats them as "something that just happened," not background noise.</li>
 *   <li><b>Guardrail counters</b> — each reminder type fires at most N times; a reminder that
 *       fires every iteration becomes noise the model learns to ignore.</li>
 *   <li><b>Targeted templates</b> — 6 error-specific recovery templates outperform generic
 *       "try again" by telling the model specifically what failed and what to do.</li>
 *   <li><b>Graceful degradation</b> — if a template is missing, the agent continues with the
 *       system prompt. Reminders reinforce; they don't introduce new rules.</li>
 * </ul>
 *
 * <h2>Eight event detectors (from paper Figure 12)</h2>
 * <ol>
 *   <li>Tool failure without retry</li>
 *   <li>Exploration spirals (5+ consecutive reads)</li>
 *   <li>Denied tool re-attempts</li>
 *   <li>Premature completion with incomplete todos</li>
 *   <li>Continued work after all todos are done</li>
 *   <li>Plan approval without follow-through</li>
 *   <li>Unprocessed subagent results</li>
 *   <li>Empty completion messages</li>
 * </ol>
 */
@ApplicationScoped
public class SystemReminderEngine {

    private static final Logger log = LoggerFactory.getLogger(SystemReminderEngine.class);

    // Guardrail caps (paper §2.3.4 — MAX_TODO_NUDGES=2, MAX_NUDGE_ATTEMPTS=3)
    private static final int MAX_TODO_NUDGES    = 2;
    private static final int MAX_ERROR_NUDGES   = 3;
    private static final int MAX_EXPLORE_NUDGES = 1;

    @Inject AgentTelemetry telemetry;

    // Per-session state (reset on clear)
    private final AtomicInteger todoNudgeCount      = new AtomicInteger(0);
    private final AtomicInteger errorNudgeCount     = new AtomicInteger(0);
    private final AtomicInteger exploreNudgeCount   = new AtomicInteger(0);
    private final AtomicBoolean planApprovedFired   = new AtomicBoolean(false);
    private final AtomicBoolean allDoneNudgeFired   = new AtomicBoolean(false);
    private final AtomicBoolean completionFired     = new AtomicBoolean(false);
    private final AtomicInteger consecutiveReads    = new AtomicInteger(0);
    private final AtomicInteger consecutiveErrors   = new AtomicInteger(0);
    private volatile String     lastError           = "";

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Evaluates all event detectors against the current agent state and returns
     * zero or more reminders to inject before the next LLM call.
     *
     * <p>Reminders are returned as {@code role: user} messages for maximum salience.
     *
     * @param event the current agent state snapshot
     * @return list of reminder messages to inject (may be empty)
     */
    public List<ConversationMessage> evaluate(ReminderEvent event) {
        List<ConversationMessage> reminders = new ArrayList<>();

        // Detector 1: Tool failure without retry
        if (event.lastToolFailed() && !event.lastToolCallPresent() &&
                consecutiveErrors.incrementAndGet() <= MAX_ERROR_NUDGES) {
            String recovery = errorRecoveryTemplate(event.lastErrorMessage());
            reminders.add(remind(recovery));
            telemetry.count("reminder.error_recovery");
            log.debug("[reminder] injecting error recovery (attempt {}/{})",
                    consecutiveErrors.get(), MAX_ERROR_NUDGES);
        } else if (!event.lastToolFailed()) {
            consecutiveErrors.set(0);
            lastError = "";
        }

        // Detector 2: Exploration spiral (5+ consecutive reads, no writes/tool output)
        if (event.consecutiveReadOps() >= 5 && exploreNudgeCount.getAndIncrement() < MAX_EXPLORE_NUDGES) {
            reminders.add(remind(EXPLORE_SPIRAL));
            telemetry.count("reminder.explore_spiral");
            log.debug("[reminder] injecting exploration spiral nudge");
        }

        // Detector 3: Denied tool re-attempt
        if (event.lastToolDenied()) {
            reminders.add(remind(TOOL_DENIED));
            telemetry.count("reminder.tool_denied");
        }

        // Detector 4: Premature completion with incomplete todos
        if (event.agentSignalledComplete() && event.incompleteTodoCount() > 0 &&
                todoNudgeCount.getAndIncrement() < MAX_TODO_NUDGES) {
            String msg = INCOMPLETE_TODOS.formatted(
                    event.incompleteTodoCount(),
                    event.incompleteTodoTitles().stream()
                            .limit(5).map(t -> "- " + t)
                            .reduce("", (a, b) -> a + "\n" + b).strip());
            reminders.add(remind(msg));
            telemetry.count("reminder.incomplete_todos");
            log.debug("[reminder] incomplete todos: {}", event.incompleteTodoCount());
        }

        // Detector 5: All todos done — signal finalization
        if (event.allTodosComplete() && !allDoneNudgeFired.getAndSet(true)) {
            reminders.add(remind(ALL_TODOS_DONE));
            telemetry.count("reminder.all_todos_complete");
        }

        // Detector 6: Plan approved but no action taken yet
        if (event.planJustApproved() && !planApprovedFired.getAndSet(true)) {
            reminders.add(remind(PLAN_APPROVED.formatted(event.planFilePath())));
            telemetry.count("reminder.plan_approved");
        }

        // Detector 7: Subagent returned results not yet synthesized
        if (event.unprocessedSubagentResult() && !event.lastToolCallPresent()) {
            reminders.add(remind(SUBAGENT_RESULTS));
            telemetry.count("reminder.subagent_results");
        }

        // Detector 8: Empty completion message
        if (event.agentSignalledComplete() && event.completionMessage().isBlank() &&
                !completionFired.getAndSet(true)) {
            reminders.add(remind(EMPTY_COMPLETION));
            telemetry.count("reminder.empty_completion");
        }

        return reminders;
    }

    /** Called when the agent successfully makes a tool call. */
    public void recordToolCall(String toolName) {
        if (isReadOnlyTool(toolName)) {
            consecutiveReads.incrementAndGet();
        } else {
            consecutiveReads.set(0);
        }
        consecutiveErrors.set(0);
    }

    /** Resets all per-session counters (call on /clear). */
    public void reset() {
        todoNudgeCount.set(0);
        errorNudgeCount.set(0);
        exploreNudgeCount.set(0);
        planApprovedFired.set(false);
        allDoneNudgeFired.set(false);
        completionFired.set(false);
        consecutiveReads.set(0);
        consecutiveErrors.set(0);
        lastError = "";
    }

    // ── Error recovery template selection ─────────────────────────────────

    private String errorRecoveryTemplate(String errorMsg) {
        if (errorMsg == null || errorMsg.isBlank()) return GENERIC_ERROR;
        String lower = errorMsg.toLowerCase();
        if (lower.contains("permission") || lower.contains("access denied"))
            return ERROR_PERMISSION;
        if (lower.contains("not found") || lower.contains("no such file"))
            return ERROR_NOT_FOUND;
        if (lower.contains("not found in file") || lower.contains("old_content"))
            return ERROR_EDIT_MISMATCH;
        if (lower.contains("syntax") || lower.contains("compilation"))
            return ERROR_SYNTAX;
        if (lower.contains("rate limit") || lower.contains("429"))
            return ERROR_RATE_LIMIT;
        if (lower.contains("timeout") || lower.contains("timed out"))
            return ERROR_TIMEOUT;
        return GENERIC_ERROR;
    }

    private boolean isReadOnlyTool(String tool) {
        return Set.of("read_file", "list_dir", "glob", "search_files", "git").contains(tool);
    }

    private ConversationMessage remind(String content) {
        // Inject as role: user — paper §2.3.4: user-role reminders consistently produced
        // stronger compliance; the model treats them as recent input, not background noise.
        return ConversationMessage.user("<system-reminder>\n" + content + "\n</system-reminder>");
    }

    // ── Reminder templates (paper §F — Reminder Catalog) ──────────────────

    private static final String INCOMPLETE_TODOS = """
            You have %d incomplete todo item(s). Do NOT call task_complete until all are done.
            Remaining:
            %s
            Continue working through the list.""";

    private static final String ALL_TODOS_DONE = """
            All todos are marked complete. You may now call task_complete with a brief summary
            of what was accomplished.""";

    private static final String PLAN_APPROVED = """
            The plan at `%s` has been approved. Extract the steps as todos with write_todos,
            then work through them in order. Do NOT ask for additional approval — proceed.""";

    private static final String SUBAGENT_RESULTS = """
            A subagent returned results. Synthesize those results into your response before
            calling task_complete. Do not summarize each agent separately — merge the findings
            into a single coherent answer.""";

    private static final String EMPTY_COMPLETION = """
            Your completion message is empty. Provide a brief summary (1–3 sentences) of what
            was accomplished before signalling done.""";

    private static final String EXPLORE_SPIRAL = """
            You have made 5+ consecutive read operations without writing or acting. If you have
            gathered enough context, stop exploring and proceed with the task. If you are stuck,
            try a different search or ask for clarification.""";

    private static final String TOOL_DENIED = """
            Your last tool call was denied. Do NOT retry the same operation. Either find an
            alternative approach or ask the user how to proceed.""";

    // Error-specific recovery templates (paper §2.3.5 — 6 classified types)
    private static final String GENERIC_ERROR = """
            Your last tool call failed. Read the error message carefully, identify the root cause,
            and apply a targeted fix. Do NOT retry the exact same call without changes.""";

    private static final String ERROR_PERMISSION = """
            Error: Permission denied. You lack write access to that path. Check file permissions
            with `run_command("ls -la <path>")` or try an alternative path.""";

    private static final String ERROR_NOT_FOUND = """
            Error: File not found. The path you specified does not exist.
            Use `list_dir` or `glob` to find the correct path before retrying.""";

    private static final String ERROR_EDIT_MISMATCH = """
            Error: old_content not found in file. The file has changed since you last read it,
            or your memory of the content is incorrect.
            Re-read the file with `read_file`, identify the exact current content, and retry.""";

    private static final String ERROR_SYNTAX = """
            Error: Syntax / compilation error. Read the error location carefully.
            Fix the specific line indicated, then re-run to verify.""";

    private static final String ERROR_RATE_LIMIT = """
            Error: Rate limit hit. Wait a moment before retrying. If this persists,
            reduce the number of concurrent tool calls.""";

    private static final String ERROR_TIMEOUT = """
            Error: Operation timed out. The command ran too long.
            Consider running it in the background or breaking it into smaller steps.""";

    // ── Event snapshot ─────────────────────────────────────────────────────

    /**
     * Immutable snapshot of the agent state, evaluated by all detectors.
     * Build one at each ReAct iteration boundary.
     */
    public record ReminderEvent(
            boolean      lastToolFailed,
            boolean      lastToolCallPresent,
            boolean      lastToolDenied,
            String       lastErrorMessage,
            int          consecutiveReadOps,
            boolean      agentSignalledComplete,
            int          incompleteTodoCount,
            List<String> incompleteTodoTitles,
            boolean      allTodosComplete,
            boolean      planJustApproved,
            String       planFilePath,
            boolean      unprocessedSubagentResult,
            String       completionMessage
    ) {
        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private boolean      lastToolFailed           = false;
            private boolean      lastToolCallPresent      = false;
            private boolean      lastToolDenied           = false;
            private String       lastErrorMessage         = "";
            private int          consecutiveReadOps       = 0;
            private boolean      agentSignalledComplete   = false;
            private int          incompleteTodoCount      = 0;
            private List<String> incompleteTodoTitles     = List.of();
            private boolean      allTodosComplete         = false;
            private boolean      planJustApproved         = false;
            private String       planFilePath             = "";
            private boolean      unprocessedSubagentResult = false;
            private String       completionMessage        = "";

            public Builder toolFailed(boolean v, String error) {
                this.lastToolFailed = v; this.lastErrorMessage = error; return this; }
            public Builder toolCallPresent(boolean v)     { lastToolCallPresent = v; return this; }
            public Builder toolDenied(boolean v)          { lastToolDenied = v; return this; }
            public Builder consecutiveReads(int v)        { consecutiveReadOps = v; return this; }
            public Builder agentComplete(boolean v, String msg) {
                agentSignalledComplete = v; completionMessage = msg; return this; }
            public Builder incompleteTodos(int count, List<String> titles) {
                incompleteTodoCount = count; incompleteTodoTitles = titles; return this; }
            public Builder allTodosDone(boolean v)        { allTodosComplete = v; return this; }
            public Builder planApproved(boolean v, String path) {
                planJustApproved = v; planFilePath = path; return this; }
            public Builder subagentResult(boolean v)      { unprocessedSubagentResult = v; return this; }

            public ReminderEvent build() {
                return new ReminderEvent(lastToolFailed, lastToolCallPresent, lastToolDenied,
                        lastErrorMessage, consecutiveReadOps, agentSignalledComplete,
                        incompleteTodoCount, incompleteTodoTitles, allTodosComplete,
                        planJustApproved, planFilePath, unprocessedSubagentResult, completionMessage);
            }
        }
    }
}
