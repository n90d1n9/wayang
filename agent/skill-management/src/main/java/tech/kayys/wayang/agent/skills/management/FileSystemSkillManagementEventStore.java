package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * File-backed event sink/reader for restart-safe local diagnostics.
 */
public final class FileSystemSkillManagementEventStore
        implements SkillManagementEventSink, SkillManagementEventReader, SkillManagementEventPruner {

    private final Path directory;
    private final int maxEvents;

    public FileSystemSkillManagementEventStore(Path directory) {
        this(directory, InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public FileSystemSkillManagementEventStore(Path directory, int maxEvents) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.maxEvents = SkillManagementEventRetention.normalizeCapacity(maxEvents);
    }

    @Override
    public synchronized void record(SkillManagementEvent event) {
        if (event == null) {
            return;
        }
        SkillManagementFileStoreSupport.ensureDirectory(
                directory,
                "Failed to create skill-management event directory");
        SkillManagementFileStoreSupport.writeUtf8Properties(
                pathFor(event),
                SkillManagementEventPropertiesCodec.toProperties(event),
                "Wayang skill-management event",
                "Failed to persist skill-management event");
        pruneToCapacity();
    }

    @Override
    public synchronized SkillManagementEventPage query(SkillManagementEventQuery query) {
        return SkillManagementEventPages.from(events(), query);
    }

    public synchronized List<SkillManagementEvent> events() {
        return eventFiles().stream()
                .map(this::read)
                .toList();
    }

    @Override
    public synchronized SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
        SkillManagementEventPruneOptions resolved =
                SkillManagementEventRetention.resolve(options, maxEvents);
        List<Path> files = eventFiles();
        List<Path> targets = SkillManagementEventRetention.oldestToPrune(files, resolved.keepLatestEvents());
        List<String> references = targets.stream()
                .map(path -> path.getFileName().toString())
                .toList();
        if (!resolved.dryRun()) {
            targets.forEach(this::delete);
        }
        return SkillManagementEventPruneResult.success(resolved, files.size(), references);
    }

    private void pruneToCapacity() {
        SkillManagementEventRetention.oldestToPrune(eventFiles(), maxEvents)
                .forEach(this::delete);
    }

    private List<Path> eventFiles() {
        return SkillManagementFileStoreSupport.regularFiles(
                directory,
                path -> SkillManagementEventReferences.hasStorageExtension(path.getFileName().toString()),
                "skill-management event files");
    }

    private SkillManagementEvent read(Path path) {
        return SkillManagementEventPropertiesCodec.fromProperties(
                SkillManagementFileStoreSupport.readUtf8Properties(
                        path,
                        "Failed to read skill-management event file"),
                path.toString());
    }

    private void delete(Path path) {
        SkillManagementFileStoreSupport.deleteIfExists(
                path,
                "Failed to prune skill-management event file");
    }

    private Path pathFor(SkillManagementEvent event) {
        return directory.resolve(SkillManagementEventReferences.storageReference(event));
    }
}
