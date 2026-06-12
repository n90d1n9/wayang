package tech.kayys.wayang.agenticcommerce.wayang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Append-only JSONL file store for persistence transfer audit trails.
 */
public final class FileSystemAgenticCommerceWayangPersistenceTransferAuditStore
        implements AgenticCommerceWayangPersistenceTransferAuditSink,
                AgenticCommerceWayangPersistenceTransferAuditReader {

    public static final String JOURNAL_FORMAT =
            AgenticCommerceWayangPersistenceTransferAuditJsonl.JOURNAL_FORMAT;

    private final Path journalPath;
    private final AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy;
    private final InMemoryAgenticCommerceWayangPersistenceTransferAuditSink mirror;

    public FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(Path journalPath) {
        this(journalPath, InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.DEFAULT_MAX_TRAILS);
    }

    public FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(Path journalPath, int maxTrails) {
        this(journalPath, AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.ofMaxTrails(maxTrails));
    }

    public FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(
            Path journalPath,
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy) {
        this.journalPath = Objects.requireNonNull(journalPath, "journalPath");
        this.retentionPolicy = retentionPolicy == null
                ? AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.defaults()
                : retentionPolicy;
        this.mirror = new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(this.retentionPolicy.maxTrails());
        reload();
    }

    public Path journalPath() {
        return journalPath;
    }

    public int maxTrails() {
        return retentionPolicy.maxTrails();
    }

    public AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy() {
        return retentionPolicy;
    }

    @Override
    public synchronized void record(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        if (trail == null) {
            return;
        }
        append(trail);
        reloadMirror(journalLines());
    }

    @Override
    public synchronized AgenticCommerceWayangPersistenceTransferAuditPage query(
            AgenticCommerceWayangPersistenceTransferAuditQuery query) {
        return mirror.query(query);
    }

    public synchronized List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails() {
        return mirror.trails();
    }

    public synchronized Optional<AgenticCommerceWayangPersistenceTransferAuditTrail> latest() {
        return mirror.latest();
    }

    public synchronized void reload() {
        try {
            pruneToCapacity();
            reloadMirror(journalLines());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to prune Agentic Commerce persistence transfer audit journal",
                    exception);
        }
    }

    private void append(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        try {
            Path parent = journalPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    journalPath,
                    AgenticCommerceWayangPersistenceTransferAuditJsonl.toJsonLine(trail) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            pruneToCapacity();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to persist Agentic Commerce persistence transfer audit trail",
                    exception);
        }
    }

    private List<String> journalLines() {
        if (!Files.exists(journalPath)) {
            return List.of();
        }
        try {
            return Files.readAllLines(journalPath, StandardCharsets.UTF_8).stream()
                    .flatMap(line -> AgenticCommerceWayangPersistenceTransferAuditJsonl.linesFromBody(line).stream())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read Agentic Commerce persistence transfer audit journal",
                    exception);
        }
    }

    private void pruneToCapacity() throws IOException {
        if (!Files.exists(journalPath)) {
            return;
        }
        List<String> lines = Files.readAllLines(journalPath, StandardCharsets.UTF_8);
        List<String> retained = AgenticCommerceWayangPersistenceTransferAuditJsonl.retainedLines(
                lines,
                retentionPolicy);
        if (lines.equals(retained)) {
            return;
        }
        Files.write(
                journalPath,
                retained,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private void reloadMirror(List<String> lines) {
        mirror.clear();
        for (String line : lines) {
            parseJournalLine(line).ifPresent(mirror::record);
        }
    }

    private static Optional<AgenticCommerceWayangPersistenceTransferAuditTrail> parseJournalLine(String line) {
        return AgenticCommerceWayangPersistenceTransferAuditJsonl.fromJsonLine(line);
    }
}
