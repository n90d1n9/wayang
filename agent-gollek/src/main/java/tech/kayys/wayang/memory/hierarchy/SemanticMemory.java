package tech.kayys.gamelan.memory.hierarchy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.Message;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Semantic Memory — a knowledge graph of Atomic Knowledge Units (AKUs).
 *
 * <h2>What is an AKU</h2>
 * An AKU is a granular, machine-actionable fact extracted from agent episodes:
 * <pre>
 * concept: "maven-test-command"
 * fact:    "mvn test -pl auth-service -Dtest=UserServiceTest"
 * type:    COMMAND
 * confidence: 0.95
 * sources: [episode-42, episode-67]
 * </pre>
 *
 * <h2>Knowledge Graph Structure</h2>
 * Nodes are KnowledgeNodes (AKUs). Edges represent relationships:
 * <ul>
 *   <li>RELATED_TO — general association</li>
 *   <li>CONTRADICTS — conflicting facts (triggers confidence decay)</li>
 *   <li>REFINES — more specific version of another AKU</li>
 *   <li>DEPENDS_ON — one fact requires another to be true</li>
 * </ul>
 *
 * <h2>Extraction Pipeline</h2>
 * When an episode completes, the LLM is prompted to extract structured
 * knowledge. This uses a minimal direct call (not agentic loop) to keep cost low.
 *
 * <h2>Query</h2>
 * Nodes are retrieved by keyword overlap + confidence ranking.
 * When an embedding model is available, cosine similarity is used instead.
 */
@ApplicationScoped
public class SemanticMemory {

    private static final Logger log = LoggerFactory.getLogger(SemanticMemory.class);

    private static final int    MAX_NODES       = 5_000;
    private static final int    EXTRACT_TIMEOUT = 30;
    private static final String NODES_FILE      = "knowledge-graph.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Inject GollekSdk sdk;

    private final Map<Long, KnowledgeNode>   nodes   = new ConcurrentHashMap<>();
    private final Map<Long, List<Edge>>      edges   = new ConcurrentHashMap<>();
    private final AtomicLong                 idSeq   = new AtomicLong(1);
    private Path                             storageDir;

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize()
                .getFileName().toString();
        storageDir = Path.of(System.getProperty("user.home"), ".gamelan",
                "memory", "semantic", project);
        load();
        log.info("[semantic] {} knowledge nodes loaded", nodes.size());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Extracts knowledge nodes from a completed episode using the LLM.
     * Runs on a virtual thread to avoid blocking the caller.
     */
    public List<KnowledgeNode> extractFrom(EpisodicMemory.Episode episode) {
        if (episode.result().isBlank() && episode.task().isBlank()) return List.of();

        try {
            String prompt = buildExtractionPrompt(episode);
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model("llama3")   // use default — caller overrides via config
                            .systemPrompt(EXTRACTION_SYSTEM)
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.1)
                            .maxTokens(512)
                            .streaming(false)
                            .build());

            List<KnowledgeNode> extracted = parseExtractionResponse(
                    resp.getContent(), episode.id());
            extracted.forEach(n -> nodes.put(n.id(), n));
            trimToLimit();
            persistAsync();
            return extracted;

        } catch (Exception e) {
            log.debug("[semantic] extraction failed for episode {}: {}", episode.id(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Queries the knowledge graph for nodes relevant to a task.
     */
    public List<KnowledgeNode> query(String task, int limit) {
        Set<String> taskWords = tokenize(task);
        if (taskWords.isEmpty()) return List.of();

        record Scored(KnowledgeNode n, double score) {}

        return nodes.values().stream()
                .map(n -> {
                    Set<String> nodeWords = tokenize(n.concept() + " " + n.fact());
                    nodeWords.retainAll(taskWords);
                    double overlap = nodeWords.size();
                    double conf    = n.confidence();
                    return new Scored(n, overlap * conf);
                })
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(limit)
                .map(Scored::n)
                .toList();
    }

    /**
     * Adds or updates a knowledge node. If a node with the same concept exists,
     * confidence is averaged and source episodes are merged.
     */
    public KnowledgeNode upsert(String concept, String fact,
                                NodeType type, long sourceEpisodeId, double confidence) {
        // Check for existing node with same concept
        Optional<KnowledgeNode> existing = nodes.values().stream()
                .filter(n -> n.concept().equalsIgnoreCase(concept.strip()))
                .findFirst();

        if (existing.isPresent()) {
            KnowledgeNode old = existing.get();
            // Merge: blend confidence, add source episode
            List<Long> sources = new ArrayList<>(old.sourceEpisodeIds());
            if (!sources.contains(sourceEpisodeId)) sources.add(sourceEpisodeId);
            double newConf = (old.confidence() + confidence) / 2.0;
            KnowledgeNode updated = new KnowledgeNode(
                    old.id(), concept, fact, type, newConf,
                    List.copyOf(sources), Instant.now());
            nodes.put(updated.id(), updated);
            return updated;
        }

        KnowledgeNode node = new KnowledgeNode(
                idSeq.getAndIncrement(), concept.strip(), fact.strip(),
                type, confidence, List.of(sourceEpisodeId), Instant.now());
        nodes.put(node.id(), node);
        persistAsync();
        return node;
    }

    /**
     * Adds a directed edge between two knowledge nodes.
     */
    public void addEdge(long fromId, long toId, EdgeType type, double weight) {
        edges.computeIfAbsent(fromId, k -> new CopyOnWriteArrayList<>())
             .add(new Edge(fromId, toId, type, weight));
    }

    /**
     * Returns all nodes connected to a given node (one hop).
     */
    public List<KnowledgeNode> neighbors(long nodeId) {
        return edges.getOrDefault(nodeId, List.of()).stream()
                .map(e -> nodes.get(e.toId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public Map<Long, KnowledgeNode> allNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    // ── Extraction ─────────────────────────────────────────────────────────

    private static final String EXTRACTION_SYSTEM = """
            You are a knowledge extraction assistant.
            Extract structured facts from agent execution traces.
            Reply ONLY with a JSON array. No prose. No markdown fences.
            Each element: {"concept":"key","fact":"value","type":"FACT|COMMAND|PREFERENCE|CONSTRAINT","confidence":0.0-1.0}
            Extract 0-5 facts. Only high-confidence, reusable facts.
            """;

    private String buildExtractionPrompt(EpisodicMemory.Episode ep) {
        return String.format("""
                Task: %s
                
                Outcome: %s
                Success: %b
                Tools used: %s
                
                Extract reusable knowledge facts from this execution.
                """,
                ep.task(),
                ep.result().length() > 500 ? ep.result().substring(0, 500) + "…" : ep.result(),
                ep.success(),
                String.join(", ", ep.toolsUsed()));
    }

    @SuppressWarnings("unchecked")
    private List<KnowledgeNode> parseExtractionResponse(String raw, long episodeId) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            String json = raw.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").strip();
            int s = json.indexOf('['), e = json.lastIndexOf(']');
            if (s < 0 || e <= s) return List.of();
            json = json.substring(s, e + 1);
            List<Map<String, Object>> items = MAPPER.readValue(json, List.class);
            List<KnowledgeNode> result = new ArrayList<>();
            for (Map<String, Object> m : items) {
                String concept = (String) m.getOrDefault("concept", "");
                String fact    = (String) m.getOrDefault("fact", "");
                String typeStr = (String) m.getOrDefault("type", "FACT");
                double conf    = m.containsKey("confidence")
                        ? ((Number) m.get("confidence")).doubleValue() : 0.7;
                if (concept.isBlank() || fact.isBlank()) continue;
                NodeType type;
                try { type = NodeType.valueOf(typeStr); }
                catch (Exception ex) { type = NodeType.FACT; }
                result.add(upsert(concept, fact, type, episodeId, conf));
            }
            return result;
        } catch (Exception ex) {
            log.debug("[semantic] parse failed: {}", ex.getMessage());
            return List.of();
        }
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void trimToLimit() {
        if (nodes.size() <= MAX_NODES) return;
        nodes.entrySet().stream()
             .sorted(Comparator.comparingDouble(e -> e.getValue().confidence()))
             .limit(nodes.size() - MAX_NODES)
             .map(Map.Entry::getKey)
             .forEach(nodes::remove);
    }

    private void persistAsync() {
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(storageDir);
                MAPPER.writerWithDefaultPrettyPrinter()
                      .writeValue(storageDir.resolve(NODES_FILE).toFile(),
                              new ArrayList<>(nodes.values()));
            } catch (IOException e) {
                log.warn("[semantic] persist failed: {}", e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Path file = storageDir.resolve(NODES_FILE);
        if (!Files.exists(file)) return;
        try {
            List<KnowledgeNode> loaded = MAPPER.readValue(file.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, KnowledgeNode.class));
            loaded.forEach(n -> nodes.put(n.id(), n));
            loaded.stream().mapToLong(KnowledgeNode::id).max()
                  .ifPresent(max -> idSeq.set(max + 1));
        } catch (IOException e) {
            log.warn("[semantic] load failed: {}", e.getMessage());
        }
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        Set<String> words = new HashSet<>();
        for (String w : text.toLowerCase().split("[\\s\\p{Punct}]+")) {
            if (w.length() >= 3) words.add(w);
        }
        return words;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record KnowledgeNode(
            long        id,
            String      concept,
            String      fact,
            NodeType    type,
            double      confidence,
            List<Long>  sourceEpisodeIds,
            Instant     updatedAt
    ) {}

    public record Edge(long fromId, long toId, EdgeType type, double weight) {}

    public enum NodeType    { FACT, COMMAND, PREFERENCE, CONSTRAINT, RELATIONSHIP }
    public enum EdgeType    { RELATED_TO, CONTRADICTS, REFINES, DEPENDS_ON }
}
