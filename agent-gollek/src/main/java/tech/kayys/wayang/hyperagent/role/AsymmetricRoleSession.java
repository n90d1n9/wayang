package tech.kayys.gamelan.hyperagent.role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.role.AgentRole;
import tech.kayys.gamelan.communication.AgentMessageBus;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * AsymmetricRoleSession — structured dialogue between agents with complementary roles.
 *
 * <h2>The Core Insight</h2>
 * A single agent asked to "review your own code" produces shallow self-critique.
 * Two agents in asymmetric roles — one GENERATOR who wrote the code, one CRITIC
 * who must find flaws — produce dramatically higher-quality output because:
 * <ol>
 *   <li>The Generator defends decisions → makes them more explicit and reasoned</li>
 *   <li>The Critic can't approve without finding something → maintains rigor</li>
 *   <li>Multiple rounds converge on a genuinely better solution</li>
 * </ol>
 *
 * <h2>Session Patterns</h2>
 *
 * <h3>Generator-Critic (adversarial improvement)</h3>
 * <pre>
 * Round 1: Generator produces initial solution
 * Round 2: Critic reviews and lists specific issues with severity
 * Round 3: Generator revises addressing all CRITICAL + HIGH issues
 * Round 4: Critic re-reviews and confirms resolution
 * [repeat until Critic reports PASS or max rounds reached]
 * </pre>
 *
 * <h3>Tutor-Student (knowledge transfer with verification)</h3>
 * <pre>
 * Round 1: Student states understanding attempt
 * Round 2: Tutor identifies gaps and asks probing questions
 * Round 3: Student answers and deepens explanation
 * Round 4: Tutor presents edge cases / counterexamples
 * [repeat until Tutor assesses MASTERY or max rounds reached]
 * </pre>
 *
 * <h3>Planner-Executor-Verifier (hierarchical execution)</h3>
 * <pre>
 * Phase 1: Planner decomposes task into numbered steps
 * Phase 2: Executor implements each step and reports completion
 * Phase 3: Verifier checks each step against acceptance criteria
 * [Planner adjusts plan if blockers are reported]
 * </pre>
 *
 * <h2>Convergence Detection</h2>
 * The session monitors for convergence signals:
 * <ul>
 *   <li>CRITIC produces "PASS" or "no issues found"</li>
 *   <li>VERIFIER produces "all criteria met"</li>
 *   <li>TUTOR produces "mastery confirmed"</li>
 *   <li>JUDGE produces a final decision</li>
 * </ul>
 */
public final class AsymmetricRoleSession {

    private static final Logger log = LoggerFactory.getLogger(AsymmetricRoleSession.class);

    private static final Set<String> CONVERGENCE_SIGNALS = Set.of(
            "pass", "no issues", "all criteria met", "mastery confirmed",
            "no critical", "lgtm", "approved", "complete", "resolved",
            "no further", "nothing to add", "satisfied");

    private final String               sessionId;
    private final RoleAgent            agentA;
    private final RoleAgent            agentB;
    private final int                  maxRounds;
    private final boolean              parallel;
    private final AgentMessageBus      bus;
    private final BiConsumer<RoleAgent, RoleAgent.TurnResult> onRound;

    private final List<DialogueRound>  rounds = new ArrayList<>();

    private AsymmetricRoleSession(Builder b) {
        this.sessionId = b.sessionId;
        this.agentA    = b.agentA;
        this.agentB    = b.agentB;
        this.maxRounds = b.maxRounds;
        this.parallel  = b.parallel;
        this.bus       = b.bus;
        this.onRound   = b.onRound;
    }

    public static Builder builder(RoleAgent agentA, RoleAgent agentB) {
        return new Builder(agentA, agentB);
    }

    // ── Session execution ──────────────────────────────────────────────────

    /**
     * Runs the full asymmetric dialogue to convergence or max rounds.
     *
     * @param initialTask the task or topic for this session
     * @return the session result containing all rounds and the final synthesis
     */
    public SessionResult run(String initialTask) {
        log.info("[role-session] {} starting: {} ↔ {} task='{}'",
                sessionId, agentA.role(), agentB.role(), truncate(initialTask, 60));
        Instant start = Instant.now();

        String currentContext = initialTask;
        boolean converged = false;
        String convergenceReason = null;

        for (int round = 1; round <= maxRounds; round++) {
            log.debug("[role-session] round {}/{}", round, maxRounds);

            // Agent A turn
            RoleAgent.TurnResult resultA = agentA.turn(buildTurnInput(
                    agentA.role(), round, currentContext,
                    round > 1 ? rounds.get(rounds.size()-1).resultB().response() : null));

            if (!resultA.success()) {
                return SessionResult.failed(sessionId, agentA, agentB,
                        rounds, "Agent A failed: " + resultA.error(),
                        Duration.between(start, Instant.now()));
            }
            if (onRound != null) onRound.accept(agentA, resultA);

            // Check convergence after A's turn
            if (detectConvergence(agentA.role(), resultA.response())) {
                converged = true;
                convergenceReason = agentA.role() + " signalled completion in round " + round;
                rounds.add(new DialogueRound(round, resultA,
                        RoleAgent.TurnResult.failed(agentB.agentId(), agentB.name(),
                                agentB.role(), "", "Session converged before B's turn", Duration.ZERO),
                        Instant.now()));
                break;
            }

            // Agent B turn (reads A's response)
            RoleAgent.TurnResult resultB = agentB.turn(buildTurnInput(
                    agentB.role(), round, currentContext, resultA.response()));

            if (!resultB.success()) {
                return SessionResult.failed(sessionId, agentA, agentB,
                        rounds, "Agent B failed: " + resultB.error(),
                        Duration.between(start, Instant.now()));
            }
            if (onRound != null) onRound.accept(agentB, resultB);

            rounds.add(new DialogueRound(round, resultA, resultB, Instant.now()));

            // Update context for next round
            currentContext = summarizeRound(resultA, resultB);

            // Check convergence after B's turn
            if (detectConvergence(agentB.role(), resultB.response())) {
                converged = true;
                convergenceReason = agentB.role() + " signalled completion in round " + round;
                break;
            }

            log.debug("[role-session] round {} complete — no convergence yet", round);
        }

        if (!converged) {
            convergenceReason = "Max rounds (" + maxRounds + ") reached";
        }

        // Synthesize final answer
        String synthesis = synthesize();

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[role-session] {} complete: {} rounds, converged={}, {}ms",
                sessionId, rounds.size(), converged, elapsed.toMillis());

        return new SessionResult(sessionId, agentA, agentB, rounds,
                synthesis, converged, convergenceReason, elapsed);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String buildTurnInput(AgentRole role, int round, String baseContext, String priorResponse) {
        StringBuilder sb = new StringBuilder();
        if (round == 1) {
            sb.append("Task: ").append(baseContext);
        } else {
            sb.append("Round ").append(round).append(" context:\n").append(baseContext).append("\n\n");
            if (priorResponse != null && !priorResponse.isBlank()) {
                sb.append("Previous agent's response:\n").append(
                        priorResponse.length() > 2000 ? priorResponse.substring(0,2000)+"…" : priorResponse);
            }
        }
        // Add role-specific instruction for the round
        sb.append("\n\n").append(getRoundInstruction(role, round));
        return sb.toString();
    }

    private String getRoundInstruction(AgentRole role, int round) {
        return switch (role) {
            case CRITIC     -> round == 1 ? "Review the task and identify initial concerns."
                                         : "Review the latest response. List all remaining issues by severity (CRITICAL/HIGH/MEDIUM/LOW). If no CRITICAL or HIGH issues remain, say PASS.";
            case GENERATOR  -> round == 1 ? "Produce your initial solution."
                                         : "Address all CRITICAL and HIGH issues identified by the Critic. Show exactly what changed and why.";
            case VERIFIER   -> round == 1 ? "Verify the implementation against the specification."
                                         : "Re-verify. Confirm which issues have been resolved. List any remaining failures. If all criteria are met, say PASS.";
            case STUDENT    -> round == 1 ? "State your current understanding of the topic."
                                         : "Answer the Tutor's questions and deepen your explanation.";
            case TUTOR      -> round == 1 ? "Identify gaps in the student's understanding and ask targeted questions."
                                         : "Evaluate the student's answers. Introduce edge cases or counterexamples. If mastery is confirmed, say MASTERY CONFIRMED.";
            case EXECUTOR   -> round == 1 ? "Execute step 1 of the plan and report completion with evidence."
                                         : "Execute the next step and report. Flag any blockers immediately.";
            case PLANNER    -> round == 1 ? "Create a numbered, ordered execution plan."
                                         : "Review execution results. Adjust the plan if needed. Confirm next steps.";
            default         -> round == 1 ? "Begin your analysis." : "Continue based on the prior exchange.";
        };
    }

    private boolean detectConvergence(AgentRole role, String response) {
        if (response == null || response.isBlank()) return false;
        String lower = response.toLowerCase();
        // Role-specific convergence signals
        return switch (role) {
            case CRITIC, VERIFIER -> lower.contains("pass") || lower.contains("no critical") ||
                                      lower.contains("all criteria") || lower.contains("lgtm");
            case TUTOR            -> lower.contains("mastery confirmed") || lower.contains("well done") ||
                                      lower.contains("correct understanding");
            case JUDGE            -> lower.contains("decision:") || lower.contains("ruling:") ||
                                      lower.contains("final answer:");
            default               -> CONVERGENCE_SIGNALS.stream().anyMatch(lower::contains);
        };
    }

    private String summarizeRound(RoleAgent.TurnResult a, RoleAgent.TurnResult b) {
        return "Agent A (" + a.role() + ") said:\n" +
               truncate(a.response(), 1000) + "\n\n" +
               "Agent B (" + b.role() + ") said:\n" +
               truncate(b.response(), 1000);
    }

    private String synthesize() {
        if (rounds.isEmpty()) return "";
        // Final answer: take the last agent's response that produced actual content
        for (int i = rounds.size()-1; i >= 0; i--) {
            DialogueRound r = rounds.get(i);
            if (r.resultB().success() && !r.resultB().response().isBlank()) {
                return r.resultB().response();
            }
            if (r.resultA().success() && !r.resultA().response().isBlank()) {
                return r.resultA().response();
            }
        }
        return "";
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record DialogueRound(
            int                     roundNumber,
            RoleAgent.TurnResult    resultA,
            RoleAgent.TurnResult    resultB,
            Instant                 completedAt
    ) {}

    public record SessionResult(
            String                  sessionId,
            RoleAgent               agentA,
            RoleAgent               agentB,
            List<DialogueRound>     rounds,
            String                  synthesis,
            boolean                 converged,
            String                  convergenceReason,
            Duration                elapsed
    ) {
        static SessionResult failed(String id, RoleAgent a, RoleAgent b,
                                     List<DialogueRound> r, String reason, Duration d) {
            return new SessionResult(id, a, b, r, "", false, reason, d);
        }

        public boolean success()    { return !synthesis.isBlank(); }
        public int totalRounds()    { return rounds.size(); }
        public long totalToolCalls() {
            return rounds.stream()
                    .mapToLong(r -> r.resultA().toolExecutions().size() +
                            r.resultB().toolExecutions().size()).sum();
        }

        public String summary() {
            return String.format("Session %s: %d rounds | converged=%b (%s) | %dms | tools=%d",
                    sessionId, totalRounds(), converged, convergenceReason,
                    elapsed.toMillis(), totalToolCalls());
        }
    }

    // ── Builder ────────────────────────────────────────────────────────────

    public static final class Builder {
        private final RoleAgent agentA, agentB;
        private String          sessionId  = UUID.randomUUID().toString();
        private int             maxRounds  = 6;
        private boolean         parallel   = false;
        private AgentMessageBus bus;
        private BiConsumer<RoleAgent, RoleAgent.TurnResult> onRound;

        Builder(RoleAgent a, RoleAgent b) { this.agentA = a; this.agentB = b; }
        public Builder sessionId(String s) { this.sessionId = s;  return this; }
        public Builder maxRounds(int n)    { this.maxRounds = n;  return this; }
        public Builder parallel(boolean p) { this.parallel  = p;  return this; }
        public Builder bus(AgentMessageBus b) { this.bus    = b;  return this; }
        public Builder onRound(BiConsumer<RoleAgent, RoleAgent.TurnResult> cb) { this.onRound = cb; return this; }
        public AsymmetricRoleSession build() { return new AsymmetricRoleSession(this); }
    }
}
