package tech.kayys.gamelan.memory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Vector-backed semantic memory store (Section VIII — Semantic Memory Layer).
 *
 * <p>Replaces the plain {@code Map<String, String>} in {@link MemoryHierarchy}
 * with a proper vector repository. Semantic facts are stored as embeddings;
 * retrieval uses cosine similarity rather than keyword matching.
 *
 * <h2>Why vectors beat keyword matching</h2>
 * <ul>
 *   <li>"Authentication" matches "auth", "login", "JWT", "session token" via
 *       semantic similarity even though none of those words appear in the query</li>
 *   <li>"ORM" matches "Hibernate", "JPA", "entity manager" — concepts, not tokens</li>
 *   <li>Long facts with richer context are retrieved by their meaning, not
 *       by whether the query happens to contain the right keyword</li>
 * </ul>
 *
 * <h2>Fallback</h2>
 * When the embedding model is unavailable ({@link EmbeddingService#isAvailable()}
 * returns false), the store falls back to keyword BM25-style matching so the
 * system remains functional without a running embedding model.
 *
 * <h2>Metadata schema</h2>
 * Each entry carries metadata:
 * <ul>
 *   <li>{@code type} — FACT | PREFERENCE | DECISION | COMMAND | PROCEDURE</li>
 *   <li>{@code topic} — short key (stable identifier for upsert)</li>
 *   <li>{@code project} — project name or "_global"</li>
 * </ul>
 */
@ApplicationScoped
public class SemanticMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(SemanticMemoryStore.class);

    private static final float DEFAULT_MIN_SCORE = 0.60f; // cosine threshold for relevance
    private static final int   DEFAULT_TOP_K     = 8;

    @Inject EmbeddingService embeddings;

    private VectorStore store;

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        Path storeFile = Path.of(System.getProperty("user.home"),
                ".gamelan", "memory", project, "semantic.vec");
        store = new VectorStore(storeFile, "semantic");
        log.info("[semantic-store] loaded {} facts", store.size());
    }

    // ── Write ──────────────────────────────────────────────────────────────

    /**
     * Stores a semantic fact, replacing any existing fact with the same topic.
     *
     * @param topic   stable key (e.g. "db-migration-tool")
     * @param fact    full fact text (e.g. "This project uses Flyway. Migrations in src/main/resources/db/migration")
     * @param type    memory type: FACT | PREFERENCE | DECISION | COMMAND | PROCEDURE
     * @param project project name or "_global"
     */
    public void store(String topic, String fact, String type, String project) {
        float[] vec = embeddings.embed(topic + ". " + fact); // embed topic + fact together

        Map<String, String> meta = Map.of(
                "type",    type,
                "topic",   topic,
                "project", project
        );

        // Upsert with stable ID derived from topic+project (for deduplication)
        String id = stableId(topic, project);
        if (vec.length > 0) {
            store.upsert(id, fact, vec, meta);
        } else {
            // Embedding unavailable — store as tombstone with zero-length vector
            // so keyword fallback can still find it
            store.upsert(id, fact, new float[0], meta);
        }
        log.debug("[semantic-store] stored '{}' dims={}", topic, vec.length);
    }

    /**
     * Convenience method: store without project (uses current project).
     */
    public void store(String topic, String fact, String type) {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        store(topic, fact, type, project);
    }

    /** Delete a fact by topic. */
    public boolean delete(String topic) {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        return store.delete(stableId(topic, project));
    }

    // ── Search ─────────────────────────────────────────────────────────────

    /**
     * Find the most relevant semantic facts for the given query.
     *
     * <p>When the embedding model is available, uses cosine similarity.
     * Falls back to keyword overlap when unavailable.
     *
     * @param query   natural language query or task description
     * @param topK    max results to return
     * @param project project filter (pass null to include global facts too)
     * @return ranked list of relevant facts
     */
    public List<SemanticFact> search(String query, int topK, String project) {
        if (query == null || query.isBlank()) return List.of();

        if (embeddings.isAvailable()) {
            return vectorSearch(query, topK, project);
        } else {
            log.debug("[semantic-store] embedding unavailable — using keyword fallback");
            return keywordSearch(query, topK, project);
        }
    }

    /** Convenience: search with default topK. */
    public List<SemanticFact> search(String query) {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        return search(query, DEFAULT_TOP_K, project);
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** All stored facts (any project). */
    public List<SemanticFact> all() {
        return store.all().stream()
                .map(this::toFact)
                .toList();
    }

    /** All facts for a specific project + global facts. */
    public List<SemanticFact> forProject(String project) {
        return store.all().stream()
                .filter(e -> project.equals(e.metadata().get("project"))
                        || "_global".equals(e.metadata().get("project")))
                .map(this::toFact)
                .toList();
    }

    public int size() { return store.size(); }

    // ── Private ────────────────────────────────────────────────────────────

    private List<SemanticFact> vectorSearch(String query, int topK, String project) {
        float[] qVec = embeddings.embed(query);
        if (qVec.length == 0) return keywordSearch(query, topK, project);

        Map<String, String> filter = project != null
                ? null   // we filter post-search to include both project + global
                : null;

        return store.search(qVec, topK * 2, DEFAULT_MIN_SCORE, filter)
                .stream()
                .filter(r -> project == null
                        || project.equals(r.metaGet("project"))
                        || "_global".equals(r.metaGet("project")))
                .limit(topK)
                .map(r -> new SemanticFact(
                        r.metaGet("topic"),
                        r.text(),
                        r.metaGet("type"),
                        r.metaGet("project"),
                        r.score()))
                .toList();
    }

    private List<SemanticFact> keywordSearch(String query, int topK, String project) {
        String lower = query.toLowerCase();
        Set<String> queryTokens = tokenise(lower);

        return store.all().stream()
                .filter(e -> project == null
                        || project.equals(e.metadata().get("project"))
                        || "_global".equals(e.metadata().get("project")))
                .map(e -> {
                    // BM25-lite: score by token overlap
                    String text  = (e.text() + " " + e.metadata().getOrDefault("topic", "")).toLowerCase();
                    Set<String> entryTokens = tokenise(text);
                    long overlap = queryTokens.stream().filter(entryTokens::contains).count();
                    float score = overlap == 0 ? 0f : (float) overlap / queryTokens.size();
                    return new VectorStore.SearchResult(e, score);
                })
                .filter(r -> r.score() > 0)
                .sorted(Comparator.comparingDouble(VectorStore.SearchResult::score).reversed())
                .limit(topK)
                .map(r -> new SemanticFact(
                        r.metaGet("topic"),
                        r.text(),
                        r.metaGet("type"),
                        r.metaGet("project"),
                        r.score()))
                .toList();
    }

    private SemanticFact toFact(VectorStore.VectorEntry e) {
        return new SemanticFact(
                e.metadata().getOrDefault("topic", e.id()),
                e.text(),
                e.metadata().getOrDefault("type", "FACT"),
                e.metadata().getOrDefault("project", ""),
                0f);
    }

    private Set<String> tokenise(String text) {
        Set<String> tokens = new HashSet<>();
        for (String w : text.split("[\\s,;.!?\\-_/]+")) {
            if (w.length() >= 3) tokens.add(w.toLowerCase());
        }
        return tokens;
    }

    private String stableId(String topic, String project) {
        return project + "::" + topic.toLowerCase().replace(" ", "-");
    }

    // ── Types ──────────────────────────────────────────────────────────────

    /**
     * A retrieved semantic fact with its similarity score.
     *
     * @param topic   the stable topic key
     * @param fact    the full fact text
     * @param type    memory type (FACT / PREFERENCE / DECISION / COMMAND / PROCEDURE)
     * @param project project name or "_global"
     * @param score   cosine similarity (0 when retrieved by fallback)
     */
    public record SemanticFact(
            String topic,
            String fact,
            String type,
            String project,
            float  score
    ) {}
}
