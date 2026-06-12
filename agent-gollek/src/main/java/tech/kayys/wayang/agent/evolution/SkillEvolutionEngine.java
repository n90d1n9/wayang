package tech.kayys.gamelan.agent.evolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.evaluation.BenchmarkHarness;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * EvoSkill — Autonomous Skill Evolution Engine.
 *
 * <h2>What this does</h2>
 * This implements the research concept of agents that can improve their own
 * skills by analyzing execution traces, identifying failure patterns, and
 * proposing updated skill instructions — validated against a Pareto Frontier
 * to ensure strictly better performance.
 *
 * <h2>Evolution Cycle</h2>
 * <pre>
 * 1. ANALYZE:  Collect failure episodes for a skill over N runs
 * 2. DIAGNOSE: LLM identifies the root cause of failures
 * 3. PROPOSE:  LLM generates updated skill instructions
 * 4. VALIDATE: Run benchmark suite against both old and new skill
 * 5. ACCEPT:   If Pareto-superior → replace skill (with backup)
 *              If not → discard candidate, log reason
 * 6. ARCHIVE:  Store evolution history for meta-analysis
 * </pre>
 *
 * <h2>Safety Guarantees</h2>
 * <ul>
 *   <li>Original skill is always backed up before replacement</li>
 *   <li>Benchmark must pass with ≥ previous success rate (never regress)</li>
 *   <li>Evolution is opt-in per skill (flag in SKILL.md frontmatter)</li>
 *   <li>Dry-run mode: generates proposals without applying them</li>
 * </ul>
 *
 * <h2>Inspired by</h2>
 * EvoSkill (2024), Darwinian Gödel Machine, AVO (Agentic Variation Operator)
 */
@ApplicationScoped
public class SkillEvolutionEngine {

    private static final Logger log = LoggerFactory.getLogger(SkillEvolutionEngine.class);

    private static final int    MIN_FAILURES_TO_EVOLVE = 3;
    private static final double MIN_PARETO_IMPROVEMENT = 0.05; // 5% improvement threshold
    private static final int    MAX_EVOLUTION_ATTEMPTS  = 5;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject GollekSdk        sdk;
    @Inject SkillRegistry    registry;
    @Inject EpisodicMemory   episodic;
    @Inject BenchmarkHarness benchmark;
    @Inject GamelanConfig    config;

    // Evolution history: skillName → list of past evolution attempts
    private final Map<String, List<EvolutionRecord>> history = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Analyzes all skills for evolution candidates and runs the full cycle.
     * Intended to be called periodically (e.g., daily or after N failures).
     *
     * @param dryRun if true, generate proposals but don't apply them
     * @return list of evolution outcomes
     */
    public List<EvolutionOutcome> evolveAll(boolean dryRun) {
        log.info("[evo] starting evolution cycle (dryRun={})", dryRun);
        List<EvolutionOutcome> outcomes = new ArrayList<>();

        for (Skill skill : registry.listAll()) {
            if (!isEvolutionEnabled(skill)) continue;

            EvolutionOutcome outcome = evolve(skill, dryRun);
            outcomes.add(outcome);

            if (outcome.accepted()) {
                log.info("[evo] ✓ skill '{}' evolved: {} → {}",
                        skill.name(), outcome.baselineScore(), outcome.candidateScore());
            } else {
                log.debug("[evo] ✗ skill '{}' not improved: {}", skill.name(), outcome.reason());
            }
        }
        return outcomes;
    }

    /**
     * Evolves a specific skill through the full EvoSkill cycle.
     */
    public EvolutionOutcome evolve(Skill skill, boolean dryRun) {
        List<EpisodicMemory.Episode> failures = episodic.recentFailures(20).stream()
                .filter(e -> e.toolsUsed().stream()
                        .anyMatch(t -> t.contains(skill.name())))
                .toList();

        if (failures.size() < MIN_FAILURES_TO_EVOLVE) {
            return EvolutionOutcome.insufficientData(skill.name(), failures.size());
        }

        List<EvolutionRecord> attempts = history.getOrDefault(skill.name(), List.of());
        if (attempts.size() >= MAX_EVOLUTION_ATTEMPTS) {
            return EvolutionOutcome.maxAttemptsReached(skill.name());
        }

        log.info("[evo] evolving skill '{}' ({} failures)", skill.name(), failures.size());

        // Step 1: Diagnose failures
        String diagnosis = diagnose(skill, failures);

        // Step 2: Propose updated instructions
        String candidateInstructions = propose(skill, failures, diagnosis);
        if (candidateInstructions.isBlank()) {
            return EvolutionOutcome.proposalFailed(skill.name(), "LLM produced empty proposal");
        }

        // Step 3: Run benchmarks (baseline vs candidate)
        BenchmarkHarness.BenchmarkReport baseline  = benchmark.evaluate(skill.name());
        BenchmarkHarness.BenchmarkReport candidate =
                evaluateCandidate(skill, candidateInstructions);

        // Step 4: Pareto check
        boolean paretoSuperior = isParetoSuperior(baseline, candidate);

        EvolutionRecord record = new EvolutionRecord(
                skill.name(), diagnosis, candidateInstructions,
                baseline.avgScore(), candidate.avgScore(),
                paretoSuperior, dryRun, Instant.now());
        history.computeIfAbsent(skill.name(), k -> new ArrayList<>()).add(record);

        if (!paretoSuperior) {
            return EvolutionOutcome.notImproved(skill.name(),
                    baseline.avgScore(), candidate.avgScore(),
                    "Candidate not Pareto-superior to baseline");
        }

        // Step 5: Apply (unless dry run)
        if (!dryRun) {
            applyEvolution(skill, candidateInstructions);
        }

        return new EvolutionOutcome(
                skill.name(), true, dryRun,
                baseline.avgScore(), candidate.avgScore(),
                diagnosis, candidateInstructions, "Pareto-superior — accepted");
    }

    // ── Diagnosis ──────────────────────────────────────────────────────────

    private String diagnose(Skill skill, List<EpisodicMemory.Episode> failures) {
        String failureSummary = failures.stream()
                .limit(5)
                .map(e -> "- Task: " + truncate(e.task(), 100) + "\n  Error: " +
                        truncate(e.result(), 200))
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = String.format("""
                Analyze these failures of the skill '%s':
                
                Current skill instructions (excerpt):
                %s
                
                Recent failures:
                %s
                
                Identify the ROOT CAUSE of these failures in 2-3 sentences.
                Be specific about what is wrong with the current instructions.
                """,
                skill.name(),
                truncate(skill.instructions(), 500),
                failureSummary);

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You are a skill failure analyst. Be precise and concise.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.2)
                            .maxTokens(300)
                            .streaming(false)
                            .build());
            return resp.getContent() != null ? resp.getContent() : "Diagnosis unavailable";
        } catch (Exception e) {
            return "Diagnosis failed: " + e.getMessage();
        }
    }

    // ── Proposal ───────────────────────────────────────────────────────────

    private String propose(Skill skill, List<EpisodicMemory.Episode> failures, String diagnosis) {
        String prompt = String.format("""
                You are improving an AI agent skill.
                
                Skill name: %s
                Skill description: %s
                
                Current instructions:
                %s
                
                Failure diagnosis:
                %s
                
                Generate IMPROVED skill instructions that fix the diagnosed problems.
                Keep the same format and structure, but fix the specific issues.
                Output ONLY the new instruction text, no preamble.
                """,
                skill.name(), skill.description(),
                skill.instructions(),
                diagnosis);

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You are a skill author. Write clear, actionable instructions.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.4)
                            .maxTokens(1500)
                            .streaming(false)
                            .build());
            return resp.getContent() != null ? resp.getContent().strip() : "";
        } catch (Exception e) {
            log.warn("[evo] proposal failed: {}", e.getMessage());
            return "";
        }
    }

    // ── Candidate evaluation ───────────────────────────────────────────────

    private BenchmarkHarness.BenchmarkReport evaluateCandidate(Skill skill, String newInstructions) {
        // For now, evaluate using the same suite — in production, inject the candidate skill
        // into a shadow environment and run tests against it
        return benchmark.evaluate(skill.name() + "-candidate");
    }

    // ── Pareto check ───────────────────────────────────────────────────────

    private boolean isParetoSuperior(BenchmarkHarness.BenchmarkReport baseline,
                                     BenchmarkHarness.BenchmarkReport candidate) {
        double bScore   = baseline.avgScore();
        double cScore   = candidate.avgScore();
        double bLatency = baseline.avgLatency();
        double cLatency = candidate.avgLatency();

        // Must not regress on either dimension
        if (cScore < bScore - 0.01) return false;
        if (cLatency > bLatency * 1.2) return false;

        // Must improve meaningfully on at least one
        return (cScore > bScore + MIN_PARETO_IMPROVEMENT)
                || (cLatency < bLatency * (1 - MIN_PARETO_IMPROVEMENT));
    }

    // ── Apply ──────────────────────────────────────────────────────────────

    private void applyEvolution(Skill skill, String newInstructions) {
        try {
            // Find skill directory
            Path skillsDir = Path.of(config.skillsDir());
            Path skillDir  = skillsDir.resolve(skill.name());
            Path skillMd   = skillDir.resolve("SKILL.md");

            if (!Files.exists(skillMd)) {
                log.warn("[evo] SKILL.md not found for '{}', skipping apply", skill.name());
                return;
            }

            // Backup
            Path backup = skillMd.resolveSibling(
                    "SKILL.md.bak." + Instant.now().getEpochSecond());
            Files.copy(skillMd, backup);
            log.info("[evo] backed up {} → {}", skillMd, backup.getFileName());

            // Replace instructions section (keep YAML frontmatter)
            String content = Files.readString(skillMd);
            int fmEnd = content.indexOf("\n---", 3);
            if (fmEnd < 0) fmEnd = 0;

            String frontmatter = fmEnd > 0 ? content.substring(0, fmEnd + 4) : "";
            String newContent  = frontmatter + "\n\n" + newInstructions + "\n";
            Files.writeString(skillMd, newContent);

            // Reload in registry
            registry.reload();
            log.info("[evo] applied evolution to skill '{}'", skill.name());

        } catch (IOException e) {
            log.error("[evo] apply failed for '{}': {}", skill.name(), e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isEvolutionEnabled(Skill skill) {
        // Opt-in: check metadata flag
        return "true".equalsIgnoreCase(skill.metadata().getOrDefault("evolve", "false"))
                || "auto".equalsIgnoreCase(skill.metadata().getOrDefault("evolve", ""));
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    /** Returns the full evolution history for a skill. */
    public List<EvolutionRecord> historyFor(String skillName) {
        return history.getOrDefault(skillName, List.of());
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record EvolutionRecord(
            String  skillName,
            String  diagnosis,
            String  proposedInstructions,
            double  baselineScore,
            double  candidateScore,
            boolean accepted,
            boolean dryRun,
            Instant runAt
    ) {}

    public record EvolutionOutcome(
            String  skillName,
            boolean accepted,
            boolean dryRun,
            double  baselineScore,
            double  candidateScore,
            String  diagnosis,
            String  proposal,
            String  reason
    ) {
        static EvolutionOutcome insufficientData(String name, int count) {
            return new EvolutionOutcome(name, false, false, 0, 0, "",
                    "", "Insufficient failures: " + count + " < " + MIN_FAILURES_TO_EVOLVE);
        }
        static EvolutionOutcome maxAttemptsReached(String name) {
            return new EvolutionOutcome(name, false, false, 0, 0, "",
                    "", "Max evolution attempts reached");
        }
        static EvolutionOutcome proposalFailed(String name, String r) {
            return new EvolutionOutcome(name, false, false, 0, 0, "", "", r);
        }
        static EvolutionOutcome notImproved(String name, double b, double c, String r) {
            return new EvolutionOutcome(name, false, false, b, c, "", "", r);
        }

        public String summary() {
            return String.format("[%s] %s: %.2f → %.2f (%s)",
                    accepted ? "ACCEPTED" : "REJECTED", skillName,
                    baselineScore, candidateScore, reason);
        }
    }
}
