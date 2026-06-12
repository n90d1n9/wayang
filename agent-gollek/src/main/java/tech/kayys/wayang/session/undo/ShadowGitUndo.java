package tech.kayys.gamelan.session.undo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ShadowGitUndo — per-step undo via a shadow git repository for comprehensive rollback.
 *
 * <h2>From the OPENDEV paper (§2.5.2 — Shadow git snapshots)</h2>
 * The in-memory undo log tracks only file operations performed through the agent's tools.
 * It cannot capture side effects from shell commands (e.g., npm install modifying
 * package-lock.json) or build processes. For comprehensive per-step undo, the system maintains
 * a shadow git repository, a bare repository at ~/.gamelan/snapshot/<project-id>/ that shares
 * no history with the user's actual repository.
 *
 * <p>At every agent step that modifies files, the snapshot system runs:
 * {@code git add . && git write-tree} against the project working directory using the shadow
 * repository's object store, recording a tree hash in the session metadata. The /undo command
 * computes a git diff between the current tree and the snapshot tree, identifies changed files,
 * and restores them via {@code git checkout <hash> -- <file>}.
 *
 * <h2>Implementation details from the paper</h2>
 * <ul>
 *   <li>Shadow repo's .gitignore is synchronized from the real repository to avoid tracking
 *       build artifacts</li>
 *   <li>Scheduled cleanup: {@code git gc --prune=7.days} keeps the shadow repo compact</li>
 *   <li>History cap: in-memory undo log bounded at 50 operations to prevent OOM</li>
 *   <li>Operates alongside version control — not a replacement for git</li>
 * </ul>
 *
 * <h2>Why not just git stash?</h2>
 * Stash requires clean working tree. The agent may be mid-edit. The shadow repo is a private
 * content-addressable store — operations never interfere with the user's git history.
 */
@ApplicationScoped
public class ShadowGitUndo {

    private static final Logger log = LoggerFactory.getLogger(ShadowGitUndo.class);

    // Paper constant: in-memory history cap
    private static final int MAX_HISTORY = 50;

    @Inject AgentTelemetry telemetry;

    private final Deque<SnapshotEntry>  history     = new ArrayDeque<>(MAX_HISTORY);
    private final AtomicInteger         stepCounter = new AtomicInteger(0);
    private volatile Path               shadowRepo;
    private volatile Path               projectRoot;
    private volatile boolean            initialized  = false;

    // ── Initialization ─────────────────────────────────────────────────────

    /**
     * Initializes the shadow git repository for the current project.
     * Creates a bare repo at ~/.gamelan/snapshot/<project-id>/ if it doesn't exist.
     *
     * @param workingDir the project's working directory
     */
    public void initialize(Path workingDir) {
        this.projectRoot = workingDir.toAbsolutePath().normalize();
        String projectId = projectRoot.getFileName().toString() +
                           "-" + Integer.toHexString(projectRoot.hashCode());
        Path snapshotBase = Path.of(System.getProperty("user.home"), ".gamelan", "snapshot");
        this.shadowRepo = snapshotBase.resolve(projectId);

        try {
            Files.createDirectories(snapshotBase);
            if (!Files.exists(shadowRepo)) {
                exec("git", "init", "--bare", shadowRepo.toString());
                log.info("[shadow-git] initialized shadow repo at {}", shadowRepo);
            }
            syncGitignore();
            initialized = true;
            telemetry.count("shadow_git.initialized");
        } catch (Exception e) {
            log.warn("[shadow-git] init failed: {} — undo will use in-memory log only",
                    e.getMessage());
        }
    }

    // ── Snapshot API ───────────────────────────────────────────────────────

    /**
     * Takes a snapshot of the current working directory state.
     * Records the git tree hash in the history.
     * Call at the start of each agent step that will modify files.
     *
     * @param stepName  human-readable description of the upcoming step
     * @return the snapshot entry, or empty if git is unavailable
     */
    public Optional<SnapshotEntry> snapshot(String stepName) {
        if (!initialized) return Optional.empty();

        int stepId = stepCounter.incrementAndGet();
        Instant ts = Instant.now();

        try {
            // Stage all tracked files in the shadow repo
            String treeHash = captureTreeHash();
            if (treeHash == null || treeHash.isBlank()) return Optional.empty();

            SnapshotEntry entry = new SnapshotEntry(stepId, stepName, treeHash, ts,
                    List.copyOf(stagedFiles()));

            // Enforce history cap (paper: bounded to 50)
            synchronized (history) {
                if (history.size() >= MAX_HISTORY) {
                    history.pollFirst(); // evict oldest
                }
                history.addLast(entry);
            }

            log.debug("[shadow-git] snapshot #{}: {} tree={}", stepId, stepName,
                    treeHash.substring(0, 8));
            telemetry.count("shadow_git.snapshot.taken");
            return Optional.of(entry);

        } catch (Exception e) {
            log.warn("[shadow-git] snapshot failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Undoes all changes made since the most recent snapshot.
     * Restores files that differ between the current state and the snapshot tree.
     *
     * @return an UndoResult describing what was restored
     */
    public UndoResult undo() {
        SnapshotEntry entry;
        synchronized (history) {
            entry = history.pollLast();
        }
        if (entry == null) {
            return UndoResult.noHistory();
        }
        return restoreFromTree(entry);
    }

    /**
     * Undoes to a specific step by ID.
     *
     * @param stepId the step to restore to
     */
    public UndoResult undoTo(int stepId) {
        SnapshotEntry target = null;
        synchronized (history) {
            Iterator<SnapshotEntry> it = history.descendingIterator();
            while (it.hasNext()) {
                SnapshotEntry e = it.next();
                if (e.stepId() == stepId) { target = e; break; }
            }
        }
        if (target == null) return UndoResult.notFound(stepId);
        return restoreFromTree(target);
    }

    /** Returns the undo history (most recent first). */
    public List<SnapshotEntry> history() {
        synchronized (history) {
            List<SnapshotEntry> list = new ArrayList<>(history);
            Collections.reverse(list);
            return list;
        }
    }

    /** Returns true if there is at least one snapshot to undo to. */
    public boolean canUndo() {
        synchronized (history) { return !history.isEmpty(); }
    }

    /**
     * Schedules a cleanup pass on the shadow repository (git gc --prune=7.days).
     * Called on session end or periodically.
     */
    public void scheduleCleanup() {
        if (!initialized) return;
        Thread.ofVirtual().start(() -> {
            try {
                exec("git", "--git-dir=" + shadowRepo,
                        "gc", "--prune=7.days", "--quiet");
                log.debug("[shadow-git] gc complete");
                telemetry.count("shadow_git.gc.ran");
            } catch (Exception e) {
                log.debug("[shadow-git] gc failed: {}", e.getMessage());
            }
        });
    }

    /** Clears the in-memory history (does not delete shadow repo objects). */
    public void clearHistory() {
        synchronized (history) { history.clear(); }
        stepCounter.set(0);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String captureTreeHash() throws IOException, InterruptedException {
        if (projectRoot == null) return null;
        // Use shadow repo's GIT_DIR but project's working tree
        ProcessResult addResult = execCapture(
                "git", "--git-dir=" + shadowRepo, "--work-tree=" + projectRoot,
                "add", "--all", "--ignore-errors");
        if (addResult.exitCode() != 0) {
            log.debug("[shadow-git] add warning: {}", addResult.output());
        }
        ProcessResult treeResult = execCapture(
                "git", "--git-dir=" + shadowRepo, "--work-tree=" + projectRoot,
                "write-tree");
        return treeResult.exitCode() == 0 ? treeResult.output().strip() : null;
    }

    private List<String> stagedFiles() {
        if (projectRoot == null) return List.of();
        try {
            ProcessResult result = execCapture(
                    "git", "--git-dir=" + shadowRepo, "--work-tree=" + projectRoot,
                    "diff-index", "--name-only", "--cached", "HEAD");
            if (result.exitCode() == 0 && !result.output().isBlank()) {
                return Arrays.asList(result.output().strip().split("\n"));
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private UndoResult restoreFromTree(SnapshotEntry entry) {
        if (!initialized || projectRoot == null) {
            return UndoResult.failed(entry, "shadow git not initialized");
        }
        try {
            // Compute diff between current state and snapshot tree
            ProcessResult diffResult = execCapture(
                    "git", "--git-dir=" + shadowRepo, "--work-tree=" + projectRoot,
                    "diff-tree", "--no-commit-id", "-r", "--name-only",
                    entry.treeHash(), "HEAD");

            List<String> changedFiles = new ArrayList<>();
            if (diffResult.exitCode() == 0 && !diffResult.output().isBlank()) {
                changedFiles = Arrays.asList(diffResult.output().strip().split("\n"));
            }

            // Restore each changed file from the snapshot tree
            List<String> restored = new ArrayList<>();
            for (String file : changedFiles) {
                ProcessResult checkoutResult = execCapture(
                        "git", "--git-dir=" + shadowRepo, "--work-tree=" + projectRoot,
                        "checkout", entry.treeHash(), "--", file);
                if (checkoutResult.exitCode() == 0) {
                    restored.add(file);
                }
            }

            telemetry.count("shadow_git.undo.completed");
            log.info("[shadow-git] restored {} files to step #{} ({})",
                    restored.size(), entry.stepId(), entry.stepName());
            return new UndoResult(true, entry, restored, null);

        } catch (Exception e) {
            log.warn("[shadow-git] undo failed: {}", e.getMessage());
            return UndoResult.failed(entry, e.getMessage());
        }
    }

    private void syncGitignore() {
        Path realIgnore = projectRoot != null ? projectRoot.resolve(".gitignore") : null;
        if (realIgnore == null || !Files.exists(realIgnore)) return;
        try {
            String content = Files.readString(realIgnore);
            Path shadowIgnore = shadowRepo.resolve("info").resolve("exclude");
            Files.createDirectories(shadowIgnore.getParent());
            Files.writeString(shadowIgnore, "# Synced from project .gitignore\n" + content);
        } catch (IOException e) {
            log.debug("[shadow-git] gitignore sync failed: {}", e.getMessage());
        }
    }

    private void exec(String... cmd) throws IOException, InterruptedException {
        new ProcessBuilder(cmd).start().waitFor();
    }

    private ProcessResult execCapture(String... cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd)
                .directory(projectRoot != null ? projectRoot.toFile() : null)
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        return new ProcessResult(code, out);
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record SnapshotEntry(
            int          stepId,
            String       stepName,
            String       treeHash,
            Instant      timestamp,
            List<String> stagedFiles
    ) {
        public String shortHash() { return treeHash.length() >= 8 ? treeHash.substring(0, 8) : treeHash; }
        public String display()   { return "#" + stepId + " " + stepName + " [" + shortHash() + "]"; }
    }

    public record UndoResult(
            boolean       success,
            SnapshotEntry entry,
            List<String>  restoredFiles,
            String        errorMessage
    ) {
        static UndoResult noHistory() {
            return new UndoResult(false, null, List.of(), "No snapshots in undo history");
        }
        static UndoResult notFound(int stepId) {
            return new UndoResult(false, null, List.of(), "Step #" + stepId + " not found in history");
        }
        static UndoResult failed(SnapshotEntry e, String msg) {
            return new UndoResult(false, e, List.of(), msg);
        }
        public String summary() {
            if (!success) return "Undo failed: " + errorMessage;
            return String.format("Restored %d file(s) to step %s",
                    restoredFiles.size(), entry != null ? entry.display() : "unknown");
        }
    }

    private record ProcessResult(int exitCode, String output) {}
}
