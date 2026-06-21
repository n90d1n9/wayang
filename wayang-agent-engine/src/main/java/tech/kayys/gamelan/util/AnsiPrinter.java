package tech.kayys.gamelan.util;

import tech.kayys.gamelan.agent.AgentResponse;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.PrintStream;

/**
 * Terminal output helper with ANSI colour support.
 *
 * <p>Colour is disabled automatically when:
 * <ul>
 *   <li>{@code NO_COLOR} environment variable is set (https://no-color.org/)</li>
 *   <li>{@code useColor=false} is passed</li>
 *   <li>stdout is not a terminal (piped output)</li>
 * </ul>
 *
 * <h2>agentFooter overloads</h2>
 * Two overloads exist:
 * <ul>
 *   <li>{@link #agentFooter(OrchestratorResult, long)} — used by the new
 *       three-tier orchestrator path (ChatCommand, RunCommand)</li>
 *   <li>{@link #agentFooter(AgentResponse, long)} — used by legacy callers
 *       (GamelanWorkflowEngine, WatchCommand during migration)</li>
 * </ul>
 */
public final class AnsiPrinter {

    private static final String RESET   = "\033[0m";
    private static final String BOLD    = "\033[1m";
    private static final String CYAN    = "\033[36m";
    private static final String GREEN   = "\033[32m";
    private static final String YELLOW  = "\033[33m";
    private static final String RED     = "\033[31m";
    private static final String MAGENTA = "\033[35m";
    private static final String GREY    = "\033[90m";

    private final boolean     color;
    private final PrintStream out;
    private final PrintStream err;

    public AnsiPrinter(boolean useColor) {
        this(useColor, System.out, System.err);
    }

    public AnsiPrinter(boolean useColor, PrintStream out, PrintStream err) {
        boolean tty  = System.console() != null;
        this.color   = useColor && tty && System.getenv("NO_COLOR") == null;
        this.out     = out;
        this.err     = err;
    }

    // ── Banner / headers ───────────────────────────────────────────────────

    public void banner() {
        if (color) {
            out.println(CYAN + BOLD
                    + "╔══════════════════════════════════════╗\n"
                    + "║  🎶  G A M E L A N   C L I  v1.0    ║\n"
                    + "║  Agentic AI · Powered by Gollek       ║\n"
                    + "╚══════════════════════════════════════╝" + RESET);
        } else {
            out.println("=== GAMELAN CLI v1.0 — Agentic AI powered by Gollek ===");
        }
    }

    public void sectionHeader(String title) {
        out.println();
        if (color) {
            out.println(BOLD + CYAN + title + RESET);
            out.println(GREY + "─".repeat(Math.min(title.length() + 2, 60)) + RESET);
        } else {
            out.println(title);
            out.println("─".repeat(Math.min(title.length() + 2, 60)));
        }
    }

    // ── Status messages ────────────────────────────────────────────────────

    public void info(String msg) {
        if (color) out.println(GREY + "  " + msg + RESET);
        else       out.println("  " + msg);
    }

    public void success(String msg) {
        if (color) out.println(GREEN + "✓ " + msg + RESET);
        else       out.println("[OK] " + msg);
    }

    public void warn(String msg) {
        if (color) out.println(YELLOW + "⚠ " + msg + RESET);
        else       out.println("[WARN] " + msg);
    }

    public void error(String msg) {
        if (color) err.println(RED + "✗ " + msg + RESET);
        else       err.println("[ERROR] " + msg);
    }

    // ── Content ────────────────────────────────────────────────────────────

    public void println(String text) {
        out.println(color ? applyTags(text) : stripTags(text));
    }

    public void println() { out.println(); }

    public void listItem(String name, String description) {
        if (color) {
            out.printf("  %s%-28s%s  %s%s%s%n",
                    BOLD + CYAN, name, RESET, GREY, description, RESET);
        } else {
            out.printf("  %-28s  %s%n", name, description);
        }
    }

    // ── REPL prompt ────────────────────────────────────────────────────────

    public String prompt(ConversationSession session) {
        String shortId = session.id().length() > 9
                ? session.id().substring(5, 9) : session.id();
        if (color) {
            return MAGENTA + BOLD + "gamelan" + RESET
                    + GREY + "[" + shortId + "]" + RESET
                    + CYAN + " ❯ " + RESET;
        } else {
            return "gamelan[" + shortId + "] > ";
        }
    }

    // ── Agent response footer ──────────────────────────────────────────────

    /**
     * Prints turn footer for the three-tier orchestrator path.
     * Shows: strategy, steps, tool count, elapsed time.
     */
    public void agentFooter(OrchestratorResult result, long elapsedMs) {
        if (result == null) return;

        StringBuilder meta = new StringBuilder();
        meta.append("strategy:").append(result.strategy());
        if (result.steps() > 0)
            meta.append("  steps:").append(result.steps());
        if (!result.toolResults().isEmpty())
            meta.append("  tools:").append(result.toolResults().size());
        if (elapsedMs > 0)
            meta.append(String.format("  %.1fs", elapsedMs / 1000.0));
        if (!result.success() && result.error() != null)
            meta.append("  [ERROR]");

        printFooterLine(meta.toString());
    }

    /**
     * Prints turn footer for legacy {@link AgentResponse} callers.
     * Used by GamelanWorkflowEngine and WatchCommand.
     */
    public void agentFooter(AgentResponse response, long elapsedMs) {
        if (response == null) return;

        StringBuilder meta = new StringBuilder();
        if (!response.skillsUsed().isEmpty())
            meta.append("skills:").append(String.join(",", response.skillsUsed()));
        if (!response.toolResults().isEmpty()) {
            if (!meta.isEmpty()) meta.append("  ");
            meta.append("tools:").append(response.toolResults().size());
        }
        if (elapsedMs > 0) {
            if (!meta.isEmpty()) meta.append("  ");
            meta.append(String.format("%.1fs", elapsedMs / 1000.0));
        }

        if (!meta.isEmpty()) printFooterLine(meta.toString());
    }

    /** Compat alias used by older callers (ModelCommand, SkillCommand). */
    public void agentResponse(AgentResponse response, boolean wasStreamed) {
        agentFooter(response, 0);
    }

    private void printFooterLine(String meta) {
        if (meta.isBlank()) return;
        if (color) {
            out.println("\n" + GREY + "─── " + meta + " ───" + RESET);
        } else {
            out.println("\n[" + meta + "]");
        }
    }

    // ── Tag processing ─────────────────────────────────────────────────────

    private String applyTags(String text) {
        return text
                .replace("@|bold,cyan ", BOLD + CYAN)
                .replace("@|bold ",      BOLD)
                .replace("@|cyan ",      CYAN)
                .replace("@|green ",     GREEN)
                .replace("@|red ",       RED)
                .replace("@|yellow ",    YELLOW)
                .replace("@|grey ",      GREY)
                .replace("|@",           RESET);
    }

    private String stripTags(String text) {
        return text.replaceAll("@\\|[\\w,]+ ", "").replace("|@", "");
    }
}
