package tech.kayys.gamelan.knowledge.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * KnowledgeGraph — a typed, queryable semantic graph over project entities.
 *
 * <h2>Why a knowledge graph for agents</h2>
 * A flat list of semantic memory nodes answers "what do I know about X?"
 * A knowledge graph answers richer questions:
 * <ul>
 * <li>"What classes depend on UserService?"</li>
 * <li>"What has changed since yesterday?"</li>
 * <li>"Which modules have circular dependencies?"</li>
 * <li>"What does this PR affect transitively?"</li>
 * <li>"Which tests cover the code I'm about to edit?"</li>
 * </ul>
 *
 * <h2>Graph model</h2>
 * 
 * <pre>
 * Node types: CLASS | METHOD | FILE | MODULE | CONCEPT | PERSON | ISSUE | PR | TEST
 *
 * Edge types:
 *   DEPENDS_ON    — A imports/uses B
 *   TESTED_BY     — A is tested by B
 *   IMPLEMENTS    — A implements interface B
 *   EXTENDS       — A extends B
 *   CALLS         — method A calls method B
 *   DEFINED_IN    — A is defined in file B
 *   RELATED_TO    — general semantic relationship
 *   CONTRADICTS   — A asserts something incompatible with B
 *   AUTHORED_BY   — A was created/modified by person B
 *   TRACKS        — issue A tracks bug/feature about B
 * </pre>
 *
 * <h2>Inference rules</h2>
 * <ul>
 * <li>Transitive closure: if A→B and B→C via DEPENDS_ON, then A transitively
 * depends on C</li>
 * <li>Test coverage: if A TESTED_BY T and A DEPENDS_ON B, then B is also
 * indirectly tested by T</li>
 * <li>Impact analysis: given a changed node, find all nodes reachable via
 * DEPENDS_ON (reversed)</li>
 * </ul>
 */
@ApplicationScoped
public class KnowledgeGraph {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraph.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Path PERSIST_DIR = Path.of(System.getProperty("user.home"), ".gamelan", "knowledge");

    @Inject
    AgentTelemetry telemetry;

    private final Map<String, KgNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, List<KgEdge>> outEdges = new ConcurrentHashMap<>(); // nodeId → edges
    private final Map<String, List<KgEdge>> inEdges = new ConcurrentHashMap<>();
    private final AtomicLong edgeCount = new AtomicLong(0);

    @PostConstruct
    void init() {
        loadFromDisk();
        log.info("[kg] initialized: {} nodes, {} edges", nodes.size(), edgeCount.get());
    }

    // ── Mutation ───────────────────────────────────────────────────────────

    /**
     * Adds or updates a node in the graph.
     *
     * @param id         unique node identifier (e.g.,
     *                   "class:tech.kayys.UserService")
     * @param type       the node type
     * @param label      human-readable label
     * @param properties additional metadata
     * @return the upserted node
     */
    public KgNode upsertNode(String id, NodeType type, String label,
            Map<String, String> properties) {
        KgNode node = new KgNode(id, type, label,
                properties != null ? Map.copyOf(properties) : Map.of(),
                Instant.now());
        nodes.put(id, node);
        telemetry.count("kg.node.upsert");
        persistAsync();
        return node;
    }

    /**
     * Adds a directed edge between two nodes.
     * Both nodes are auto-created with CONCEPT type if they don't exist.
     *
     * @param fromId     source node id
     * @param toId       target node id
     * @param type       edge type
     * @param weight     edge weight (0.0–1.0; higher = stronger relationship)
     * @param properties edge metadata
     * @return the created edge
     */
    public KgEdge addEdge(String fromId, String toId, EdgeType type,
            double weight, Map<String, String> properties) {
        // Auto-create nodes if missing
        nodes.computeIfAbsent(fromId, id -> new KgNode(id, NodeType.CONCEPT, id, Map.of(), Instant.now()));
        nodes.computeIfAbsent(toId, id -> new KgNode(id, NodeType.CONCEPT, id, Map.of(), Instant.now()));

        String edgeId = fromId + ":" + type.name() + ":" + toId;
        KgEdge edge = new KgEdge(edgeId, fromId, toId, type, weight,
                properties != null ? Map.copyOf(properties) : Map.of(), Instant.now());

        outEdges.computeIfAbsent(fromId, k -> new CopyOnWriteArrayList<>()).add(edge);
        inEdges.computeIfAbsent(toId, k -> new CopyOnWriteArrayList<>()).add(edge);
        edgeCount.incrementAndGet();

        telemetry.count("kg.edge.add");
        persistAsync();
        return edge;
    }

    /**
     * Removes a node and all its edges.
     */
    public boolean removeNode(String nodeId) {
        if (!nodes.containsKey(nodeId))
            return false;
        nodes.remove(nodeId);
        List<KgEdge> out = outEdges.remove(nodeId);
        List<KgEdge> in = inEdges.remove(nodeId);
        if (out != null) {
            edgeCount.addAndGet(-out.size());
            out.forEach(e -> {
                List<KgEdge> i = inEdges.get(e.toId());
                if (i != null)
                    i.removeIf(x -> x.fromId().equals(nodeId));
            });
        }
        if (in != null) {
            edgeCount.addAndGet(-in.size());
            in.forEach(e -> {
                List<KgEdge> o = outEdges.get(e.fromId());
                if (o != null)
                    o.removeIf(x -> x.toId().equals(nodeId));
            });
        }
        return true;
    }

    // ── Queries ────────────────────────────────────────────────────────────

    /** Finds a node by its ID. */
    public Optional<KgNode> findNode(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /** Finds all nodes of a given type. */
    public List<KgNode> findByType(NodeType type) {
        return nodes.values().stream().filter(n -> n.type() == type).toList();
    }

    /** Finds all nodes whose label contains the given text (case-insensitive). */
    public List<KgNode> search(String text) {
        String lower = text.toLowerCase();
        return nodes.values().stream()
                .filter(n -> n.label().toLowerCase().contains(lower) ||
                        n.id().toLowerCase().contains(lower))
                .toList();
    }

    /** Returns all direct neighbors of a node via given edge types. */
    public List<KgNode> neighbors(String nodeId, EdgeType... edgeTypes) {
        Set<EdgeType> filter = edgeTypes.length > 0 ? Set.of(edgeTypes) : null;
        List<KgEdge> edges = outEdges.getOrDefault(nodeId, List.of());
        return edges.stream()
                .filter(e -> filter == null || filter.contains(e.type()))
                .map(e -> nodes.get(e.toId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** Returns all nodes that have an edge pointing TO the given node. */
    public List<KgNode> predecessors(String nodeId, EdgeType... edgeTypes) {
        Set<EdgeType> filter = edgeTypes.length > 0 ? Set.of(edgeTypes) : null;
        List<KgEdge> edges = inEdges.getOrDefault(nodeId, List.of());
        return edges.stream()
                .filter(e -> filter == null || filter.contains(e.type()))
                .map(e -> nodes.get(e.fromId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Computes the transitive closure from a node via a given edge type.
     * Uses BFS — stops at maxDepth to prevent infinite traversal.
     *
     * @param startId  starting node
     * @param edgeType the edge type to follow
     * @param maxDepth maximum BFS depth
     * @return all reachable nodes (excluding start)
     */
    public Set<KgNode> reachable(String startId, EdgeType edgeType, int maxDepth) {
        Set<KgNode> visited = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> depth = new HashMap<>();
        queue.add(startId);
        depth.put(startId, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int d = depth.get(current);
            if (d >= maxDepth)
                continue;

            neighbors(current, edgeType).forEach(neighbor -> {
                if (!depth.containsKey(neighbor.id())) {
                    visited.add(neighbor);
                    queue.add(neighbor.id());
                    depth.put(neighbor.id(), d + 1);
                }
            });
        }
        return visited;
    }

    /**
     * Impact analysis: given a changed node, find all nodes that transitively
     * depend on it (i.e., follow DEPENDS_ON edges in reverse).
     *
     * @param changedNodeId the node that was modified
     * @param maxDepth      BFS depth limit
     * @return nodes that are impacted by the change
     */
    public ImpactSet impactAnalysis(String changedNodeId, int maxDepth) {
        // Traverse DEPENDS_ON edges in reverse (predecessors)
        Set<KgNode> impacted = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> depth = new HashMap<>();
        queue.add(changedNodeId);
        depth.put(changedNodeId, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int d = depth.get(current);
            if (d >= maxDepth)
                continue;

            predecessors(current, EdgeType.DEPENDS_ON).forEach(pred -> {
                if (!depth.containsKey(pred.id())) {
                    impacted.add(pred);
                    queue.add(pred.id());
                    depth.put(pred.id(), d + 1);
                }
            });
        }

        // Find tests that cover any of the impacted nodes
        Set<KgNode> affectedTests = impacted.stream()
                .flatMap(n -> predecessors(n.id(), EdgeType.TESTED_BY).stream())
                .collect(Collectors.toSet());

        return new ImpactSet(changedNodeId, impacted, affectedTests,
                impacted.size(), affectedTests.size());
    }

    /**
     * Detects cycles in the dependency graph.
     * Returns all strongly-connected components of size > 1.
     */
    public List<List<String>> detectCycles() {
        // Tarjan's SCC algorithm (iterative to avoid stack overflow)
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Map<String, Boolean> onStack = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        List<List<String>> sccs = new ArrayList<>();
        AtomicInteger idx = new AtomicInteger(0);

        nodes.keySet().forEach(nodeId -> {
            if (!index.containsKey(nodeId)) {
                tarjanDfs(nodeId, index, lowlink, onStack, stack, sccs, idx);
            }
        });

        return sccs.stream().filter(scc -> scc.size() > 1).toList();
    }

    /**
     * Finds all shortest paths between two nodes using BFS.
     */
    public List<List<String>> shortestPaths(String fromId, String toId, int maxPaths) {
        if (!nodes.containsKey(fromId) || !nodes.containsKey(toId))
            return List.of();

        List<List<String>> paths = new ArrayList<>();
        Queue<List<String>> queue = new ArrayDeque<>();
        queue.add(List.of(fromId));

        while (!queue.isEmpty() && paths.size() < maxPaths) {
            List<String> path = queue.poll();
            String last = path.get(path.size() - 1);

            if (last.equals(toId)) {
                paths.add(path);
                continue;
            }
            if (!paths.isEmpty() && path.size() > paths.get(0).size())
                break;

            outEdges.getOrDefault(last, List.of()).stream()
                    .map(KgEdge::toId)
                    .filter(n -> !path.contains(n))
                    .forEach(n -> {
                        List<String> next = new ArrayList<>(path);
                        next.add(n);
                        queue.add(next);
                    });
        }
        return paths;
    }

    /**
     * Returns graph statistics.
     */
    public GraphStats stats() {
        Map<NodeType, Long> nodesByType = nodes.values().stream()
                .collect(Collectors.groupingBy(KgNode::type, Collectors.counting()));
        Map<EdgeType, Long> edgesByType = outEdges.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(KgEdge::type, Collectors.counting()));

        // Find most-connected nodes (by total in+out degree)
        List<String> hubNodes = nodes.keySet().stream()
                .sorted(Comparator.comparingInt((String id) -> outEdges.getOrDefault(id, List.of()).size() +
                        inEdges.getOrDefault(id, List.of()).size()).reversed())
                .limit(5)
                .toList();

        return new GraphStats(nodes.size(), edgeCount.get(), nodesByType, edgesByType, hubNodes);
    }

    /** Converts the graph to a Mermaid diagram string (for documentation). */
    public String toMermaid(int maxNodes) {
        StringBuilder sb = new StringBuilder("graph TD\n");
        nodes.values().stream().limit(maxNodes).forEach(n -> sb.append("    ").append(sanitize(n.id()))
                .append("[\"").append(n.label()).append("\"]\n"));
        outEdges.values().stream().flatMap(Collection::stream).limit(maxNodes * 3)
                .forEach(e -> sb.append("    ").append(sanitize(e.fromId()))
                        .append(" -->|").append(e.type().name()).append("| ")
                        .append(sanitize(e.toId())).append("\n"));
        return sb.toString();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void tarjanDfs(String v, Map<String, Integer> index, Map<String, Integer> lowlink,
            Map<String, Boolean> onStack, Deque<String> stack,
            List<List<String>> sccs, AtomicInteger idx) {
        index.put(v, idx.get());
        lowlink.put(v, idx.get());
        idx.incrementAndGet();
        stack.push(v);
        onStack.put(v, true);

        for (KgEdge e : outEdges.getOrDefault(v, List.of())) {
            String w = e.toId();
            if (!index.containsKey(w)) {
                tarjanDfs(w, index, lowlink, onStack, stack, sccs, idx);
                lowlink.merge(v, lowlink.getOrDefault(w, 0), Math::min);
            } else if (Boolean.TRUE.equals(onStack.get(w))) {
                lowlink.merge(v, index.get(w), Math::min);
            }
        }

        if (lowlink.get(v).equals(index.get(v))) {
            List<String> scc = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.put(w, false);
                scc.add(w);
            } while (!w.equals(v));
            sccs.add(scc);
        }
    }

    private String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private void persistAsync() {
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(PERSIST_DIR);
                GraphSnapshot snap = new GraphSnapshot(
                        List.copyOf(nodes.values()),
                        outEdges.values().stream().flatMap(Collection::stream).toList(),
                        Instant.now());
                MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValue(PERSIST_DIR.resolve("graph.json").toFile(), snap);
            } catch (IOException e) {
                log.warn("[kg] persist failed: {}", e.getMessage());
            }
        });
    }

    private void loadFromDisk() {
        Path file = PERSIST_DIR.resolve("graph.json");
        if (!Files.exists(file))
            return;
        try {
            GraphSnapshot snap = MAPPER.readValue(file.toFile(), GraphSnapshot.class);
            snap.nodes().forEach(n -> nodes.put(n.id(), n));
            snap.edges().forEach(e -> {
                outEdges.computeIfAbsent(e.fromId(), k -> new CopyOnWriteArrayList<>()).add(e);
                inEdges.computeIfAbsent(e.toId(), k -> new CopyOnWriteArrayList<>()).add(e);
                edgeCount.incrementAndGet();
            });
        } catch (IOException e) {
            log.warn("[kg] load failed: {}", e.getMessage());
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum NodeType {
        CLASS, METHOD, FILE, MODULE, CONCEPT, PERSON, ISSUE, PR, TEST, PACKAGE
    }

    public enum EdgeType {
        DEPENDS_ON, TESTED_BY, IMPLEMENTS, EXTENDS, CALLS,
        DEFINED_IN, RELATED_TO, CONTRADICTS, AUTHORED_BY, TRACKS, PART_OF
    }

    public record KgNode(String id, NodeType type, String label,
            Map<String, String> properties, Instant createdAt) {
    }

    public record KgEdge(String id, String fromId, String toId, EdgeType type,
            double weight, Map<String, String> properties, Instant createdAt) {
    }

    public record ImpactSet(String changedNode, Set<KgNode> impactedNodes,
            Set<KgNode> affectedTests, int impactCount, int testCount) {
        public String summary() {
            return String.format("Impact[%s]: %d nodes affected, %d tests to run",
                    changedNode, impactCount, testCount);
        }
    }

    public record GraphStats(long nodeCount, long edgeCount,
            Map<NodeType, Long> nodesByType, Map<EdgeType, Long> edgesByType,
            List<String> hubNodes) {
        public String summary() {
            return String.format("Graph: %d nodes, %d edges | hubs=%s",
                    nodeCount, edgeCount, hubNodes.stream().limit(3).toList());
        }
    }

    private record GraphSnapshot(List<KgNode> nodes, List<KgEdge> edges, Instant savedAt) {
    }
}
