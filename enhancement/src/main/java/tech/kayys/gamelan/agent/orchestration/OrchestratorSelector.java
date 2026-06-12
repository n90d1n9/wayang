package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects the appropriate orchestration tier for a given task.
 *
 * <h2>Decision logic</h2>
 * <pre>
 * explicit --strategy flag  →  honour it
 * task is short + no action words  →  Tier 1 (direct)
 * task involves files, code, tools →  Tier 2 (react)
 * task is cross-domain / "review all / analyse and fix"  →  Tier 3 (multi-agent)
 * </pre>
 *
 * <p>Heuristics are intentionally conservative: when in doubt, use Tier 2.
 * Multi-agent (Tier 3) is only selected when there are clear cross-domain
 * signals, because it is more expensive and harder to debug.
 */
@ApplicationScoped
public class OrchestratorSelector {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorSelector.class);

    @Inject DirectCallOrchestrator  direct;
    @Inject SingleAgentOrchestrator react;
    @Inject ReflexionOrchestrator   reflexion;
    @Inject MultiAgentOrchestrator  multiAgent;
    @Inject PipelineOrchestrator    pipeline;

    /**
     * Returns the orchestrator for the given strategy string.
     *
     * @param strategy explicit strategy name, or null/empty to auto-select
     * @param task     user's task (used for auto-selection heuristics)
     * @return the selected orchestrator
     */
    public AgentOrchestrator select(String strategy, String task) {
        if (strategy != null && !strategy.isBlank()) {
            return byName(strategy);
        }
        return autoSelect(task);
    }

    /** All available orchestrators, for help text generation. */
    public AgentOrchestrator[] all() {
        return new AgentOrchestrator[]{ direct, react, reflexion, multiAgent, pipeline };
    }

    // ── Auto-selection heuristics ──────────────────────────────────────────

    private AgentOrchestrator autoSelect(String task) {
        if (task == null || task.isBlank()) return react; // safe default

        String lower = task.toLowerCase();

        // Tier 1: direct — short, no action words, no file references
        if (isDirect(lower)) {
            log.debug("[selector] auto: direct (short, no action words)");
            return direct;
        }

        // Tier 3: multi-agent — cross-domain review signals
        if (isMultiAgent(lower)) {
            log.debug("[selector] auto: multi-agent (cross-domain signals)");
            return multiAgent;
        }

        // Tier 2: default — single agent with tools
        log.debug("[selector] auto: react (default single-agent)");
        return react;
    }

    private boolean isDirect(String lower) {
        // Must be SHORT (< 80 chars) and have no action words
        if (lower.length() > 80) return false;
        String[] actionWords = {
            "read", "write", "create", "delete", "edit", "fix", "run", "execute",
            "search", "find", "list", "refactor", "add", "remove", "update",
            "install", "build", "test", "commit", "push", "apply", "patch"
        };
        for (String w : actionWords) {
            if (lower.contains(w)) return false;
        }
        return true;
    }

    private boolean isMultiAgent(String lower) {
        // Cross-domain signals
        String[] multiSignals = {
            "review all", "analyse all", "analyze all", "audit",
            "full review", "comprehensive", "and security", "and performance",
            "and correctness", "and documentation", "and tests",
            "across all", "entire codebase", "full audit"
        };
        for (String s : multiSignals) {
            if (lower.contains(s)) return true;
        }
        return false;
    }

    private AgentOrchestrator byName(String name) {
        return switch (name.toLowerCase().strip()) {
            case "direct", "simple", "1" -> direct;
            case "react", "agent", "2"   -> react;
            case "reflexion", "reflect"  -> reflexion;
            case "multi", "multi-agent", "3" -> multiAgent;
            case "pipeline", "pipe", "4"   -> pipeline;
            default -> {
                log.warn("[selector] unknown strategy '{}', falling back to react", name);
                yield react;
            }
        };
    }
}
