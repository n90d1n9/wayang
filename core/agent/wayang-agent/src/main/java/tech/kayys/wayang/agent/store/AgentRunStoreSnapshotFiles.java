package tech.kayys.wayang.agent.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Filesystem adapter for run-store properties snapshots, including atomic
 * replacement, pre-mutation backups, and quarantine of unreadable snapshots.
 */
final class AgentRunStoreSnapshotFiles {

    private static final ConcurrentMap<Path, Object> LOCAL_LOCKS = new ConcurrentHashMap<>();

    private final Path path;
    private final Path lockPath;

    AgentRunStoreSnapshotFiles(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Run store path is required.");
        }
        this.path = path.toAbsolutePath().normalize();
        this.lockPath = this.path.resolveSibling(this.path.getFileName().toString() + ".lock");
    }

    Path path() {
        return path;
    }

    Path lockPath() {
        return lockPath;
    }

    boolean exists() {
        return Files.exists(path);
    }

    boolean lockExists() {
        return Files.exists(lockPath);
    }

    <T> T withExclusiveLock(Supplier<T> operation) {
        Object localLock = LOCAL_LOCKS.computeIfAbsent(lockPath, ignored -> new Object());
        synchronized (localLock) {
            return withFileLock(operation);
        }
    }

    Optional<Properties> read() {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return Optional.of(properties);
        } catch (IOException | IllegalArgumentException e) {
            quarantine("corrupt", e);
            return Optional.empty();
        }
    }

    AgentRunStoreSnapshotFileInspection inspect() {
        if (!Files.exists(path)) {
            return AgentRunStoreSnapshotFileInspection.missing();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return AgentRunStoreSnapshotFileInspection.readable(properties);
        } catch (IOException | IllegalArgumentException e) {
            return AgentRunStoreSnapshotFileInspection.unreadable(e);
        }
    }

    void write(Properties properties) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(
                    parent == null ? Path.of(".") : parent,
                    path.getFileName().toString(),
                    ".tmp");
            boolean moved = false;
            try {
                try (OutputStream output = Files.newOutputStream(
                        temp,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    properties.store(output, "Wayang run store");
                }
                forceFile(temp);
                moveIntoPlace(temp);
                forceDirectory(parent);
                moved = true;
            } finally {
                if (!moved) {
                    Files.deleteIfExists(temp);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write Wayang run store at " + path, e);
        }
    }

    Optional<Path> backup(String reason) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path backup = Files.createTempFile(
                    parent == null ? Path.of(".") : parent,
                    path.getFileName().toString() + "." + snapshotReason(reason) + "-",
                    ".properties");
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            forceFile(backup);
            forceDirectory(parent);
            return Optional.of(backup);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to back up Wayang run store at " + path, e);
        }
    }

    AgentRunStoreBackupRetentionResult pruneBackups(
            String reason,
            AgentRunStoreBackupRetentionPolicy policy) {
        AgentRunStoreBackupRetentionPolicy resolvedPolicy = policy == null
                ? AgentRunStoreBackupRetentionPolicy.defaults()
                : policy;
        List<Path> backups = backupFiles(reason);
        AgentRunStoreBackupRetentionResult preview = AgentRunStoreBackupRetentionResult.preview(
                resolvedPolicy,
                new AgentRunStoreBackupInventory(paths(backups)));
        List<Path> deleted = preview.prunedBackupPaths().stream()
                .map(Path::of)
                .filter(AgentRunStoreSnapshotFiles::deleteIfPresent)
                .toList();
        List<String> pruned = paths(deleted);
        List<String> failed = preview.prunedBackupPaths().stream()
                .filter(path -> !pruned.contains(path))
                .toList();
        return new AgentRunStoreBackupRetentionResult(
                resolvedPolicy,
                preview.retainedBackupCount(),
                pruned.size(),
                failed.size(),
                preview.retainedBackupPaths(),
                pruned,
                failed);
    }

    AgentRunStoreBackupInventory backupInventory(String reason) {
        return new AgentRunStoreBackupInventory(paths(backupFiles(reason)));
    }

    void quarantine(String reason) {
        quarantine(reason, null);
    }

    private <T> T withFileLock(Supplier<T> operation) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (FileChannel channel = FileChannel.open(
                    lockPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
                    FileLock ignored = channel.lock()) {
                return operation.get();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to lock Wayang run store at " + path, e);
        }
    }

    private void quarantine(String reason, Exception cause) {
        Path quarantine = null;
        boolean moved = false;
        try {
            Path parent = path.getParent();
            quarantine = Files.createTempFile(
                    parent == null ? Path.of(".") : parent,
                    path.getFileName().toString() + "." + snapshotReason(reason) + "-",
                    ".properties");
            Files.move(path, quarantine, StandardCopyOption.REPLACE_EXISTING);
            moved = true;
        } catch (IOException e) {
            IllegalStateException failure = cause == null
                    ? new IllegalStateException("Unable to quarantine Wayang run store at " + path)
                    : new IllegalStateException("Unable to read Wayang run store at " + path, cause);
            failure.addSuppressed(e);
            throw failure;
        } finally {
            if (!moved && quarantine != null) {
                deleteIfPresent(quarantine);
            }
        }
    }

    private static String snapshotReason(String reason) {
        String normalized = SdkText.trimToDefault(reason, "quarantine");
        return normalized.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private List<Path> backupFiles(String reason) {
        Path parent = path.getParent() == null ? Path.of(".") : path.getParent();
        if (!Files.isDirectory(parent)) {
            return List.of();
        }
        String prefix = path.getFileName().toString() + "." + snapshotReason(reason) + "-";
        try (Stream<Path> files = Files.list(parent)) {
            return files
                    .filter(file -> file.getFileName().toString().startsWith(prefix))
                    .filter(file -> file.getFileName().toString().endsWith(".properties"))
                    .sorted(Comparator
                            .comparingLong(AgentRunStoreSnapshotFiles::lastModifiedMillis)
                            .reversed()
                            .thenComparing(file -> file.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<String> paths(List<Path> paths) {
        return paths.stream()
                .map(Path::toString)
                .toList();
    }

    private static long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void forceFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void forceDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException e) {
            // Directory fsync is a best-effort durability hint.
        }
    }

    private static boolean deleteIfPresent(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            // Best-effort cleanup must not hide the original store operation result.
            return false;
        }
    }
}
