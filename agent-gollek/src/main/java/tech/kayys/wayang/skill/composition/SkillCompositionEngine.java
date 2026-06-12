package tech.kayys.gamelan.skill.composition;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.SemanticMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Skill Composition Engine — chains and traverses skills as a knowledge graph.
 *
 * <h2>Skill Composition</h2>
 * Instead of loading all skills and hoping the LLM picks the right one, this
 * engine builds skill chains based on declared dependencies:
 * <pre>
 * SKILL.md frontmatter:
 * ---
 * name: fix-security-bug
 * requires: [analyze-code, read-file]
 * produces: [patched-code, security-report]
 * ---
 * </pre>
 * The engine resolves the dependency graph and ensures required skills are
 * loaded in the correct order before the target skill executes.
 *
 * <h2>DAG-based Concurrent Execution</h2>
 * Skills with no dependency on each other execute in parallel. Skills that
 * depend on previous outputs wait for them. This is the correct model for
 * complex workflows: not sequential (too slow), not fully parallel (wrong order).
 *
 * <pre>
 * read-file ──┐
 *              ├──▶ analyze-code ──▶ fix-bug ──▶ run-tests
 * search-files ─┘
 * </pre>
 *
 * <h2>Knowledge Graph Traversal</h2>
 * The semantic memory knowledge graph is traversed to find skills relevant to
 * a query using multi-hop reasoning:
 * <pre>
 * query: "fix authentication bug"
 *   hop 1: find nodes matching "authentication", "bug"
 *   hop 2: find skill nodes connected to those concepts
 *   hop 3: expand to skills required by those skills
 * </pre>
 *
 * <h2>Progressive Disclosure</h2>
 * Skills are loaded in stages to preserve context window budget:
 * <ol>
 *   <li>Load metadata only (name, description, triggers) — minimal tokens</li>
 *   <li>Load instructions for selected skills only</li>
 *   <li>Load reference materials on-demand during execution</li>
 * </ol>
 */
@ApplicationScoped
public class SkillCompositionEngine {

    private static final Logger log = LoggerFactory.getLogger(SkillCompositionEngine.class);

    private static final int MAX_GRAPH_HOPS = 3;
    private static final int MAX_PARALLEL   = 8;

    @Inject SkillRegistry          registry;
    @Inject SemanticMemory         semantic;
    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig           config;

    // ── Progressive Disclosure ─────────────────────────────────────────────

    /**
     * Returns skills in progressive disclosure order:
     * <ol>
     *   <li>Metadata-only stubs for all skills (ultra-cheap, for routing)</li>
     *   <li>Full instructions for high-relevance skills</li>
     *   <li>References loaded on-demand</li>
     * </ol>
     */
    public ProgressiveSkillSet loadProgressively(String task) {
        List<Skill> all = registry.listAll();

        // Stage 1: score all by metadata-only relevance (no instruction reading)
        List<SkillScore> scored = scoreByMetadata(task, all);

        // Stage 2: full instructions for top-3 (the actual context injection)
        List<Skill> primary = scored.stream()
                .filter(s -> s.score() > 2.0)
                .limit(3)
                .map(SkillScore::skill)
                .toList();

        // Stage 3: light stubs for next-5 (name + description only, for awareness)
        List<SkillStub> awareness = scored.stream()
                .filter(s -> s.score() > 0.5)
                .skip(3)
                .limit(5)
                .map(s -> new SkillStub(s.skill().name(), s.skill().description()))
                .toList();

        log.debug("[composition] progressive load: {} primary + {} awareness stubs",
                primary.size(), awareness.size());

        return new ProgressiveSkillSet(primary, awareness, scored);
    }

    // ── Dependency Resolution ──────────────────────────────────────────────

    /**
     * Resolves a skill's full dependency graph and returns an ordered execution plan.
     * Uses topological sort to determine correct execution order.
     */
    public SkillExecutionPlan resolveDependencies(String targetSkillName) {
        Map<String, Skill> allByName = registry.listAll().stream()
                .collect(Collectors.toMap(Skill::name, s -> s));

        Skill target = allByName.get(targetSkillName);
        if (target == null) {
            return SkillExecutionPlan.notFound(targetSkillName);
        }

        // Build dependency graph
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        buildDependencyGraph(target, allByName, deps, new HashSet<>());

        // Topological sort
        List<String> ordered = topologicalSort(deps);

        // Map to skill objects
        List<Skill> orderedSkills = ordered.stream()
                .map(allByName::get)
                .filter(Objects::nonNull)
                .toList();

        return new SkillExecutionPlan(targetSkillName, orderedSkills, deps);
    }

    // ── DAG-based Execution ────────────────────────────────────────────────

    /**
     * Executes a skill chain following the dependency DAG.
     * Independent skills run in parallel; dependent skills wait for their prerequisites.
     */
    public SkillChainResult executeChain(SkillExecutionPlan plan, String userTask) {
        Instant start = Instant.now();
        log.info("[composition] executing skill chain: {} skills", plan.orderedSkills().size());

        Map<String, OrchestratorResult> results = new ConcurrentHashMap<>();
        Map<String, CompletableFuture<OrchestratorResult>> futures = new ConcurrentHashMap<>();

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

        for (Skill skill : plan.orderedSkills()) {
            Set<String> skillDeps = plan.dependencies().getOrDefault(skill.name(), Set.of());

            CompletableFuture<OrchestratorResult> future = CompletableFuture.supplyAsync(() -> {
                // Wait for all dependencies
                skillDeps.stream()
                        .map(futures::get)
                        .filter(Objects::nonNull)
                        .forEach(f -> {
                            try { f.get(60, TimeUnit.SECONDS); }
                            catch (Exception ignored) {}
                        });

                // Build context from dependency outputs
                String context = buildDependencyContext(skill.name(), skillDeps, results);
                String augmentedTask = userTask + (context.isBlank() ? "" : "\n\n" + context);

                log.info("[composition] executing skill: {}", skill.name());
                OrchestratorResult result = orchestrator.execute(
                        AgentRequest.builder(augmentedTask)
                                .model(config.defaultModel())
                                .systemExtra("Active skill: " + skill.name() +
                                        "\n\n" + skill.instructions())
                                .maxSteps(5)
                                .stream(false)
                                .build());

                results.put(skill.name(), result);
                return result;
            }, exec);

            futures.put(skill.name(), future);
        }

        // Wait for all skills to complete
        futures.values().forEach(f -> {
            try { f.get(300, TimeUnit.SECONDS); }
            catch (Exception e) { log.warn("[composition] skill future failed: {}", e.getMessage()); }
        });

        exec.shutdown();
        Duration elapsed = Duration.between(start, Instant.now());
        boolean success  = results.values().stream().allMatch(OrchestratorResult::success);

        return new SkillChainResult(plan.targetSkill(), results, success, elapsed);
    }

    // ── Knowledge Graph Traversal ──────────────────────────────────────────

    /**
     * Traverses the knowledge graph to find skills relevant to a query
     * through multi-hop semantic reasoning.
     */
    public List<Skill> traverseKnowledgeGraph(String query, int maxHops) {
        Set<String> relevantConcepts = new HashSet<>();
        Set<String> relevantSkills   = new HashSet<>();

        // Hop 0: find directly matching knowledge nodes
        List<SemanticMemory.KnowledgeNode> nodes = semantic.query(query, 10);
        nodes.forEach(n -> relevantConcepts.add(n.concept()));

        // Hops 1..maxHops: expand via graph edges
        for (int hop = 1; hop <= Math.min(maxHops, MAX_GRAPH_HOPS); hop++) {
            Set<String> expanded = new HashSet<>();
            for (SemanticMemory.KnowledgeNode node : nodes) {
                List<SemanticMemory.KnowledgeNode> neighbors = semantic.neighbors(node.id());
                neighbors.forEach(n -> expanded.add(n.concept()));
            }
            relevantConcepts.addAll(expanded);
        }

        log.debug("[composition] knowledge graph traversal: {} concepts found in {} hops",
                relevantConcepts.size(), maxHops);

        // Match concepts to skills via description overlap
        String expandedQuery = String.join(" ", relevantConcepts) + " " + query;
        return scoreByMetadata(expandedQuery, registry.listAll()).stream()
                .filter(s -> s.score() > 1.0)
                .limit(5)
                .map(SkillScore::skill)
                .toList();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private List<SkillScore> scoreByMetadata(String task, List<Skill> skills) {
        Set<String> taskWords = tokenize(task.toLowerCase());
        return skills.stream()
                .map(s -> {
                    Set<String> skillWords = tokenize((s.name() + " " + s.description()).toLowerCase());
                    skillWords.retainAll(taskWords);
                    double score = skillWords.stream()
                            .mapToDouble(w -> w.length() >= 6 ? 3.0 : 1.0).sum();
                    // Boost exact name match
                    if (task.toLowerCase().contains(s.name().replace("-", " "))) score += 10;
                    return new SkillScore(s, score);
                })
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingDouble(SkillScore::score).reversed())
                .toList();
    }

    private void buildDependencyGraph(Skill skill, Map<String, Skill> allByName,
                                      Map<String, Set<String>> deps, Set<String> visited) {
        if (visited.contains(skill.name())) return;
        visited.add(skill.name());

        // Parse 'requires' from metadata
        String requires = skill.metadata().getOrDefault("requires", "");
        Set<String> skillDeps = new HashSet<>();

        if (!requires.isBlank()) {
            Arrays.stream(requires.split("[,\\s]+"))
                    .map(String::strip)
                    .filter(r -> !r.isBlank())
                    .forEach(reqName -> {
                        skillDeps.add(reqName);
                        Skill dep = allByName.get(reqName);
                        if (dep != null) {
                            buildDependencyGraph(dep, allByName, deps, visited);
                        }
                    });
        }

        deps.put(skill.name(), skillDeps);
    }

    private List<String> topologicalSort(Map<String, Set<String>> deps) {
        List<String> sorted     = new ArrayList<>();
        Set<String>  visited    = new HashSet<>();
        Set<String>  inProgress = new HashSet<>();

        for (String node : deps.keySet()) {
            if (!visited.contains(node)) {
                topoVisit(node, deps, visited, inProgress, sorted);
            }
        }
        return sorted;
    }

    private void topoVisit(String node, Map<String, Set<String>> deps,
                            Set<String> visited, Set<String> inProgress, List<String> sorted) {
        if (inProgress.contains(node)) {
            log.warn("[composition] cycle detected at: {}", node);
            return;
        }
        if (visited.contains(node)) return;

        inProgress.add(node);
        Set<String> nodeDeps = deps.getOrDefault(node, Set.of());
        nodeDeps.forEach(d -> topoVisit(d, deps, visited, inProgress, sorted));
        inProgress.remove(node);
        visited.add(node);
        sorted.add(node);
    }

    private String buildDependencyContext(String skillName, Set<String> deps,
                                           Map<String, OrchestratorResult> results) {
        if (deps.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Context from prerequisite skills:\n");
        deps.forEach(dep -> {
            OrchestratorResult r = results.get(dep);
            if (r != null && r.success()) {
                String output = r.answer().length() > 1000
                        ? r.answer().substring(0, 1000) + "…" : r.answer();
                sb.append("\n### ").append(dep).append("\n").append(output).append("\n");
            }
        });
        return sb.toString();
    }

    private Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        for (String w : text.split("[\\s\\p{Punct}]+")) {
            if (w.length() >= 4) words.add(w);
        }
        return words;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record SkillScore(Skill skill, double score) {}

    public record SkillStub(String name, String description) {}

    public record ProgressiveSkillSet(
            List<Skill>      primary,    // full instructions loaded
            List<SkillStub>  awareness,  // metadata only
            List<SkillScore> allScored
    ) {
        /** Builds a token-efficient system prompt block. */
        public String toPromptBlock() {
            StringBuilder sb = new StringBuilder("## Active Skills\n\n");
            primary.forEach(s -> {
                sb.append("### `").append(s.name()).append("`\n");
                sb.append(s.instructions().strip()).append("\n\n");
            });
            if (!awareness.isEmpty()) {
                sb.append("### Also available (metadata only)\n");
                awareness.forEach(s -> sb.append("- `").append(s.name()).append("`: ")
                        .append(s.description()).append("\n"));
            }
            return sb.toString();
        }
    }

    public record SkillExecutionPlan(
            String              targetSkill,
            List<Skill>         orderedSkills,  // topologically sorted
            Map<String, Set<String>> dependencies
    ) {
        static SkillExecutionPlan notFound(String name) {
            return new SkillExecutionPlan(name, List.of(), Map.of());
        }
        public boolean isEmpty() { return orderedSkills.isEmpty(); }
    }

    public record SkillChainResult(
            String                          targetSkill,
            Map<String, OrchestratorResult> stepResults,
            boolean                         success,
            Duration                        elapsed
    ) {
        public String finalAnswer() {
            return stepResults.getOrDefault(targetSkill,
                    OrchestratorResult.failure("chain", "No result", Duration.ZERO)).answer();
        }
    }
}
