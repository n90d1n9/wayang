package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;

/**
 * Real-time context window advisor — warns before the context fills up.
 *
 * <h2>Why this matters</h2>
 * Without context window awareness, agents fail silently when they hit the
 * limit: the LLM truncates history, gives worse answers, or returns errors.
 * Claude Code shows a visible progress bar. Gamelan now warns at configurable
 * thresholds so users can {@code /compact} proactively.
 *
 * <h2>Thresholds</h2>
 * <ul>
 *   <li>Warning:  70% of budget → print a soft warning in the REPL footer</li>
 *   <li>Critical: 90% of budget → print a prominent warning, suggest /compact</li>
 *   <li>Exceeded: 100%          → log and suggest immediate compaction</li>
 * </ul>
 */
@ApplicationScoped
public class TokenBudgetAdvisor {

    private static final double WARN_THRESHOLD     = 0.70;
    private static final double CRITICAL_THRESHOLD = 0.90;

    @Inject GamelanConfig config;

    public enum BudgetStatus { OK, WARNING, CRITICAL, EXCEEDED }

    /**
     * Returns the current budget status for a session.
     */
    public BudgetStatus status(ConversationSession session) {
        double ratio = ratio(session);
        if (ratio >= 1.0)  return BudgetStatus.EXCEEDED;
        if (ratio >= CRITICAL_THRESHOLD) return BudgetStatus.CRITICAL;
        if (ratio >= WARN_THRESHOLD)     return BudgetStatus.WARNING;
        return BudgetStatus.OK;
    }

    /** Returns token usage as a 0.0–1.0+ fraction of the configured budget. */
    public double ratio(ConversationSession session) {
        int budget = config.tokenBudget();
        if (budget <= 0) return 0.0;
        return (double) session.tokenCount() / budget;
    }

    /**
     * Returns a human-readable budget indicator for REPL footers.
     * Example: "ctx: ████░░░░ 72%"
     */
    public String indicator(ConversationSession session) {
        double r = Math.min(1.0, ratio(session));
        int pct  = (int) (r * 100);
        int filled = (int) (r * 8);
        String bar = "█".repeat(filled) + "░".repeat(8 - filled);
        String status = switch (status(session)) {
            case WARNING  -> " ⚠";
            case CRITICAL -> " ⚠⚠";
            case EXCEEDED -> " !!";
            default       -> "";
        };
        return String.format("ctx: %s %d%%%s", bar, pct, status);
    }

    /**
     * Returns an advisory message when the budget is being stressed.
     * Returns empty string if the session is comfortably within budget.
     */
    public String advisory(ConversationSession session) {
        return switch (status(session)) {
            case WARNING  -> "Context at " + (int)(ratio(session)*100) + "% — consider /compact soon";
            case CRITICAL -> "⚠ Context at " + (int)(ratio(session)*100) + "% — run /compact now";
            case EXCEEDED -> "!! Context exceeded budget — run /compact immediately";
            default       -> "";
        };
    }

    /** Raw token count from the session. */
    public int usedTokens(ConversationSession session) { return session.tokenCount(); }

    /** Configured token budget. */
    public int budget() { return config.tokenBudget(); }
}
