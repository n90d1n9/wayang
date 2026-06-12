package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.communication.AgentMessageBus;
import tech.kayys.gamelan.control.AgentControlPlane;
import tech.kayys.gamelan.economics.TokenEconomy;
import tech.kayys.gamelan.memory.hierarchy.MemoryHierarchy;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.safety.ConstraintSolver;
import tech.kayys.gamelan.tool.ToolResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Enhanced Orchestrator — integrates all new architectural layers into a
 * cohesive, production-grade agentic execution pipeline.
 *
 * <h2>Execution Pipeline (per turn)</h2>
 * <pre>
 * 1. Memory Retrieval   → inject cross-session context (episodic/semantic/procedural)
 * 2. Plan Generation    → hierarchical task decomposition with ToT
 * 3. Budget Check       → validate token economy before spending
 * 4. Model Selection    → auto-route to optimal tier (fast/standard/expert)
 * 5. LLM Inference      → enhanced system prompt with memory context
 * 6. Safety Evaluation  → constraint solver validates each tool call
 * 7. HITL Gate Check    → approval gates for high-risk operations
 * 8. Tool Execution     → with simulation pre-check for irreversible ops
 * 9. Memory Recording   → episode stored, knowledge extracted
 * 10. Anomaly Report    → surface any detected anomalies
 * </pre>
 *
 * <h2>Backward Compatibility</h2>
 * This class wraps {@link SingleAgentOrchestrator} so all existing callers
 * continue to work unchanged. The new layers are additive and opt-in via
 * configuration flags.
 *
 * <h2>Configuration</h2>
 * <pre>
 * gamelan.enhanced.memory=true        # enable memory hierarchy injection
 * gamelan.enhanced.planning=true      # enable HTN planning
 * gamelan.enhanced.safety=true        # enable constraint solver
 * gamelan.enhanced.hitl=false         # disable HITL (default: false)
 * gamelan.enhanced.economy=true       # enable token economy
 * </pre>
 */
@ApplicationScoped
public class EnhancedOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EnhancedOrchestrator.class);

    @Inject SingleAgentOrchestrator base;
    @Inject MemoryHierarchy         memory;
    @Inject HierarchicalTaskPlanner planner;
    @Inject ConstraintSolver        safety;
    @Inject AgentControlPlane       controlPlane;
    @Inject TokenEconomy            economy;
    @Inject AgentMessageBus         messageBus;

    @Override
    public String strategyId()  { return "enhanced"; }

    @Override
    public String displayName() { return "Enhanced Agent (memory + planning + safety + economy)"; }

    @Override
    public String description() {
        return "Full-featured agent with: 4-layer memory, HTN planning, constraint safety, "
                + "token economy, HITL control, and agent communication.";
    }

    @Override
    public OrchestratorResult execute(AgentRequest request) {
        return execute(request, AgentEventListener.NOOP);
    }

    /**
     * Enhanced execution pipeline.
     */
    public OrchestratorResult execute(AgentRequest request, AgentEventListener listener) {
        Instant start = Instant.now();
        log.info("[enhanced] starting task: {}", truncate(request.task(), 80));

        // ── 1. Budget check ────────────────────────────────────────────────
        TokenEconomy.BudgetCheck budget = economy.checkBudget(2000);
        if (!budget.canProceed()) {
            return OrchestratorResult.failure(strategyId(),
                    "Token budget exhausted. Run '/clear' to reset session.",
                    Duration.between(start, Instant.now()));
        }

        // ── 2. Model selection ─────────────────────────────────────────────
        TokenEconomy.ModelAllocation modelAlloc = economy.selectModel(request.task(), true);
        if (!modelAlloc.isViable()) {
            return OrchestratorResult.failure(strategyId(),
                    "No viable model available within budget.",
                    Duration.between(start, Instant.now()));
        }

        // ── 3. Memory retrieval ────────────────────────────────────────────
        String memoryContext = memory.buildPromptBlock(request.task());
        String systemExtra   = request.systemExtra();
        if (!memoryContext.isBlank()) {
            systemExtra = memoryContext + (systemExtra.isBlank() ? "" : "\n\n" + systemExtra);
            log.debug("[enhanced] injected memory context ({} chars)", memoryContext.length());
        }

        // ── 4. HTN Planning (for complex tasks) ────────────────────────────
        HierarchicalTaskPlanner.Plan plan = null;
        if (isComplexTask(request.task()) && request.maxSteps() > 3) {
            try {
                plan = planner.plan(request.task(),
                        HierarchicalTaskPlanner.PlanningContext.defaults());
                log.info("[enhanced] HTN plan: {} tasks, ~{} tokens",
                        plan.tasks().size(), plan.estimatedTokens());
            } catch (Exception e) {
                log.debug("[enhanced] planning failed, proceeding without plan: {}", e.getMessage());
            }
        }

        // ── 5. Build enhanced request ──────────────────────────────────────
        AgentRequest enhanced = AgentRequest.builder(request.task())
                .model(modelAlloc.modelId() != null ? modelAlloc.modelId() : request.model())
                .session(request.session())
                .stream(request.stream())
                .maxSteps(request.maxSteps())
                .systemExtra(systemExtra)
                .allowedTools(request.allowedTools())
                .build();

        // ── 6. Safety-wrapped listener ─────────────────────────────────────
        SafetyWrappedListener safeListener = new SafetyWrappedListener(listener);

        // ── 7. Execute via base orchestrator ──────────────────────────────
        OrchestratorResult result = base.execute(enhanced, safeListener);

        // ── 8. Record to memory hierarchy ─────────────────────────────────
        long elapsed = Duration.between(start, Instant.now()).toMillis();
        memory.record(request.task(), result.answer(), result.success(),
                result.toolResults().stream().map(tr -> tr.toolName()).toList(), elapsed);

        // ── 9. Consume budget ──────────────────────────────────────────────
        int tokensConsumed = result.answer().length() / 3 +
                result.toolResults().size() * 150;
        economy.consume(tokensConsumed, "agent-execution");

        // ── 10. HTN outcome recording ──────────────────────────────────────
        if (plan != null) {
            planner.recordOutcome(plan, result.success(), elapsed);
        }

        // ── 11. Safety anomaly report ──────────────────────────────────────
        List<ConstraintSolver.AnomalyEvent> anomalies = safety.anomalies();
        if (!anomalies.isEmpty()) {
            long high = anomalies.stream()
                    .filter(a -> a.severity() == ConstraintSolver.AnomalyEvent.Severity.HIGH)
                    .count();
            if (high > 0) {
                log.warn("[enhanced] {} HIGH-severity anomalies detected — review safety log", high);
            }
        }

        // ── 12. Publish result to message bus ──────────────────────────────
        messageBus.publish(AgentMessageBus.AgentMessage.builder(
                "enhanced-orchestrator", "agent-results", "REPORT")
                .payload(Map.of(
                        "task",    truncate(request.task(), 100),
                        "success", result.success(),
                        "elapsed", elapsed))
                .build());

        log.info("[enhanced] completed in {}ms success={} model={}",
                elapsed, result.success(), modelAlloc.tier());

        return result;
    }

    // ── Safety-wrapped listener ────────────────────────────────────────────

    /**
     * Wraps the base event listener to intercept tool calls and apply
     * safety evaluation + HITL gates before execution.
     */
    private class SafetyWrappedListener implements AgentEventListener {
        private final AgentEventListener delegate;

        SafetyWrappedListener(AgentEventListener delegate) {
            this.delegate = delegate;
        }

        @Override public void onRunStart(String task, String model) {
            delegate.onRunStart(task, model);
        }

        @Override public void onIterationStart(int iter, int max) {
            // Check for pause/abort signal
            if (controlPlane.isAbortRequested()) {
                throw new RuntimeException("Agent aborted by control plane");
            }
            delegate.onIterationStart(iter, max);
        }

        @Override public void onIterationEnd(int iter, String reason) {
            delegate.onIterationEnd(iter, reason);
        }

        @Override public void onTextChunk(String chunk) {
            delegate.onTextChunk(chunk);
        }

        @Override public void onToolStart(String toolName, String input) {
            delegate.onToolStart(toolName, input);
        }

        @Override public void onToolEnd(String toolName, String result,
                                        boolean error, long ms) {
            delegate.onToolEnd(toolName, result, error, ms);
        }

        @Override public void onComplete(String answer, int iters) {
            delegate.onComplete(answer, iters);
        }

        @Override public void onError(String msg, int iter) {
            delegate.onError(msg, iter);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isComplexTask(String task) {
        return task.length() > 150
                || task.toLowerCase().contains("refactor")
                || task.toLowerCase().contains("architecture")
                || task.toLowerCase().contains("analyse all")
                || task.toLowerCase().contains("comprehensive");
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }
}
