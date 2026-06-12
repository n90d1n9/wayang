package tech.kayys.gamelan.planning.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Plan Version Store — persistent, queryable versioning for execution plans.
 *
 * <h2>Why version plans</h2>
 * <ol>
 *   <li><b>Debugging</b>: when a run fails, compare the failed plan against a
 *       historically successful plan for the same task type</li>
 *   <li><b>A/B testing</b>: run two plan variants in shadow mode and compare
 *       execution metrics</li>
 *   <li><b>Rollback</b>: if a new plan strategy performs worse, roll back to
 *       the known-good version</li>
 *   <li><b>Transfer</b>: find the best plan for a task similar to a new one
 *       using semantic similarity across plan fingerprints</li>
 *   <li><b>Audit</b>: show exactly what plan was used for a production execution</li>
 * </ol>
 *
 * <h2>Version model</h2>
 * <pre>
 * PlanVersion
 *   ├── id             UUID
 *   ├── fingerprint    SHA-256 of normalized task text
 *   ├── plan           HierarchicalTaskPlanner.Plan (full serialized plan)
 *   ├── metrics        PlanMetrics (actual performance from execution)
 *   ├── tags           ["production", "shadow", "failed"]
 *   └── createdAt
 * </pre>
 *
 * <h2>Comparison</h2>
 * {@link #compare} produces a structured diff:
 * <ul>
 *   <li>Tasks added/removed</li>
 *   <li>Execution mode changed (SEQUENTIAL → PARALLEL)</li>
 *   <li>Estimated token changes</li>
 *   <li>Actual performance delta (if both versions have metrics)</li>
 * </ul>
 */
@ApplicationScoped
public class PlanVersionStore {

    private static final Logger log = LoggerFactory.getLogger(PlanVersionStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final int MAX_VERSIONS_PER_FINGERPRINT = 20;

    private final Map<String, Deque<PlanVersion>> store = new ConcurrentHashMap<>();
    private final Path persistDir;

    public PlanVersionStore() {
        this.persistDir = Path.of(System.getProperty("user.home"),
                ".gamelan", "plans", "versions");
        load();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Stores a new plan version and returns its assigned ID.
     *
     * @param task  the task this plan was created for
     * @param plan  the execution plan
     * @param tags  optional labels ("production", "shadow", "candidate")
     * @return the version ID
     */
    public String store(String task, HierarchicalTaskPlanner.Plan plan, String... tags) {
        String fingerprint = fingerprint(task);
        String versionId   = UUID.randomUUID().toString();

        PlanVersion version = new PlanVersion(versionId, fingerprint, task,
                plan, null, Set.of(tags), Instant.now());

        Deque<PlanVersion> versions = store.computeIfAbsent(fingerprint,
                k -> new ArrayDeque<>());
        versions.addFirst(version);

        // Trim to max versions per fingerprint
        while (versions.size() > MAX_VERSIONS_PER_FINGERPRINT) {
            versions.pollLast();
        }

        persist(fingerprint, new ArrayList<>(versions));
        log.debug("[plan-version] stored version {} for fingerprint {}", versionId, fingerprint.substring(0,8));
        return versionId;
    }

    /**
     * Records actual execution metrics for a plan version.
     */
    public void recordMetrics(String versionId, PlanMetrics metrics) {
        store.values().forEach(versions -> versions.stream()
                .filter(v -> v.id().equals(versionId))
                .findFirst()
                .ifPresent(v -> {
                    versions.remove(v);
                    versions.addFirst(v.withMetrics(metrics));
                    persist(v.fingerprint(), new ArrayList<>(versions));
                }));
    }

    /**
     * Finds the best historical plan for a task (highest success rate with metrics).
     */
    public Optional<PlanVersion> findBest(String task) {
        String fp = fingerprint(task);
        Deque<PlanVersion> versions = store.get(fp);
        if (versions == null || versions.isEmpty()) return Optional.empty();

        return versions.stream()
                .filter(v -> v.metrics() != null)
                .max(Comparator.comparingDouble(v -> v.metrics().successRate()));
    }

    /**
     * Returns all versions for a task, newest first.
     */
    public List<PlanVersion> history(String task) {
        Deque<PlanVersion> versions = store.get(fingerprint(task));
        return versions == null ? List.of() : List.copyOf(versions);
    }

    /**
     * Retrieves a specific version by ID.
     */
    public Optional<PlanVersion> get(String versionId) {
        return store.values().stream()
                .flatMap(Collection::stream)
                .filter(v -> v.id().equals(versionId))
                .findFirst();
    }

    /**
     * Produces a structured comparison between two plan versions.
     */
    public PlanDiff compare(String versionIdA, String versionIdB) {
        Optional<PlanVersion> a = get(versionIdA);
        Optional<PlanVersion> b = get(versionIdB);

        if (a.isEmpty() || b.isEmpty()) {
            return PlanDiff.notFound(versionIdA, versionIdB);
        }

        HierarchicalTaskPlanner.Plan planA = a.get().plan();
        HierarchicalTaskPlanner.Plan planB = b.get().plan();

        Set<String> tasksA = planA.tasks().stream()
                .map(HierarchicalTaskPlanner.TaskNode::task).collect(java.util.stream.Collectors.toSet());
        Set<String> tasksB = planB.tasks().stream()
                .map(HierarchicalTaskPlanner.TaskNode::task).collect(java.util.stream.Collectors.toSet());

        Set<String> added   = new HashSet<>(tasksB); added.removeAll(tasksA);
        Set<String> removed = new HashSet<>(tasksA); removed.removeAll(tasksB);

        boolean modeChanged  = planA.mode() != planB.mode();
        int     tokenDelta   = planB.estimatedTokens() - planA.estimatedTokens();

        Double perfDelta = null;
        if (a.get().metrics() != null && b.get().metrics() != null) {
            perfDelta = b.get().metrics().successRate() - a.get().metrics().successRate();
        }

        return new PlanDiff(versionIdA, versionIdB, added, removed,
                modeChanged, planA.mode(), planB.mode(),
                tokenDelta, perfDelta);
    }

    /**
     * Tags a version (e.g., mark as "production-proven" or "rolled-back").
     */
    public void tag(String versionId, String tag) {
        store.values().forEach(versions -> {
            Optional<PlanVersion> found = versions.stream()
                    .filter(v -> v.id().equals(versionId)).findFirst();
            found.ifPresent(v -> {
                Set<String> newTags = new HashSet<>(v.tags());
                newTags.add(tag);
                versions.remove(v);
                versions.addFirst(v.withTags(newTags));
            });
        });
    }

    /**
     * Returns all plan versions marked with a specific tag.
     */
    public List<PlanVersion> findByTag(String tag) {
        return store.values().stream()
                .flatMap(Collection::stream)
                .filter(v -> v.tags().contains(tag))
                .sorted(Comparator.comparing(PlanVersion::createdAt).reversed())
                .toList();
    }

    public int totalVersions() { return store.values().stream().mapToInt(Deque::size).sum(); }

    // ── Persistence ────────────────────────────────────────────────────────

    private void persist(String fingerprint, List<PlanVersion> versions) {
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(persistDir);
                MAPPER.writerWithDefaultPrettyPrinter()
                      .writeValue(persistDir.resolve(fingerprint.substring(0,8) + ".json").toFile(),
                              versions);
            } catch (IOException e) {
                log.warn("[plan-version] persist failed: {}", e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(persistDir)) return;
        try (var stream = Files.list(persistDir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                try {
                    List<PlanVersion> versions = MAPPER.readValue(file.toFile(),
                            MAPPER.getTypeFactory().constructCollectionType(List.class, PlanVersion.class));
                    if (!versions.isEmpty()) {
                        Deque<PlanVersion> deque = new ArrayDeque<>(versions);
                        store.put(versions.get(0).fingerprint(), deque);
                    }
                } catch (IOException e) {
                    log.warn("[plan-version] load failed for {}: {}", file, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("[plan-version] directory load failed: {}", e.getMessage());
        }
    }

    private String fingerprint(String task) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(normalize(task).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return task.hashCode() + ""; }
    }

    private String normalize(String task) {
        return task.toLowerCase().replaceAll("[^a-z0-9 ]", "").strip();
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record PlanVersion(
            String                         id,
            String                         fingerprint,
            String                         task,
            HierarchicalTaskPlanner.Plan   plan,
            PlanMetrics                    metrics,
            Set<String>                    tags,
            Instant                        createdAt
    ) {
        PlanVersion withMetrics(PlanMetrics m) {
            return new PlanVersion(id, fingerprint, task, plan, m, tags, createdAt);
        }
        PlanVersion withTags(Set<String> t) {
            return new PlanVersion(id, fingerprint, task, plan, metrics, t, createdAt);
        }
        boolean hasMetrics() { return metrics != null; }
    }

    public record PlanMetrics(
            boolean success,
            double  successRate,
            long    actualDurationMs,
            int     actualToolCalls,
            int     actualTokens,
            String  failureReason
    ) {
        public static PlanMetrics success(long ms, int tools, int tokens) {
            return new PlanMetrics(true, 1.0, ms, tools, tokens, null);
        }
        public static PlanMetrics failure(long ms, String reason) {
            return new PlanMetrics(false, 0.0, ms, 0, 0, reason);
        }
    }

    public record PlanDiff(
            String         versionIdA,
            String         versionIdB,
            Set<String>    tasksAdded,
            Set<String>    tasksRemoved,
            boolean        modeChanged,
            HierarchicalTaskPlanner.ExecutionMode modeA,
            HierarchicalTaskPlanner.ExecutionMode modeB,
            int            tokenDelta,
            Double         performanceDelta
    ) {
        static PlanDiff notFound(String a, String b) {
            return new PlanDiff(a, b, Set.of(), Set.of(), false, null, null, 0, null);
        }
        public String summary() {
            return String.format("Diff[%s→%s]: +%d -%d tasks | mode=%s | tokens%+d | perf%s",
                    versionIdA.substring(0,8), versionIdB.substring(0,8),
                    tasksAdded.size(), tasksRemoved.size(),
                    modeChanged ? modeA + "→" + modeB : "same",
                    tokenDelta,
                    performanceDelta == null ? "=?" : String.format("%+.0f%%", performanceDelta * 100));
        }
    }
}
