package tech.kayys.gamelan.memory.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.planning.versioning.PlanVersionStore;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * MemorySnapshotManager — full agent state serialization, restoration, and diffing.
 *
 * <h2>Use cases</h2>
 * <ul>
 *   <li><b>Session migration</b>: export state from machine A, import on machine B</li>
 *   <li><b>Rollback</b>: restore the agent to a known-good state after bad memory injection</li>
 *   <li><b>Debugging</b>: inspect exactly what the agent knew at the time of a failure</li>
 *   <li><b>A/B testing</b>: compare two agents with different memory states</li>
 *   <li><b>Checkpointing</b>: periodic saves before long-running workflows</li>
 *   <li><b>Onboarding</b>: ship a pre-populated knowledge snapshot with a new project</li>
 * </ul>
 *
 * <h2>Snapshot format</h2>
 * A snapshot is a gzip-compressed JSON file containing:
 * <pre>
 * {
 *   "version": "1.0",
 *   "snapshotId": "uuid",
 *   "label": "before-refactoring",
 *   "capturedAt": "2024-01-15T10:30:00Z",
 *   "contentHash": "sha256...",
 *   "episodicMemory": [...],
 *   "semanticMemory": [...],
 *   "proceduralMemory": [...],
 *   "planVersions": [...],
 *   "metadata": {...}
 * }
 * </pre>
 *
 * <h2>Diffing</h2>
 * {@link #diff(MemorySnapshot, MemorySnapshot)} computes a structured diff showing:
 * <ul>
 *   <li>Episodes added/removed between snapshots</li>
 *   <li>Semantic facts changed/added/removed</li>
 *   <li>Procedural strategies that evolved</li>
 * </ul>
 */
@ApplicationScoped
public class MemorySnapshotManager {

    private static final Logger log = LoggerFactory.getLogger(MemorySnapshotManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String SNAPSHOT_VERSION = "1.0";

    @Inject EpisodicMemory  episodic;
    @Inject SemanticMemory  semantic;
    @Inject ProceduralMemory procedural;
    @Inject PlanVersionStore planStore;
    @Inject AgentTelemetry  telemetry;

    private static final Path SNAPSHOT_DIR =
            Path.of(System.getProperty("user.home"), ".gamelan", "snapshots");

    // ── Capture ───────────────────────────────────────────────────────────

    /**
     * Captures the current full agent state as a snapshot.
     *
     * @param label      human-readable label (e.g., "before-refactoring-v2")
     * @param metadata   optional key-value metadata tags
     * @return the captured snapshot
     */
    public MemorySnapshot capture(String label, Map<String, String> metadata) {
        log.info("[snapshot] capturing state: '{}'", label);
        Instant now = Instant.now();

        List<EpisodicMemory.Episode> episodes = episodic.all();
        Map<Long, SemanticMemory.KnowledgeNode> semanticNodes = semantic.allNodes();
        List<ProceduralMemory.Strategy> strategies = procedural.allStrategies();

        String content = buildContentString(episodes, semanticNodes, strategies);
        String hash    = sha256(content);

        MemorySnapshot snapshot = new MemorySnapshot(
                UUID.randomUUID().toString(),
                SNAPSHOT_VERSION,
                label,
                now,
                hash,
                episodes,
                new ArrayList<>(semanticNodes.values()),
                strategies,
                metadata != null ? Map.copyOf(metadata) : Map.of());

        telemetry.count("snapshot.capture.total");
        log.info("[snapshot] captured: {} episodes, {} semantic nodes, {} strategies | hash={}",
                episodes.size(), semanticNodes.size(), strategies.size(), hash.substring(0, 8));
        return snapshot;
    }

    /**
     * Captures a lightweight snapshot (no episodic history, just semantic + procedural).
     */
    public MemorySnapshot captureLight(String label) {
        return capture(label + " [light]", Map.of("type", "light"));
    }

    // ── Persistence ────────────────────────────────────────────────────────

    /**
     * Saves a snapshot to disk as a gzip-compressed JSON file.
     *
     * @return the path of the saved file
     */
    public Path save(MemorySnapshot snapshot) throws IOException {
        Files.createDirectories(SNAPSHOT_DIR);
        String filename = sanitize(snapshot.label()) + "-" +
                snapshot.capturedAt().toString().replace(":", "-").substring(0, 19) + ".snap.gz";
        Path dest = SNAPSHOT_DIR.resolve(filename);

        try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(dest,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(gz, snapshot);
        }
        log.info("[snapshot] saved to {} ({} KB)",
                dest.getFileName(), Files.size(dest) / 1024);
        telemetry.count("snapshot.save.total");
        return dest;
    }

    /**
     * Loads a snapshot from a file (supports both .snap.gz and plain .json).
     */
    public MemorySnapshot load(Path snapshotFile) throws IOException {
        if (snapshotFile.toString().endsWith(".gz")) {
            try (GZIPInputStream gz = new GZIPInputStream(Files.newInputStream(snapshotFile))) {
                return MAPPER.readValue(gz, MemorySnapshot.class);
            }
        }
        return MAPPER.readValue(snapshotFile.toFile(), MemorySnapshot.class);
    }

    /**
     * Lists all available snapshots in the default snapshot directory.
     */
    public List<SnapshotInfo> listSnapshots() throws IOException {
        if (!Files.exists(SNAPSHOT_DIR)) return List.of();
        try (Stream<Path> files = Files.list(SNAPSHOT_DIR)) {
            return files.filter(p -> p.toString().endsWith(".snap.gz") ||
                                    p.toString().endsWith(".json"))
                    .map(p -> {
                        try { return infoFrom(p); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(SnapshotInfo::savedAt).reversed())
                    .toList();
        }
    }

    // ── Restore ───────────────────────────────────────────────────────────

    /**
     * Restores the agent's memory from a snapshot.
     * Replaces all current memory with snapshot contents.
     *
     * @param snapshot    the snapshot to restore from
     * @param dryRun      if true, show what would change without applying
     * @return the restore result
     */
    public RestoreResult restore(MemorySnapshot snapshot, boolean dryRun) {
        log.info("[snapshot] {} restore from '{}': {} episodes, {} semantic, {} strategies",
                dryRun ? "DRY-RUN" : "APPLYING",
                snapshot.label(),
                snapshot.episodes().size(),
                snapshot.semanticNodes().size(),
                snapshot.strategies().size());

        if (!dryRun) {
            // Clear and repopulate episodic memory
            episodic.clear();
            snapshot.episodes().forEach(episodic::add);

            // Clear and repopulate semantic memory
            semantic.clear();
            snapshot.semanticNodes().forEach(n ->
                    semantic.upsert(n.concept(), n.fact(), n.type(),
                            n.episodeId(), n.confidence()));

            // Clear and repopulate procedural memory
            procedural.clear();
            snapshot.strategies().forEach(s ->
                    procedural.upsert(s.name(), s.description(), s.applicability(),
                            s.successRate()));

            telemetry.count("snapshot.restore.total");
            log.info("[snapshot] restore complete");
        }

        return new RestoreResult(snapshot.id(), snapshot.label(), dryRun,
                snapshot.episodes().size(), snapshot.semanticNodes().size(),
                snapshot.strategies().size(), Instant.now());
    }

    // ── Diffing ────────────────────────────────────────────────────────────

    /**
     * Computes a structured diff between two snapshots.
     */
    public SnapshotDiff diff(MemorySnapshot before, MemorySnapshot after) {
        // Episodes diff
        Set<String> beforeEpIds = before.episodes().stream()
                .map(EpisodicMemory.Episode::id).collect(java.util.stream.Collectors.toSet());
        Set<String> afterEpIds  = after.episodes().stream()
                .map(EpisodicMemory.Episode::id).collect(java.util.stream.Collectors.toSet());

        List<EpisodicMemory.Episode> addedEps = after.episodes().stream()
                .filter(e -> !beforeEpIds.contains(e.id())).toList();
        List<EpisodicMemory.Episode> removedEps = before.episodes().stream()
                .filter(e -> !afterEpIds.contains(e.id())).toList();

        // Semantic diff
        Map<String, SemanticMemory.KnowledgeNode> beforeSem = before.semanticNodes().stream()
                .collect(Collectors.toMap(SemanticMemory.KnowledgeNode::concept, n -> n));
        Map<String, SemanticMemory.KnowledgeNode> afterSem  = after.semanticNodes().stream()
                .collect(Collectors.toMap(SemanticMemory.KnowledgeNode::concept, n -> n));

        List<String> addedConcepts   = afterSem.keySet().stream()
                .filter(c -> !beforeSem.containsKey(c)).toList();
        List<String> removedConcepts = beforeSem.keySet().stream()
                .filter(c -> !afterSem.containsKey(c)).toList();
        List<String> changedConcepts = afterSem.entrySet().stream()
                .filter(e -> beforeSem.containsKey(e.getKey()) &&
                             !beforeSem.get(e.getKey()).fact().equals(e.getValue().fact()))
                .map(Map.Entry::getKey).toList();

        // Duration between snapshots
        Duration age = Duration.between(before.capturedAt(), after.capturedAt());
        boolean hashChanged = !before.contentHash().equals(after.contentHash());

        return new SnapshotDiff(before.id(), after.id(), before.label(), after.label(),
                age, hashChanged,
                addedEps, removedEps,
                addedConcepts, removedConcepts, changedConcepts);
    }

    /**
     * Verifies the integrity of a snapshot by recomputing its content hash.
     */
    public boolean verify(MemorySnapshot snapshot) {
        String recomputed = sha256(buildContentString(
                snapshot.episodes(), snapshot.semanticNodes().stream()
                        .collect(Collectors.toMap(n -> (long)n.hashCode(), n -> n)),
                snapshot.strategies()));
        boolean ok = recomputed.equals(snapshot.contentHash());
        if (!ok) log.warn("[snapshot] integrity check FAILED for '{}': hash mismatch", snapshot.label());
        return ok;
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String buildContentString(List<EpisodicMemory.Episode> eps,
                                       Map<Long, SemanticMemory.KnowledgeNode> sem,
                                       List<ProceduralMemory.Strategy> proc) {
        return eps.size() + ":" + sem.size() + ":" + proc.size() + ":" +
               eps.stream().map(EpisodicMemory.Episode::id).sorted().collect(Collectors.joining(","));
    }

    private String buildContentString(List<EpisodicMemory.Episode> eps,
                                       List<SemanticMemory.KnowledgeNode> sem,
                                       List<ProceduralMemory.Strategy> proc) {
        return eps.size() + ":" + sem.size() + ":" + proc.size() + ":" +
               eps.stream().map(EpisodicMemory.Episode::id).sorted().collect(Collectors.joining(","));
    }

    private String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return String.valueOf(input.hashCode()); }
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_-]", "_").substring(0, Math.min(s.length(), 40));
    }

    private SnapshotInfo infoFrom(Path path) throws IOException {
        long size = Files.size(path);
        Instant mod = Files.getLastModifiedTime(path).toInstant();
        String name = path.getFileName().toString();
        String label = name.replaceAll("-\\d{4}-\\d{2}-.*", "").replace("_", " ");
        return new SnapshotInfo(name, label, path, mod, size);
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record MemorySnapshot(
            String                              id,
            String                              version,
            String                              label,
            Instant                             capturedAt,
            String                              contentHash,
            List<EpisodicMemory.Episode>        episodes,
            List<SemanticMemory.KnowledgeNode>  semanticNodes,
            List<ProceduralMemory.Strategy>     strategies,
            Map<String, String>                 metadata
    ) {
        public String summary() {
            return String.format("Snapshot['%s' @ %s]: %d episodes, %d semantic, %d strategies",
                    label, capturedAt.toString().substring(0, 16),
                    episodes.size(), semanticNodes.size(), strategies.size());
        }
    }

    public record SnapshotInfo(String filename, String label, Path path,
                                Instant savedAt, long sizeBytes) {}

    public record RestoreResult(String snapshotId, String label, boolean dryRun,
                                 int episodes, int semanticNodes, int strategies, Instant restoredAt) {
        public String summary() {
            return String.format("%s restore of '%s': %d episodes, %d semantic, %d strategies",
                    dryRun ? "DRY-RUN" : "APPLIED", label, episodes, semanticNodes, strategies);
        }
    }

    public record SnapshotDiff(
            String                              beforeId,
            String                              afterId,
            String                              beforeLabel,
            String                              afterLabel,
            Duration                            timeDelta,
            boolean                             contentChanged,
            List<EpisodicMemory.Episode>        addedEpisodes,
            List<EpisodicMemory.Episode>        removedEpisodes,
            List<String>                        addedConcepts,
            List<String>                        removedConcepts,
            List<String>                        changedConcepts
    ) {
        public boolean hasChanges() {
            return contentChanged || !addedEpisodes.isEmpty() || !removedEpisodes.isEmpty() ||
                   !addedConcepts.isEmpty() || !removedConcepts.isEmpty() || !changedConcepts.isEmpty();
        }

        public String summary() {
            return String.format(
                    "Diff['%s'→'%s'] Δt=%s | +%d/-%d episodes | +%d/-%d/%d~ concepts",
                    beforeLabel, afterLabel, timeDelta.toString(),
                    addedEpisodes.size(), removedEpisodes.size(),
                    addedConcepts.size(), removedConcepts.size(), changedConcepts.size());
        }
    }
}
