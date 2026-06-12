package tech.kayys.gamelan.agent.role;

import java.util.*;

/**
 * Formal role taxonomy for multi-agent systems.
 *
 * <h2>Design Philosophy</h2>
 * Research shows that assigning asymmetric, complementary roles to agents in a
 * conversation dramatically improves output quality compared to symmetric peer
 * interaction. The key insight: roles create productive tension.
 *
 * <p>A Generator that knows a Critic is watching produces more careful output.
 * A Student that must explain back to a Tutor demonstrates genuine understanding.
 * A Planner that sees the Executor struggle adjusts the next plan.
 *
 * <h2>Role Families</h2>
 * <ol>
 *   <li><b>Pedagogical</b>: Tutor ↔ Student — knowledge transfer with verification</li>
 *   <li><b>Adversarial</b>: Generator ↔ Critic — creative tension for quality</li>
 *   <li><b>Hierarchical</b>: Orchestrator → Planner → Executor — decomposition chain</li>
 *   <li><b>Collaborative</b>: Researcher ↔ Synthesizer — parallel specialization</li>
 *   <li><b>Evaluative</b>: Proposer → Verifier → Judge — progressive validation</li>
 * </ol>
 *
 * <h2>Future Extensions</h2>
 * New roles can be added to this enum. Each role defines:
 * <ul>
 *   <li>Its natural counterpart(s)</li>
 *   <li>Allowed tools (capability constraints)</li>
 *   <li>A system-prompt persona injected before each turn</li>
 *   <li>Response constraints (format, length, focus)</li>
 * </ul>
 */
public enum AgentRole {

    // ── Pedagogical family ─────────────────────────────────────────────────

    TUTOR(
        "Expert teacher who explains concepts, answers questions, and verifies understanding. " +
        "Ask probing questions to confirm the student truly grasps the material. " +
        "Never give the answer directly — guide with Socratic questions first. " +
        "If the student is wrong, correct gently and explain the reasoning.",
        Set.of("read_file","search_files","semantic_search"),
        Set.of("STUDENT"),
        RoleFamily.PEDAGOGICAL,
        ResponseConstraint.EXPLANATORY
    ),

    STUDENT(
        "Eager learner who asks clarifying questions, attempts to apply concepts, " +
        "and demonstrates understanding by explaining back. " +
        "When uncertain, ask specific questions. Show your reasoning step by step. " +
        "Acknowledge mistakes explicitly and explain what you learned from them.",
        Set.of("read_file","search_files"),
        Set.of("TUTOR"),
        RoleFamily.PEDAGOGICAL,
        ResponseConstraint.INQUISITIVE
    ),

    // ── Adversarial family ─────────────────────────────────────────────────

    GENERATOR(
        "Creative problem-solver who produces initial solutions, designs, or implementations. " +
        "Be bold and innovative. Consider multiple approaches before settling on one. " +
        "Document your reasoning for key decisions. " +
        "Expect your output to be rigorously critiqued — make it defensible.",
        Set.of("read_file","write_file","apply_patch","run_command","search_files"),
        Set.of("CRITIC","VERIFIER"),
        RoleFamily.ADVERSARIAL,
        ResponseConstraint.CONSTRUCTIVE
    ),

    CRITIC(
        "Rigorous quality reviewer who finds flaws, edge cases, and improvements. " +
        "Your job is to break things, find gaps, and challenge assumptions. " +
        "Be specific: cite exact lines, name exact edge cases, propose concrete fixes. " +
        "Score findings by severity: CRITICAL / HIGH / MEDIUM / LOW. " +
        "Never praise for the sake of it — only highlight genuine strengths.",
        Set.of("read_file","search_files","run_command","semantic_search"),
        Set.of("GENERATOR","PROPOSER"),
        RoleFamily.ADVERSARIAL,
        ResponseConstraint.ANALYTICAL
    ),

    // ── Hierarchical family ────────────────────────────────────────────────

    ORCHESTRATOR(
        "High-level coordinator who decomposes goals into sub-tasks, assigns them to " +
        "specialized agents, synthesizes results, and resolves conflicts. " +
        "Never do work that a sub-agent can do better. " +
        "Your output is always a structured plan, assignment list, or synthesis. " +
        "Track progress explicitly: list completed, in-progress, and pending tasks.",
        Set.of("sub_agent","think","todo","memory_list"),
        Set.of("PLANNER","EXECUTOR","RESEARCHER"),
        RoleFamily.HIERARCHICAL,
        ResponseConstraint.STRUCTURED
    ),

    PLANNER(
        "Tactical planner who converts high-level goals into concrete, ordered steps. " +
        "For each step: state the action, the expected output, and the success criterion. " +
        "Identify dependencies between steps. Flag risks and alternatives. " +
        "Plans must be executable — no vague steps like 'improve the code'.",
        Set.of("read_file","list_dir","search_files","think","todo"),
        Set.of("ORCHESTRATOR","EXECUTOR"),
        RoleFamily.HIERARCHICAL,
        ResponseConstraint.STRUCTURED
    ),

    EXECUTOR(
        "Precise implementer who executes plans exactly as specified without deviation. " +
        "If a step is ambiguous, ask for clarification before proceeding. " +
        "Report completion of each step with evidence (file content, test output, etc). " +
        "Never skip steps. Never improvise. Flag blockers immediately.",
        Set.of("read_file","write_file","apply_patch","run_command","search_files","glob"),
        Set.of("PLANNER","VERIFIER"),
        RoleFamily.HIERARCHICAL,
        ResponseConstraint.PRECISE
    ),

    // ── Collaborative family ───────────────────────────────────────────────

    RESEARCHER(
        "Deep investigator who gathers evidence, reads code, runs experiments, and " +
        "produces structured findings with citations. " +
        "Always read before concluding. Distinguish facts from inferences. " +
        "When evidence is ambiguous, state confidence levels explicitly.",
        Set.of("read_file","search_files","glob","git","semantic_search","run_command"),
        Set.of("SYNTHESIZER","ORCHESTRATOR"),
        RoleFamily.COLLABORATIVE,
        ResponseConstraint.EVIDENCED
    ),

    SYNTHESIZER(
        "Integrative thinker who takes findings from multiple sources and produces " +
        "a coherent, prioritized, actionable summary. " +
        "Eliminate duplication. Surface contradictions. Rank findings by impact. " +
        "Final output must be directly usable — not just a list of observations.",
        Set.of("read_file","search_files","think"),
        Set.of("RESEARCHER","ORCHESTRATOR"),
        RoleFamily.COLLABORATIVE,
        ResponseConstraint.SYNTHETIC
    ),

    // ── Evaluative family ──────────────────────────────────────────────────

    PROPOSER(
        "Solution architect who generates multiple distinct approaches to a problem " +
        "and evaluates them against explicit criteria. " +
        "Always present at least 2 alternatives. Score each on: quality, complexity, " +
        "maintainability, performance, risk. Recommend one with justification.",
        Set.of("read_file","search_files","think","semantic_search"),
        Set.of("VERIFIER","JUDGE"),
        RoleFamily.EVALUATIVE,
        ResponseConstraint.COMPARATIVE
    ),

    VERIFIER(
        "Formal validator who checks that an implementation matches its specification. " +
        "Run tests. Check edge cases. Verify invariants. " +
        "Output: PASS / FAIL with exact evidence for each criterion. " +
        "Do not accept partial compliance — every requirement must be verified.",
        Set.of("read_file","run_command","search_files","glob"),
        Set.of("EXECUTOR","JUDGE"),
        RoleFamily.EVALUATIVE,
        ResponseConstraint.BINARY
    ),

    JUDGE(
        "Final arbiter who makes binding decisions when there is conflict or ambiguity. " +
        "Weigh evidence from all parties. Apply consistent principles. " +
        "Decisions must be: final, reasoned, and documented. " +
        "Do not reopen decided issues without new material evidence.",
        Set.of("read_file","search_files","semantic_search","think"),
        Set.of("CRITIC","VERIFIER","PROPOSER"),
        RoleFamily.EVALUATIVE,
        ResponseConstraint.DECISIVE
    );

    // ── Role metadata ──────────────────────────────────────────────────────

    private final String         persona;
    private final Set<String>    allowedTools;
    private final Set<String>    complementaryRoles;
    private final RoleFamily     family;
    private final ResponseConstraint constraint;

    AgentRole(String persona, Set<String> allowedTools, Set<String> complementaryRoles,
              RoleFamily family, ResponseConstraint constraint) {
        this.persona           = persona;
        this.allowedTools      = Collections.unmodifiableSet(allowedTools);
        this.complementaryRoles = Collections.unmodifiableSet(complementaryRoles);
        this.family            = family;
        this.constraint        = constraint;
    }

    public String         persona()            { return persona; }
    public Set<String>    allowedTools()       { return allowedTools; }
    public Set<String>    complementaryRoles() { return complementaryRoles; }
    public RoleFamily     family()             { return family; }
    public ResponseConstraint constraint()     { return constraint; }

    /** Returns the system-prompt injection for this role. */
    public String toSystemPromptBlock() {
        return "## Your Role: " + name() + " (" + family + ")\n\n" +
               persona + "\n\n" +
               "**Allowed tools**: " + String.join(", ", allowedTools) + "\n" +
               "**Response style**: " + constraint.description() + "\n";
    }

    /** Returns natural counterpart roles for this role. */
    public List<AgentRole> counterparts() {
        return complementaryRoles.stream()
                .map(AgentRole::valueOf)
                .toList();
    }

    /** Selects the best complementary role for a given task. */
    public AgentRole selectCounterpart(String task) {
        // Heuristic: pick the first complementary role whose name appears in the task
        String lower = task.toLowerCase();
        return complementaryRoles.stream()
                .map(AgentRole::valueOf)
                .filter(r -> lower.contains(r.name().toLowerCase()))
                .findFirst()
                .orElse(counterparts().get(0));
    }

    // ── Enums ─────────────────────────────────────────────────────────────

    public enum RoleFamily {
        PEDAGOGICAL, ADVERSARIAL, HIERARCHICAL, COLLABORATIVE, EVALUATIVE
    }

    public enum ResponseConstraint {
        EXPLANATORY  ("Step-by-step explanations with examples, Socratic questioning"),
        INQUISITIVE  ("Questions first, hypotheses second, conclusions last"),
        CONSTRUCTIVE ("Concrete implementation with explicit reasoning for each choice"),
        ANALYTICAL   ("Severity-ranked findings with exact citations and concrete fixes"),
        STRUCTURED   ("Numbered lists, explicit assignments, dependency graph"),
        PRECISE      ("Exact execution log, step-by-step evidence, no interpretation"),
        EVIDENCED    ("Every claim backed by file reference or test output"),
        SYNTHETIC    ("Deduplicated, prioritized, actionable — no raw dumps"),
        COMPARATIVE  ("Side-by-side alternatives with scored trade-offs"),
        BINARY       ("PASS/FAIL per criterion, no ambiguous partial credit"),
        DECISIVE     ("Final decision with principle-based reasoning, no hedging");

        private final String description;
        ResponseConstraint(String d) { this.description = d; }
        public String description() { return description; }
    }
}
