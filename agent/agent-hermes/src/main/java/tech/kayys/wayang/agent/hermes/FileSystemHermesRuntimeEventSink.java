package tech.kayys.wayang.agent.hermes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * Append-only JSONL file journal for Hermes runtime events.
 */
public final class FileSystemHermesRuntimeEventSink implements HermesRuntimeEventSink, HermesRuntimeEventReader {

    public static final int DEFAULT_MAX_EVENTS = HermesRecordRetentionPolicy.DEFAULT_MAX_ENTRIES;

    private final Path journalPath;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public FileSystemHermesRuntimeEventSink(Path journalPath) {
        this(journalPath, DEFAULT_MAX_EVENTS);
    }

    public FileSystemHermesRuntimeEventSink(Path journalPath, int maxEvents) {
        this.journalPath = Objects.requireNonNull(journalPath, "journalPath");
        this.retentionPolicy = HermesRecordRetentionPolicy.bounded(maxEvents);
    }

    public Path journalPath() {
        return journalPath;
    }

    public int maxEvents() {
        return retentionPolicy.maxEntries();
    }

    @Override
    public synchronized void emit(HermesRuntimeEvent event) {
        if (event == null) {
            return;
        }
        try {
            Path parent = journalPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    journalPath,
                    HermesRuntimeEventJsonCodec.toJsonLine(event) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            pruneToCapacity();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to persist Hermes runtime event", error);
        }
    }

    @Override
    public synchronized HermesRuntimeEventPage query(HermesRuntimeEventQuery query) {
        return HermesRuntimeEventPages.from(events(), query);
    }

    public synchronized List<HermesRuntimeEvent> events() {
        if (!Files.exists(journalPath)) {
            return List.of();
        }
        try {
            return Files.readAllLines(journalPath, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(HermesRuntimeEventJsonCodec::fromJsonLine)
                    .toList();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read Hermes runtime event journal", error);
        }
    }

    private void pruneToCapacity() throws IOException {
        List<String> lines = Files.readAllLines(journalPath, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (retentionPolicy.allowsAll(lines.size())) {
            return;
        }
        Files.write(
                journalPath,
                retentionPolicy.retainNewestFromAppendOrder(lines),
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}
