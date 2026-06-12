package tech.kayys.gamelan.planning.graph;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * Graph-of-Thought (GoT) Planner with Monte Carlo Tree Search (MCTS).
 *
 * <h2>Why Graph-of-Thought over Tree-of-Thought</h2>
 * Tree-of-Thought (ToT) expands reasoning as a tree: each node has one parent.
 * Graph-of-Thought allows multiple paths to converge: a reasoning step can
 * build on insights from multiple independent branches. This models how
 * experts actually reason: synthesis of disparate threads.
 *
 * <pre>
 *   ToT:                          GoT:
 *   [A]                           [A]         [B]
 *    ├─[B]                          \         /
 *    │   ├─[D]                       ──[C]──
 *    │   └─[E]                      /       \
 *    └─[C]                        [D]       [E]
 *          └─[F]                    \       /
 *                                    ──[F]─
 * </pre>
 *
 * <h2>MCTS Integration</h2>
 * Monte Carlo Tree Search is used to explore the thought graph efficiently:
 * <ol>
 *   <li><b>Selection</b>: UCB1 formula selects the most promising unexplored node</li>
 *   <li><b>Expansion</b>: LLM generates child thoughts for the selected node</li>
 *   <li><b>Simulation</b>: lightweight LLM rollout evaluates the thought's promise</li>
 *   <li><b>Backpropagation</b>: simulation score propagates back up the graph</li>
 * </ol>
 *
 * <h2>Performance</h2>
 * MCTS with budget N simulations: O(N log N) with branching factor B.
 * Practically: 50 simulations with B=3 explores 150 thought nodes — enough
 * for complex multi-step reasoning tasks.
 *
 * <h2>When to use GoT vs HTN</h2>
 * <ul>
 *   <li>Use {@link tech.kayys.gamelan.planning.HierarchicalTaskPlanner} (HTN)
 *       for structured, decomposable tasks with known sub-problem types</li>
 *   <li>Use GoT for open-ended reasoning, creative problem-solving, and tasks
 *       where the problem structure is not known in advance</li>
 * </ul>
 */
@ApplicationScoped
public class GraphOfThoughtPlanner {

    private static final Logger log = LoggerFactory.getLogger(GraphOfThoughtPlanner.class);

    private static final int MCTS_SIMULATIONS  = 30;
    private static final int BRANCHING_FACTOR  = 3;
    private static final int MAX_DEPTH         = 5;
    private static final double UCB1_C         = 1.414; // exploration constant √2

    @Inject GollekSdk     sdk;
    @Inject GamelanConfig config;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Plans a task using Graph-of-Thought with MCTS exploration.
     *
     * @param task            the task to plan
     * @param simulationBudget max MCTS simulations (controls quality vs speed)
     * @return the best reasoning path found
     */
    public ThoughtGraph plan(String task, int simulationBudget) {
        log.info("[got] planning: {} (budget={})", truncate(task, 80), simulationBudget);
        Instant start = Instant.now();

        // Initialize the root node
        ThoughtNode root = ThoughtNode.root(task);
        ThoughtGraph graph = new ThoughtGraph(root);

        // Run MCTS
        for (int sim = 0; sim < simulationBudget; sim++) {
            // Selection: find the most promising unexplored node using UCB1
            ThoughtNode selected = select(root, graph);

            // Expansion: generate child thoughts
            if (!selected.isTerminal() && selected.depth() < MAX_DEPTH) {
                List<ThoughtNode> children = expand(selected, task);
                children.forEach(child -> {
                    graph.addNode(child);
                    graph.addEdge(selected.id(), child.id(), EdgeType.FOLLOWS);
                });

                if (!children.isEmpty()) {
                    // Simulation: evaluate the best child
                    ThoughtNode toSimulate = children.get(0);
                    double score = simulate(toSimulate, task);

                    // Backpropagation: update scores up the tree
                    backpropagate(toSimulate, score, graph);
                }
            }
        }

        // Find the best path from root to a terminal node
        List<ThoughtNode> bestPath = findBestPath(root, graph);
        graph.setBestPath(bestPath);

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[got] complete: {} nodes, {} edges, best score={:.3f}, {}ms",
                graph.nodeCount(), graph.edgeCount(),
                bestPath.isEmpty() ? 0 : bestPath.get(bestPath.size()-1).avgScore(),
                elapsed.toMillis());

        return graph;
    }

    /**
     * Merges multiple thought branches into a single synthesis node.
     * This is the "Graph" part of GoT — allowing convergence of ideas.
     */
    public ThoughtNode merge(List<ThoughtNode> branches, String synthesisPrompt) {
        String mergedContext = branches.stream()
                .map(b -> "Branch " + b.id() + ": " + truncate(b.thought(), 300))
                .collect(Collectors.joining("\n\n"));

        String mergedThought = generateThought(
                synthesisPrompt + "\n\nMerge these insights:\n" + mergedContext,
                0.3);

        return ThoughtNode.synthesis(UUID.randomUUID().toString(), mergedThought,
                branches.stream().map(ThoughtNode::id).toList());
    }

    // ── MCTS phases ────────────────────────────────────────────────────────

    /**
     * Selection: UCB1-guided traversal to find the best unexplored node.
     * UCB1 = avgScore + C * sqrt(ln(parentVisits) / nodeVisits)
     */
    private ThoughtNode select(ThoughtNode root, ThoughtGraph graph) {
        ThoughtNode current = root;
        while (!current.children(graph).isEmpty()) {
            ThoughtNode best = current.children(graph).stream()
                    .max(Comparator.comparingDouble(n -> ucb1(n, current.visitCount())))
                    .orElse(current);
            if (best.visitCount() == 0) return best; // unexplored node
            current = best;
        }
        return current;
    }

    private double ucb1(ThoughtNode node, int parentVisits) {
        if (node.visitCount() == 0) return Double.MAX_VALUE; // unvisited = highest priority
        return node.avgScore() +
               UCB1_C * Math.sqrt(Math.log(Math.max(1, parentVisits)) / node.visitCount());
    }

    /**
     * Expansion: LLM generates BRANCHING_FACTOR child thoughts.
     */
    private List<ThoughtNode> expand(ThoughtNode node, String originalTask) {
        String prompt = """
                Task: %s
                
                Current reasoning step (%d/%d):
                %s
                
                Generate exactly %d DISTINCT next reasoning steps. Each must:
                - Be a concrete, actionable thought (not a question)
                - Advance the solution meaningfully
                - Be different from the others
                
                Reply ONLY with a JSON array: ["step1", "step2", "step3"]
                """.formatted(originalTask, node.depth(), MAX_DEPTH,
                        truncate(node.thought(), 400), BRANCHING_FACTOR);

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You generate diverse, concrete reasoning steps. Reply only in JSON.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.8)  // high temp for diversity
                            .maxTokens(400)
                            .streaming(false)
                            .build());

            return parseThoughts(resp.getContent(), node);
        } catch (Exception e) {
            log.debug("[got] expansion failed for node {}: {}", node.id(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Simulation: lightweight evaluation of how promising a thought is.
     * Returns a score in [0, 1].
     */
    private double simulate(ThoughtNode node, String task) {
        String prompt = """
                Task: %s
                
                Proposed reasoning step:
                %s
                
                Rate this step: how likely is following this reasoning to lead to
                a correct solution? Reply with ONLY a number 0.0-1.0.
                Examples: 0.9 (very promising), 0.5 (neutral), 0.1 (poor direction)
                """.formatted(task, truncate(node.thought(), 300));

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You evaluate reasoning quality. Reply with only a number 0.0-1.0.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.1)  // low temp for consistent scoring
                            .maxTokens(10)
                            .streaming(false)
                            .build());

            String raw = resp.getContent() != null ? resp.getContent().strip() : "0.5";
            return Math.max(0, Math.min(1, Double.parseDouble(raw.replaceAll("[^0-9.]", ""))));
        } catch (Exception e) {
            return 0.5; // neutral score on failure
        }
    }

    /**
     * Backpropagation: update visit counts and scores up the graph.
     */
    private void backpropagate(ThoughtNode node, double score, ThoughtGraph graph) {
        node.update(score);
        graph.parents(node.id()).forEach(parent -> backpropagate(parent, score, graph));
    }

    /**
     * Finds the best path from root to a leaf by following max avgScore.
     */
    private List<ThoughtNode> findBestPath(ThoughtNode root, ThoughtGraph graph) {
        List<ThoughtNode> path = new ArrayList<>();
        ThoughtNode current = root;
        path.add(current);

        while (!current.children(graph).isEmpty()) {
            ThoughtNode best = current.children(graph).stream()
                    .max(Comparator.comparingDouble(ThoughtNode::avgScore))
                    .orElse(null);
            if (best == null || best.avgScore() < 0.1) break;
            path.add(best);
            current = best;
        }
        return path;
    }

    private String generateThought(String prompt, double temperature) {
        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You generate concrete reasoning steps.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(temperature).maxTokens(200).streaming(false).build());
            return resp.getContent() != null ? resp.getContent().strip() : "";
        } catch (Exception e) { return ""; }
    }

    @SuppressWarnings("unchecked")
    private List<ThoughtNode> parseThoughts(String raw, ThoughtNode parent) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            String json = raw.replaceAll("(?s)```json\\s*","").replaceAll("```","").strip();
            int s = json.indexOf('['), e = json.lastIndexOf(']');
            if (s < 0 || e <= s) return List.of();
            List<String> thoughts = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json.substring(s, e + 1), List.class);
            return thoughts.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(t -> ThoughtNode.child(
                            UUID.randomUUID().toString(), t.strip(),
                            parent.id(), parent.depth() + 1))
                    .toList();
        } catch (Exception ex) { return List.of(); }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum EdgeType { FOLLOWS, CONTRADICTS, MERGES_FROM }

    /**
     * A single node in the thought graph — represents one reasoning step.
     */
    public static final class ThoughtNode {
        private final String       id;
        private final String       thought;
        private final String       parentId;
        private final List<String> mergedFromIds; // for synthesis nodes
        private final int          depth;
        private final boolean      isSynthesis;

        // MCTS stats (mutable, thread-safe)
        private final AtomicInteger visitCount    = new AtomicInteger(0);
        private final AtomicLong    totalScore    = new AtomicLong(0);

        private ThoughtNode(String id, String thought, String parentId,
                            List<String> mergedFromIds, int depth, boolean synthesis) {
            this.id            = id;
            this.thought       = thought;
            this.parentId      = parentId;
            this.mergedFromIds = mergedFromIds;
            this.depth         = depth;
            this.isSynthesis   = synthesis;
        }

        static ThoughtNode root(String task) {
            return new ThoughtNode(UUID.randomUUID().toString(),
                    "Initial task: " + task, null, List.of(), 0, false);
        }

        static ThoughtNode child(String id, String thought, String parentId, int depth) {
            return new ThoughtNode(id, thought, parentId, List.of(), depth, false);
        }

        static ThoughtNode synthesis(String id, String thought, List<String> sourceIds) {
            return new ThoughtNode(id, thought, null, sourceIds, 0, true);
        }

        void update(double score) {
            visitCount.incrementAndGet();
            totalScore.addAndGet((long)(score * 1000)); // store as integer millis
        }

        double avgScore() {
            int v = visitCount.get();
            return v == 0 ? 0 : (double) totalScore.get() / (v * 1000.0);
        }

        List<ThoughtNode> children(ThoughtGraph graph) { return graph.children(this.id); }

        boolean isTerminal() { return false; } // could add domain-specific terminal conditions

        // Accessors
        public String       id()            { return id; }
        public String       thought()       { return thought; }
        public String       parentId()      { return parentId; }
        public List<String> mergedFromIds() { return mergedFromIds; }
        public int          depth()         { return depth; }
        public boolean      isSynthesis()   { return isSynthesis; }
        public int          visitCount()    { return visitCount.get(); }
    }

    /**
     * The directed thought graph — nodes are reasoning steps, edges represent
     * thought flow (FOLLOWS) or synthesis (MERGES_FROM).
     */
    public static final class ThoughtGraph {
        private final ThoughtNode               root;
        private final Map<String, ThoughtNode>  nodes    = new ConcurrentHashMap<>();
        private final Map<String, List<String>> children = new ConcurrentHashMap<>();
        private final Map<String, List<String>> parents  = new ConcurrentHashMap<>();
        private final List<GraphEdge>           edges    = new CopyOnWriteArrayList<>();
        private List<ThoughtNode>               bestPath = List.of();

        ThoughtGraph(ThoughtNode root) {
            this.root = root;
            nodes.put(root.id(), root);
        }

        void addNode(ThoughtNode node) {
            nodes.put(node.id(), node);
        }

        void addEdge(String fromId, String toId, EdgeType type) {
            edges.add(new GraphEdge(fromId, toId, type));
            children.computeIfAbsent(fromId, k -> new CopyOnWriteArrayList<>()).add(toId);
            parents.computeIfAbsent(toId, k -> new CopyOnWriteArrayList<>()).add(fromId);
        }

        List<ThoughtNode> children(String nodeId) {
            return children.getOrDefault(nodeId, List.of()).stream()
                    .map(nodes::get).filter(Objects::nonNull).toList();
        }

        List<ThoughtNode> parents(String nodeId) {
            return parents.getOrDefault(nodeId, List.of()).stream()
                    .map(nodes::get).filter(Objects::nonNull).toList();
        }

        void setBestPath(List<ThoughtNode> path) { this.bestPath = path; }

        public ThoughtNode       root()      { return root; }
        public int               nodeCount() { return nodes.size(); }
        public int               edgeCount() { return edges.size(); }
        public List<ThoughtNode> bestPath()  { return bestPath; }

        /** Returns the final synthesized reasoning as a concatenation of the best path. */
        public String bestReasoning() {
            return bestPath.stream()
                    .map(ThoughtNode::thought)
                    .collect(Collectors.joining("\n\n→ "));
        }

        public String summary() {
            double bestScore = bestPath.isEmpty() ? 0 :
                    bestPath.get(bestPath.size()-1).avgScore();
            return String.format("GoT: %d nodes, %d edges, best_score=%.3f, depth=%d",
                    nodeCount(), edgeCount(), bestScore, bestPath.size());
        }
    }

    public record GraphEdge(String fromId, String toId, EdgeType type) {}
}
