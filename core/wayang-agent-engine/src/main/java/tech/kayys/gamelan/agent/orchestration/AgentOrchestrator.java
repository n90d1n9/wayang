package tech.kayys.gamelan.agent.orchestration;

/**
 * SPI for the three-tier agent architecture.
 *
 * <h2>Tier 1 — Direct model call</h2>
 * {@code strategy = "direct"}
 * A single LLM call with a crafted prompt. No tools. No loop.
 * Use for: classification, summarisation, translation, simple Q&A.
 * Implemented by: {@link DirectCallOrchestrator}
 *
 * <h2>Tier 2 — Single agent with tools</h2>
 * {@code strategy = "react" | "reflexion"}
 * One agent that reasons and acts via the ReAct (plan→tool→observe→repeat)
 * cycle. Supports a configurable iteration limit to guard against loops.
 * Use for: most coding tasks, file ops, search, debugging, refactoring.
 * Implemented by: {@link SingleAgentOrchestrator}, {@link ReflexionOrchestrator}
 *
 * <h2>Tier 3 — Multi-agent orchestration</h2>
 * {@code strategy = "multi-agent"}
 * Specialised sub-agents coordinated by an orchestrator agent.
 * Use for: cross-domain tasks, parallel specialisation, tasks that exceed
 * a single agent's reliable capability due to prompt complexity.
 * Implemented by: {@link MultiAgentOrchestrator}
 *
 * <h2>Selecting a tier</h2>
 * Use {@link OrchestratorSelector} to pick the right tier automatically,
 * or specify it explicitly with {@code --strategy} on the CLI.
 */
public interface AgentOrchestrator {

    /** Short identifier used in CLI flags and result metadata. */
    String strategyId();

    /**
     * Execute the orchestration and block until a result is produced.
     *
     * @param request the task and all configuration
     * @return the final result (never null; check {@code success} field)
     */
    OrchestratorResult execute(AgentRequest request);

    /**
     * Human-readable name shown in help text and status output.
     */
    default String displayName() { return strategyId(); }

    /**
     * Brief description shown in {@code gamelan run --strategy help}.
     */
    default String description() { return ""; }

    /**
     * Whether this orchestrator supports tool use.
     * Direct-call orchestrators return false; all others return true.
     */
    default boolean supportsTools() { return true; }
}
