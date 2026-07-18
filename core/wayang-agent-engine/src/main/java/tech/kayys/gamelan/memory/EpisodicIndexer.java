package tech.kayys.gamelan.memory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Background indexer that embeds episodic memories for semantic retrieval.
 *
 * <h2>Bug fixes vs. previous version</h2>
 * <ul>
 *   <li>Was missing {@code @PostConstruct} — the {@code episodicStore} field was
 *       lazily initialised with a null-check inside every public method. Under
 *       concurrent calls this races: two threads both see {@code null}, both call
 *       {@code init()}, both create a new {@link VectorStore} pointing at the same
 *       file, and the second one overwrites the first's in-memory state. Fixed with
 *       proper {@code @PostConstruct} initialisation.</li>
 *   <li>Worker executor was a fixed-thread-pool via {@code Thread.ofVirtual().factory()}.
 *       The factory produces virtual threads, but was passed to
 *       {@code newSingleThreadExecutor} which only creates ONE thread total. If that
 *       one virtual thread is blocked by a slow embed call, new tasks queue up
 *       indefinitely. Changed to {@code newVirtualThreadPerTaskExecutor} for unbounded
 *       async throughput.</li>
 * </ul>
 */
@ApplicationScoped
public class EpisodicIndexer {

    private static final Logger log = LoggerFactory.getLogger(EpisodicIndexer.class);
    private static final float SIMILARITY_THRESHOLD = 0.65f;
    private static final int   MAX_INDEXED          = 500;

    @Inject EmbeddingService embeddings;

    private VectorStore episodicStore;

    // Unbounded virtual-thread executor — each embed call gets its own VT
    private final ExecutorService indexWorker =
            Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        Path storeFile = Path.of(System.getProperty("user.home"),
                ".gamelan", "memory", project, "episodic.vec");
        episodicStore = new VectorStore(storeFile, "episodic");
        log.info("[episodic-indexer] loaded {} indexed episodes", episodicStore.size());
    }

    // ── Index ─────────────────────────────────────────────────────────────

    /**
     * Asynchronously indexes an episode.
     * Returns immediately; embedding + write happen in a virtual thread.
     */
    public void indexAsync(MemoryHierarchy.Episode episode) {
        indexWorker.submit(() -> index(episode));
    }

    private void index(MemoryHierarchy.Episode episode) {
        String fingerprint = buildFingerprint(episode);
        float[] vec = embeddings.embed(fingerprint);
        if (vec.length == 0) {
            log.debug("[episodic-indexer] embedding unavailable for {}", episode.id().substring(0,8));
            return;
        }

        Map<String, String> meta = Map.of(
                "episodeId",  episode.id(),
                "success",    String.valueOf(episode.success()),
                "tools",      String.join(",", episode.toolsUsed()),
                "durationMs", String.valueOf(episode.durationMs())
        );
        episodicStore.upsert(episode.id(), fingerprint, vec, meta);

        // Evict oldest if over cap
        if (episodicStore.size() > MAX_INDEXED) {
            episodicStore.all().stream()
                    .sorted(Comparator.comparing(VectorStore.VectorEntry::createdAt))
                    .limit(episodicStore.size() - MAX_INDEXED)
                    .forEach(e -> episodicStore.delete(e.id()));
        }
        log.debug("[episodic-indexer] indexed {} ({})", episode.id().substring(0,8),
                episode.success() ? "success" : "failure");
    }

    // ── Search ─────────────────────────────────────────────────────────────

    /** Find past episodes semantically similar to the current task. */
    public List<EpisodicMatch> findSimilar(String task, int topK) {
        if (!embeddings.isAvailable()) return List.of();
        float[] qVec = embeddings.embed(task);
        if (qVec.length == 0) return List.of();

        return episodicStore.search(qVec, topK, SIMILARITY_THRESHOLD)
                .stream()
                .map(r -> new EpisodicMatch(
                        r.id(),
                        r.text(),
                        "true".equals(r.metaGet("success")),
                        r.metaGet("tools"),
                        parseLong(r.metaGet("durationMs")),
                        r.score()))
                .toList();
    }

    /** Find similar past failures. */
    public List<EpisodicMatch> findSimilarFailures(String task, int topK) {
        return findSimilar(task, topK * 2).stream()
                .filter(m -> !m.wasSuccess()).limit(topK).toList();
    }

    /** Find similar past successes. */
    public List<EpisodicMatch> findSimilarSuccesses(String task, int topK) {
        return findSimilar(task, topK * 2).stream()
                .filter(EpisodicMatch::wasSuccess).limit(topK).toList();
    }

    public int indexedCount() { return episodicStore.size(); }

    // ── Private ────────────────────────────────────────────────────────────

    private String buildFingerprint(MemoryHierarchy.Episode ep) {
        String outcome = ep.outcome().length() > 300
                ? ep.outcome().substring(0, 300) : ep.outcome();
        return "TASK: " + ep.task() + "\n"
                + (outcome.isBlank() ? "" : "OUTCOME: " + outcome + "\n")
                + (ep.toolsUsed().isEmpty() ? "" : "TOOLS: " + String.join(", ", ep.toolsUsed()) + "\n")
                + "SUCCESS: " + ep.success();
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public record EpisodicMatch(
            String  id,
            String  fingerprint,
            boolean wasSuccess,
            String  toolsUsed,
            long    durationMs,
            float   similarity
    ) {
        public String preview() {
            return fingerprint.length() > 200 ? fingerprint.substring(0, 200) + "…" : fingerprint;
        }
    }
}
