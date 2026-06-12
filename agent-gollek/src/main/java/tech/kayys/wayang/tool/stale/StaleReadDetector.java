package tech.kayys.gamelan.tool.stale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * StaleReadDetector — prevents silent overwrites of concurrent edits via file-time tracking.
 *
 * <h2>From the OPENDEV paper (§2.4.2 — read_file: Stale-read tracking)</h2>
 * A FileTimeTracker records the timestamp of each read. The tracker records datetime.now()
 * keyed by (session_id, file_path) on every successful read. Before any edit,
 * assert_fresh() verifies that os.path.getmtime(file_path) <= read_time + 50ms, where the
 * 50ms tolerance accommodates filesystem timestamp granularity (FAT32 rounds to 2-second
 * boundaries; NTFS and ext4 offer sub-millisecond precision, but network filesystems
 * introduce jitter). If the assertion fails, the edit is rejected with an error instructing
 * the agent to re-read the file.
 *
 * <p>A threading.Lock per file path serializes concurrent write attempts to the same file.
 *
 * <h2>Why this matters</h2>
 * Without stale-read detection, an agent can read a file, spend 30 seconds reasoning about
 * what to change, and then overwrite the file — silently clobbering changes the user made
 * in the meantime in their editor. This is especially problematic in interactive sessions
 * where the developer is simultaneously editing while the agent works.
 *
 * <h2>Session scoping</h2>
 * Read timestamps are keyed by (sessionId, filePath) so that multiple concurrent sessions
 * (e.g., parallel workflow steps) each maintain independent read clocks.
 */
@ApplicationScoped
public class StaleReadDetector {

    private static final Logger log = LoggerFactory.getLogger(StaleReadDetector.class);

    // Paper constant: 50ms tolerance for filesystem timestamp granularity
    private static final long FRESHNESS_TOLERANCE_MS = 50;
    // Extended tolerance for network filesystems (NFS, SMB, SSHFS)
    private static final long NETWORK_FS_TOLERANCE_MS = 2_000;

    @Inject AgentTelemetry telemetry;

    // (sessionId, filePath) → read timestamp
    private final ConcurrentHashMap<String, Instant> readTimestamps = new ConcurrentHashMap<>();
    // Per-file-path write locks (serializes concurrent writes to the same file)
    private final ConcurrentHashMap<String, ReentrantLock> writeLocks = new ConcurrentHashMap<>();
    // Detected network filesystem paths (use extended tolerance)
    private final Set<String> networkFsPaths = ConcurrentHashMap.newKeySet();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Records that a file was successfully read. Call immediately after every read_file.
     *
     * @param sessionId the session performing the read
     * @param filePath  the absolute or relative file path
     */
    public void recordRead(String sessionId, String filePath) {
        String key = makeKey(sessionId, filePath);
        readTimestamps.put(key, Instant.now());
        log.debug("[stale] recorded read: session={} file={}", sessionId, filePath);
        telemetry.count("stale.read.recorded");
    }

    /**
     * Asserts that a file has not been modified since the session last read it.
     * Must be called before every edit/write operation.
     *
     * @param sessionId the session attempting the edit
     * @param filePath  the file to be edited
     * @return a FreshnessResult indicating whether the edit is safe to proceed
     */
    public FreshnessResult assertFresh(String sessionId, String filePath) {
        String key = makeKey(sessionId, filePath);
        Instant readTime = readTimestamps.get(key);

        if (readTime == null) {
            telemetry.count("stale.never_read");
            return FreshnessResult.neverRead(filePath);
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            // File doesn't exist yet — always fresh (new file creation)
            return FreshnessResult.fresh(filePath, readTime, null);
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            Instant fileModTime = attrs.lastModifiedTime().toInstant();
            long toleranceMs = isNetworkFs(filePath) ? NETWORK_FS_TOLERANCE_MS : FRESHNESS_TOLERANCE_MS;

            boolean isStale = fileModTime.toEpochMilli() > readTime.toEpochMilli() + toleranceMs;

            if (isStale) {
                telemetry.count("stale.edit.rejected");
                log.warn("[stale] STALE: {} read at {} but modified at {} (diff={}ms)",
                        filePath, readTime, fileModTime,
                        fileModTime.toEpochMilli() - readTime.toEpochMilli());
                return FreshnessResult.stale(filePath, readTime, fileModTime);
            }

            telemetry.count("stale.edit.approved");
            return FreshnessResult.fresh(filePath, readTime, fileModTime);

        } catch (IOException e) {
            log.warn("[stale] cannot read attrs for {}: {}", filePath, e.getMessage());
            // Fail-safe: treat as stale if we can't verify
            return FreshnessResult.stale(filePath, readTime, null);
        }
    }

    /**
     * Acquires a write lock for the given file path.
     * Must be called before executing a write/edit operation; released after.
     * Serializes concurrent writes from parallel workflow steps.
     *
     * @param filePath  file to lock
     * @param timeoutMs max wait time in milliseconds
     * @return true if lock acquired
     */
    public boolean acquireWriteLock(String filePath, long timeoutMs) {
        ReentrantLock lock = writeLocks.computeIfAbsent(filePath, k -> new ReentrantLock());
        try {
            boolean acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("[stale] write lock acquired: {}", filePath);
                telemetry.count("stale.lock.acquired");
            } else {
                log.warn("[stale] write lock timeout: {}", filePath);
                telemetry.count("stale.lock.timeout");
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Releases the write lock for a file. */
    public void releaseWriteLock(String filePath) {
        ReentrantLock lock = writeLocks.get(filePath);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("[stale] write lock released: {}", filePath);
            telemetry.count("stale.lock.released");
        }
    }

    /**
     * Marks a path prefix as being on a network filesystem (uses extended tolerance).
     * Call during setup when NFS/SSHFS mounts are detected.
     */
    public void markNetworkFilesystem(String pathPrefix) {
        networkFsPaths.add(pathPrefix);
        log.info("[stale] marked as network fs: {}", pathPrefix);
    }

    /**
     * Invalidates the read timestamp for a file (call after an edit completes successfully
     * so the next edit correctly requires a fresh read).
     */
    public void invalidate(String sessionId, String filePath) {
        readTimestamps.remove(makeKey(sessionId, filePath));
        telemetry.count("stale.invalidated");
    }

    /** Clears all read timestamps for a session. */
    public void clearSession(String sessionId) {
        String prefix = sessionId + "::";
        readTimestamps.keySet().removeIf(k -> k.startsWith(prefix));
        log.debug("[stale] cleared session: {}", sessionId);
    }

    /** Returns all tracked read timestamps (for diagnostics). */
    public Map<String, Instant> snapshot() {
        return Collections.unmodifiableMap(readTimestamps);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String makeKey(String sessionId, String filePath) {
        return sessionId + "::" + Path.of(filePath).toAbsolutePath().normalize();
    }

    private boolean isNetworkFs(String filePath) {
        String abs = Path.of(filePath).toAbsolutePath().toString();
        return networkFsPaths.stream().anyMatch(abs::startsWith);
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record FreshnessResult(
            boolean fresh,
            String  filePath,
            Instant readTime,
            Instant fileModTime,
            String  reason
    ) {
        static FreshnessResult fresh(String path, Instant readTime, Instant modTime) {
            return new FreshnessResult(true, path, readTime, modTime, "file is fresh");
        }
        static FreshnessResult stale(String path, Instant readTime, Instant modTime) {
            String reason = modTime != null
                    ? String.format("file modified at %s, after read at %s — re-read before editing",
                            modTime, readTime)
                    : "file modification time could not be verified — re-read to be safe";
            return new FreshnessResult(false, path, readTime, modTime, reason);
        }
        static FreshnessResult neverRead(String path) {
            return new FreshnessResult(false, path, null, null,
                    "file was never read in this session — read it first before editing");
        }
        public boolean requiresReread() { return !fresh; }
        public String editRejectionMessage() {
            return "edit_file rejected: " + reason +
                   "\nUse read_file to get the current content, then retry your edit.";
        }
    }
}
