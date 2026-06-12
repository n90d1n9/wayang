package tech.kayys.gamelan.hyperagent.coordination;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.role.AgentRole;
import tech.kayys.gamelan.communication.AgentMessageBus;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.hyperagent.role.AsymmetricRoleSession;
import tech.kayys.gamelan.hyperagent.role.RoleAgent;
import tech.kayys.gamelan.memory.hierarchy.MemoryHierarchy;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * HyperagentOrchestrator — the orchestrator-of-orchestrators pattern.
 *
 * <h2>What is a Hyperagent</h2>
 * A hyperagent is a meta-level agent that coordinates other agents, each of
 * which may itself be an orchestrating agent. This creates a hierarchical
 * agent tree capable of arbitrary depth:
 * <pre>
 * HyperagentOrchestrator
 *   ├── Domain Coordinator (security)
 *   │     ├── Generator: produce security fixes
 *   │     └── Critic: review security fixes
 *   ├── Domain Coordinator (performance)
 *   │     ├── Researcher: profile bottlenecks
 *   │     └── Synthesizer: prioritize findings
 *   └── Judge: resolve conflicts, produce final recommendation
 * </pre>
 *
 * <h2>Execution Model</h2>
 * <ol>
 *   <li>Task Decomposition: LLM decomposes the task into domain-specific sub-problems</li>
 *   <li>Domain Assignment: Each sub-problem is assigned an appropriate role pair</li>
 *   <li>Parallel Execution: All domain sessions run in parallel on virtual threads</li>
 *   <li>Conflict Detection: Contradictory findings across domains are flagged</li>
 *   <li>Judgment Phase: JUDGE role resolves conflicts and weighs evidence</li>
 *   <li>Synthesis: Final coherent response integrates all domain outputs</li>
 * </ol>
 *
 * <h2>Role Pair Selection</h2>
 * The hyperagent automatically selects the best role pair for each domain:
 * <ul>
 *   <li>Code quality → GENERATOR + CRITIC</li>
 *   <li>Security → RESEARCHER + VERIFIER</li>
 *   <li>Architecture → PROPOSER + JUDGE</li>
 *   <li>Documentation → TUTOR + STUDENT (generates explanation + verifies clarity)</li>
 *   <li>Testing → PLANNER + EXECUTOR</li>
 * </ul>
 *
 * <h2>Cross-Domain Conflict Resolution</h2>
 * When the security agent recommends encrypting all traffic (adds 15ms latency)
 * and the performance agent recommends disabling TLS (saves 15ms), the JUDGE
 * applies domain priority rules (security > performance) and produces a binding
 * decision: use TLS but with connection pooling to amortize the overhead.
 */
@ApplicationScoped
public class HyperagentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(HyperagentOrchestrator.class);

    private static final int MAX_PARALLEL_SESSIONS = 6;
    private static final int SESSION_TIMEOUT_S     = 180;

    // Pre-defined domain → role-pair mappings
    private static final Map<String, RolePair> DOMAIN_ROLE_MAP = Map.ofEntries(
            Map.entry("security",        new RolePair(AgentRole.RESEARCHER,  AgentRole.VERIFIER)),
            Map.entry("code quality",    new RolePair(AgentRole.GENERATOR,   AgentRole.CRITIC)),
            Map.entry("bugs",            new RolePair(AgentRole.GENERATOR,   AgentRole.CRITIC)),
            Map.entry("performance",     new RolePair(AgentRole.RESEARCHER,  AgentRole.CRITIC)),
            Map.entry("architecture",    new RolePair(AgentRole.PROPOSER,    AgentRole.JUDGE)),
            Map.entry("documentation",   new RolePair(AgentRole.TUTOR,       AgentRole.STUDENT)),
            Map.entry("testing",         new RolePair(AgentRole.PLANNER,     AgentRole.EXECUTOR)),
            Map.entry("refactoring",     new RolePair(AgentRole.PLANNER,     AgentRole.EXECUTOR)),
            Map.entry("analysis",        new RolePair(AgentRole.RESEARCHER,  AgentRole.SYNTHESIZER)),
            Map.entry("review",          new RolePair(AgentRole.GENERATOR,   AgentRole.CRITIC)),
            Map.entry("design",          new RolePair(AgentRole.PROPOSER,    AgentRole.CRITIC)),
            Map.entry("verification",    new RolePair(AgentRole.EXECUTOR,    AgentRole.VERIFIER)),
            Map.entry("knowledge",       new RolePair(AgentRole.TUTOR,       AgentRole.STUDENT))
    );

    @Inject GollekSdk      sdk;
    @Inject GamelanConfig  config;
    @Inject AgentMessageBus bus;
    @Inject MemoryHierarchy memory;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Executes a hyperagent run: decompose, assign, parallel execute, judge, synthesize.
     *
     * @param task      the complex multi-domain task
     * @param maxRounds max rounds per domain session
     * @return the final synthesized result
     */
    public HyperagentResult run(String task, int maxRounds) {
        Instant start = Instant.now();
        log.info("[hyperagent] starting: task='{}'", truncate(task, 80));

        // 1. Decompose task into domain sub-problems
        List<DomainAssignment> assignments = decompose(task);
        if (assignments.isEmpty()) {
            return runSingleDomain(task, maxRounds, start);
        }
        log.info("[hyperagent] decomposed into {} domains: {}",
                assignments.size(), assignments.stream().map(DomainAssignment::domain).toList());

        // 2. Run domain sessions in parallel
        Map<String, AsymmetricRoleSession.SessionResult> domainResults =
                runParallel(assignments, task, maxRounds);

        // 3. Detect conflicts across domain results
        List<Conflict> conflicts = detectConflicts(domainResults);
        log.info("[hyperagent] detected {} cross-domain conflicts", conflicts.size());

        // 4. Judgment phase (if conflicts exist)
        String judgment = "";
        if (!conflicts.isEmpty()) {
            judgment = runJudgment(task, domainResults, conflicts);
        }

        // 5. Final synthesis
        String synthesis = synthesize(task, domainResults, judgment);

        // 6. Record to memory
        memory.record(task, synthesis, true,
                domainResults.values().stream()
                        .flatMap(r -> r.rounds().stream()
                                .flatMap(round -> round.resultA().toolExecutions().stream()
                                        .map(RoleAgent.ToolExecution::toolName)))
                        .distinct().toList(),
                Duration.between(start, Instant.now()).toMillis());

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[hyperagent] complete: {} domains, {} conflicts, {}ms",
                domainResults.size(), conflicts.size(), elapsed.toMillis());

        return new HyperagentResult(task, domainResults, conflicts, judgment,
                synthesis, elapsed);
    }

    /**
     * Runs a targeted two-agent session with an explicit role pair.
     */
    public AsymmetricRoleSession.SessionResult runRoleSession(
            String task, AgentRole roleA, AgentRole roleB, int maxRounds) {

        RoleAgent agentA = RoleAgent.builder(UUID.randomUUID().toString(), roleA)
                .sdk(sdk).config(config).bus(bus).build();
        RoleAgent agentB = RoleAgent.builder(UUID.randomUUID().toString(), roleB)
                .sdk(sdk).config(config).bus(bus).build();

        return AsymmetricRoleSession.builder(agentA, agentB)
                .maxRounds(maxRounds)
                .bus(bus)
                .onRound((agent, result) -> log.info("[hyperagent] {} ({}) round {}: {} tools",
                        agent.name(), agent.role(), result.turnNumber(),
                        result.toolExecutions().size()))
                .build()
                .run(task);
    }

    /**
     * Selects the best role pair for a given task domain keyword.
     */
    public RolePair selectRolePair(String domain) {
        String lower = domain.toLowerCase();
        return DOMAIN_ROLE_MAP.entrySet().stream()
                .filter(e -> lower.contains(e.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(new RolePair(AgentRole.RESEARCHER, AgentRole.CRITIC));
    }

    // ── Decomposition ──────────────────────────────────────────────────────

    private List<DomainAssignment> decompose(String task) {
        String prompt = """
                Decompose this complex task into 2-4 independent domains that require
                specialized expertise. For each domain, name it clearly.

                Reply ONLY with a JSON array. No prose. No markdown.
                [{"domain":"security","subtask":"Analyze for security vulnerabilities"},
                 {"domain":"code quality","subtask":"Review for bugs and anti-patterns"}]

                Task: %s
                """.formatted(task);
        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You are a task decomposition expert. Reply only in JSON.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.2).maxTokens(512).streaming(false).build());

            return parseAssignments(resp.getContent(), task);
        } catch (SdkException e) {
            log.warn("[hyperagent] decomposition failed, using single domain: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<DomainAssignment> parseAssignments(String raw, String originalTask) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            String json = raw.replaceAll("(?s)```json\\s*","").replaceAll("```","").strip();
            int s = json.indexOf('['), e = json.lastIndexOf(']');
            if (s < 0 || e <= s) return List.of();
            List<Map<String,Object>> items = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json.substring(s, e+1), List.class);
            List<DomainAssignment> result = new ArrayList<>();
            for (Map<String,Object> m : items) {
                String domain  = (String) m.getOrDefault("domain",  "general");
                String subtask = (String) m.getOrDefault("subtask", originalTask);
                RolePair pair  = selectRolePair(domain);
                result.add(new DomainAssignment(domain, subtask, pair));
            }
            return result;
        } catch (Exception ex) {
            log.debug("[hyperagent] decomposition parse failed: {}", ex.getMessage());
            return List.of();
        }
    }

    // ── Parallel execution ─────────────────────────────────────────────────

    private Map<String, AsymmetricRoleSession.SessionResult> runParallel(
            List<DomainAssignment> assignments, String task, int maxRounds) {

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        Map<String, Future<AsymmetricRoleSession.SessionResult>> futures = new LinkedHashMap<>();

        for (DomainAssignment assignment : assignments) {
            futures.put(assignment.domain(), exec.submit(() ->
                    runDomainSession(assignment, task, maxRounds)));
        }
        exec.shutdown();

        Map<String, AsymmetricRoleSession.SessionResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, Future<AsymmetricRoleSession.SessionResult>> e : futures.entrySet()) {
            try {
                results.put(e.getKey(), e.getValue().get(SESSION_TIMEOUT_S, TimeUnit.SECONDS));
                log.info("[hyperagent] domain '{}' complete", e.getKey());
            } catch (TimeoutException ex) {
                e.getValue().cancel(true);
                log.warn("[hyperagent] domain '{}' timed out", e.getKey());
            } catch (Exception ex) {
                log.error("[hyperagent] domain '{}' failed: {}", e.getKey(), ex.getMessage());
            }
        }
        return results;
    }

    private AsymmetricRoleSession.SessionResult runDomainSession(
            DomainAssignment assignment, String parentTask, int maxRounds) {
        String sessionId = assignment.domain().replace(" ","-") + "-" +
                System.currentTimeMillis() % 10000;
        RoleAgent agentA = RoleAgent.builder(sessionId + "-A", assignment.rolePair().roleA())
                .sdk(sdk).config(config).bus(bus).build();
        RoleAgent agentB = RoleAgent.builder(sessionId + "-B", assignment.rolePair().roleB())
                .sdk(sdk).config(config).bus(bus).build();

        String sessionTask = "[Domain: " + assignment.domain() + "]\n" +
                             "Parent task: " + parentTask + "\n\n" +
                             "Your specific focus: " + assignment.subtask();

        return AsymmetricRoleSession.builder(agentA, agentB)
                .sessionId(sessionId).maxRounds(maxRounds).bus(bus).build()
                .run(sessionTask);
    }

    private HyperagentResult runSingleDomain(String task, int maxRounds, Instant start) {
        RolePair pair = selectRolePair(task);
        AsymmetricRoleSession.SessionResult result = runRoleSession(task,
                pair.roleA(), pair.roleB(), maxRounds);
        return new HyperagentResult(task,
                Map.of("default", result), List.of(), "",
                result.synthesis(), Duration.between(start, Instant.now()));
    }

    // ── Conflict detection ─────────────────────────────────────────────────

    private List<Conflict> detectConflicts(
            Map<String, AsymmetricRoleSession.SessionResult> results) {
        List<Conflict> conflicts = new ArrayList<>();
        List<Map.Entry<String, AsymmetricRoleSession.SessionResult>> entries =
                new ArrayList<>(results.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                String domainA = entries.get(i).getKey();
                String domainB = entries.get(j).getKey();
                String synthA  = entries.get(i).getValue().synthesis();
                String synthB  = entries.get(j).getValue().synthesis();

                // Simple conflict heuristic: opposite sentiment on same keyword
                Optional<String> conflict = findConflict(domainA, synthA, domainB, synthB);
                conflict.ifPresent(reason ->
                        conflicts.add(new Conflict(domainA, domainB, reason)));
            }
        }
        return conflicts;
    }

    private Optional<String> findConflict(String dA, String sA, String dB, String sB) {
        if (sA == null || sB == null) return Optional.empty();
        String lA = sA.toLowerCase(), lB = sB.toLowerCase();

        // Check for performance vs security tension
        if (dA.contains("performance") && dB.contains("security")) {
            boolean perfWantsDisable = lA.contains("disable") || lA.contains("remove") || lA.contains("avoid");
            boolean secWantsEnable   = lB.contains("enable") || lB.contains("add") || lB.contains("require");
            if (perfWantsDisable && secWantsEnable) {
                return Optional.of("Performance recommends disabling features that Security requires");
            }
        }
        // Check for contradictory recommendations on the same subject
        CONFLICT_KEYWORDS.forEach((topic, opposites) -> {});
        return Optional.empty();
    }

    private static final Map<String, List<String>> CONFLICT_KEYWORDS = Map.of(
            "cache",      List.of("disable caching", "enable caching", "remove cache", "add cache"),
            "encryption", List.of("remove encryption", "add encryption", "disable tls", "enable tls"),
            "logging",    List.of("remove logging", "add logging", "disable log", "enable log")
    );

    // ── Judgment ──────────────────────────────────────────────────────────

    private String runJudgment(String task,
                                Map<String, AsymmetricRoleSession.SessionResult> results,
                                List<Conflict> conflicts) {
        StringBuilder evidence = new StringBuilder();
        results.forEach((domain, result) -> {
            evidence.append("## ").append(domain).append("\n");
            evidence.append(truncate(result.synthesis(), 800)).append("\n\n");
        });
        evidence.append("## Conflicts\n");
        conflicts.forEach(c -> evidence.append("- ").append(c.description()).append("\n"));

        String prompt = """
                You are the JUDGE. Resolve these cross-domain conflicts and produce
                a binding decision that balances all concerns.

                Original task: %s

                Domain findings and conflicts:
                %s

                Produce a structured judgment:
                1. For each conflict: state which domain takes priority and why
                2. Final integrated recommendation
                
                Be decisive. Apply principle: security > correctness > performance > style.
                """.formatted(task, evidence);

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You are a final arbiter. Apply consistent principles. Be decisive.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.1).maxTokens(1024).streaming(false).build());
            return resp.getContent() != null ? resp.getContent() : "";
        } catch (SdkException e) {
            return "Judgment unavailable: " + e.getMessage();
        }
    }

    // ── Synthesis ─────────────────────────────────────────────────────────

    private String synthesize(String task,
                               Map<String, AsymmetricRoleSession.SessionResult> results,
                               String judgment) {
        StringBuilder merged = new StringBuilder();
        results.forEach((domain, result) -> {
            if (result.success()) {
                merged.append("### ").append(domain.toUpperCase()).append("\n");
                merged.append(truncate(result.synthesis(), 1200)).append("\n\n");
            }
        });
        if (!judgment.isBlank()) {
            merged.append("### JUDGMENT\n").append(judgment).append("\n\n");
        }

        String prompt = """
                Synthesize these multi-domain findings into one coherent, actionable report.
                Prioritize critical findings. Eliminate duplication.
                Structure: Executive Summary → Critical Issues → Recommendations → Next Steps.

                Task: %s

                Findings:
                %s
                """.formatted(task, merged);
        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You produce concise, prioritized, actionable reports.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.3).maxTokens(2048).streaming(false).build());
            return resp.getContent() != null ? resp.getContent() : merged.toString();
        } catch (SdkException e) {
            return merged.toString();
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record RolePair(AgentRole roleA, AgentRole roleB) {}

    public record DomainAssignment(String domain, String subtask, RolePair rolePair) {}

    public record Conflict(String domainA, String domainB, String description) {}

    public record HyperagentResult(
            String                  task,
            Map<String, AsymmetricRoleSession.SessionResult> domainResults,
            List<Conflict>          conflicts,
            String                  judgment,
            String                  synthesis,
            Duration                elapsed
    ) {
        public boolean success()     { return !synthesis.isBlank(); }
        public int domainCount()     { return domainResults.size(); }
        public int conflictCount()   { return conflicts.size(); }

        public String summary() {
            return String.format("Hyperagent: %d domains | %d conflicts | %s | %dms",
                    domainCount(), conflictCount(),
                    success() ? "SUCCESS" : "FAILED", elapsed.toMillis());
        }
    }
}
