package tech.kayys.wayang.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.event.AgentRunEventSequences;
import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.agent.store.AgentRunStore;
import tech.kayys.wayang.agent.store.AgentRunStoreBackupRetentionPolicy;
import tech.kayys.wayang.agent.store.AgentRunStoreBackupRetentionResult;
import tech.kayys.wayang.agent.store.AgentRunStoreCompactionResult;
import tech.kayys.wayang.agent.store.AgentRunStoreCompactionResults;
import tech.kayys.wayang.agent.store.AgentRunStoreDiagnostics;
import tech.kayys.wayang.agent.store.AgentRunStoreRetention;
import tech.kayys.wayang.agent.store.AgentRunStoreRetentionAssessment;
import tech.kayys.wayang.agent.store.AgentRunStoreRetentionPolicy;
import tech.kayys.wayang.agent.store.AgentRunStoreSnapshot;
import tech.kayys.wayang.agent.store.AgentRunStoreSnapshotFileInspection;
import tech.kayys.wayang.agent.store.AgentRunStoreSnapshotFiles;
import tech.kayys.wayang.agent.store.AgentRunStoreVerification;
import tech.kayys.wayang.agent.store.AgentRunStoreVerificationIssue;
import tech.kayys.wayang.agent.store.AgentRunStoreVerifications;
import tech.kayys.wayang.client.SdkText;

/**
 * File-backed run store that persists agent run statuses and timeline events as
 * compact properties snapshots with atomic replacement when the filesystem
 * supports it.
 */
public final class FileAgentRunStore implements AgentRunStore {

    private static final String STORE_VERSION = "1";

    private final AgentRunStoreSnapshotFiles snapshots;
    private final AgentRunStoreRetentionPolicy retentionPolicy;
    private final AgentRunStoreBackupRetentionPolicy backupRetentionPolicy;

    public FileAgentRunStore(Path path) {
        this(path, AgentRunStoreRetentionPolicy.defaults(), AgentRunStoreBackupRetentionPolicy.defaults());
    }

    public FileAgentRunStore(Path path, AgentRunStoreRetentionPolicy retentionPolicy) {
        this(path, retentionPolicy, AgentRunStoreBackupRetentionPolicy.defaults());
    }

    public FileAgentRunStore(
            Path path,
            AgentRunStoreRetentionPolicy retentionPolicy,
            AgentRunStoreBackupRetentionPolicy backupRetentionPolicy) {
        this.snapshots = new AgentRunStoreSnapshotFiles(path);
        this.retentionPolicy = retentionPolicy == null
                ? AgentRunStoreRetentionPolicy.defaults()
                : retentionPolicy;
        this.backupRetentionPolicy = backupRetentionPolicy == null
                ? AgentRunStoreBackupRetentionPolicy.defaults()
                : backupRetentionPolicy;
    }

    public Path path() {
        return snapshots.path();
    }

    @Override
    public synchronized AgentRunStoreDiagnostics diagnostics() {
        return snapshots.withExclusiveLock(this::diagnosticsLocked);
    }

    @Override
    public synchronized AgentRunStoreVerification verification() {
        return snapshots.withExclusiveLock(this::verificationLocked);
    }

    @Override
    public synchronized AgentRunStoreCompactionResult compact() {
        return snapshots.withExclusiveLock(this::compactLocked);
    }

    private AgentRunStoreDiagnostics diagnosticsLocked() {
        boolean snapshotPresent = snapshots.exists();
        boolean lockPresent = snapshots.lockExists();
        Optional<Properties> properties = snapshots.read();
        String version = properties
                .map(value -> SdkText.trimToEmpty(value.getProperty("version")))
                .orElse("");
        boolean unsupportedVersion = properties.isPresent() && unsupportedVersion(properties.get());
        if (unsupportedVersion) {
            snapshots.quarantine("unsupported-version");
            properties = Optional.empty();
        }
        AgentRunStoreSnapshot snapshot = properties
                .map(this::toRunStoreSnapshot)
                .orElseGet(AgentRunStoreSnapshot::empty);
        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(snapshot, retentionPolicy);
        return new AgentRunStoreDiagnostics(
                "file",
                true,
                snapshots.path().toString(),
                snapshots.lockPath().toString(),
                snapshotPresent,
                lockPresent,
                version,
                unsupportedVersion,
                assessment.totalRuns(),
                snapshot.statuses().size(),
                snapshot.events().size(),
                retentionPolicy,
                assessment,
                backupRetentionPolicy,
                snapshots.backupInventory("compaction"));
    }

    private AgentRunStoreVerification verificationLocked() {
        AgentRunStoreSnapshotFileInspection inspection = snapshots.inspect();
        List<AgentRunStoreVerificationIssue> issues = new ArrayList<>();
        if (!inspection.readable()) {
            AgentRunStoreDiagnostics diagnostics = emptyDiagnostics(
                    inspection.exists(),
                    snapshots.lockExists(),
                    "",
                    false);
            issues.add(AgentRunStoreVerificationIssue.error(
                    "snapshot.unreadable",
                    "Run-store snapshot could not be read: " + inspection.errorMessage()));
            return new AgentRunStoreVerification(diagnostics, issues);
        }
        Properties properties = inspection.properties();
        String version = SdkText.trimToEmpty(properties.getProperty("version"));
        boolean unsupportedVersion = inspection.exists() && unsupportedVersion(properties);
        if (unsupportedVersion) {
            AgentRunStoreDiagnostics diagnostics = emptyDiagnostics(
                    inspection.exists(),
                    snapshots.lockExists(),
                    version,
                    true);
            issues.add(AgentRunStoreVerificationIssue.error(
                    "snapshot.unsupported-version",
                    "Run-store snapshot version is not supported by this SDK."));
            return new AgentRunStoreVerification(diagnostics, issues);
        }
        AgentRunStoreSnapshot snapshot = inspection.exists()
                ? toRunStoreSnapshot(properties)
                : AgentRunStoreSnapshot.empty();
        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(snapshot, retentionPolicy);
        AgentRunStoreDiagnostics diagnostics = new AgentRunStoreDiagnostics(
                "file",
                true,
                snapshots.path().toString(),
                snapshots.lockPath().toString(),
                inspection.exists(),
                snapshots.lockExists(),
                version,
                false,
                assessment.totalRuns(),
                snapshot.statuses().size(),
                snapshot.events().size(),
                retentionPolicy,
                assessment,
                backupRetentionPolicy,
                snapshots.backupInventory("compaction"));
        addCountMismatchIssue(issues, properties, "count", snapshot.statuses().size(), "status");
        addCountMismatchIssue(issues, properties, "event.count", snapshot.events().size(), "event");
        AgentRunStoreVerifications.addRetentionWarning(diagnostics, issues);
        AgentRunStoreVerifications.addBackupRetentionWarning(diagnostics, issues);
        return new AgentRunStoreVerification(diagnostics, issues);
    }

    private AgentRunStoreCompactionResult compactLocked() {
        AgentRunStoreVerification before = verificationLocked();
        if (before.errorCount() > 0 || !before.diagnostics().retentionAssessment().pruned()) {
            return AgentRunStoreCompactionResults.skipped(before, backupRetentionPolicy);
        }
        AgentRunStoreSnapshot snapshot = readStoreSnapshot();
        AgentRunStoreSnapshot retained = AgentRunStoreRetention.apply(snapshot, retentionPolicy);
        Optional<Path> backupPath;
        try {
            backupPath = snapshots.backup("compaction");
        } catch (RuntimeException e) {
            return AgentRunStoreCompactionResults.failed(
                    before,
                    backupRetentionPolicy,
                    AgentRunStoreVerificationIssue.error(
                            "snapshot.backup-failed",
                            "Run-store snapshot backup could not be created: "
                                    + SdkText.trimToDefault(e.getMessage(), "unknown failure")));
        }
        write(retained.statuses(), retained.events());
        AgentRunStoreBackupRetentionResult backupRetention = snapshots.pruneBackups(
                "compaction",
                backupRetentionPolicy);
        return AgentRunStoreCompactionResults.compacted(
                before,
                diagnosticsLocked(),
                backupPath.map(Path::toString).orElse(""),
                backupRetention);
    }

    private AgentRunStoreDiagnostics emptyDiagnostics(
            boolean snapshotPresent,
            boolean lockPresent,
            String snapshotVersion,
            boolean unsupportedSnapshotVersion) {
        AgentRunStoreSnapshot snapshot = AgentRunStoreSnapshot.empty();
        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(snapshot, retentionPolicy);
        return new AgentRunStoreDiagnostics(
                "file",
                true,
                snapshots.path().toString(),
                snapshots.lockPath().toString(),
                snapshotPresent,
                lockPresent,
                snapshotVersion,
                unsupportedSnapshotVersion,
                0,
                0,
                0,
                retentionPolicy,
                assessment,
                backupRetentionPolicy,
                snapshots.backupInventory("compaction"));
    }

    private void addCountMismatchIssue(
            List<AgentRunStoreVerificationIssue> issues,
            Properties properties,
            String key,
            int decodedCount,
            String label) {
        if (!properties.containsKey(key)) {
            return;
        }
        int declaredCount = intProperty(properties, key, decodedCount);
        if (declaredCount != decodedCount) {
            issues.add(AgentRunStoreVerificationIssue.error(
                    "snapshot." + label + "-count-mismatch",
                    "Run-store declares " + declaredCount + " " + label
                            + " entries but decoded " + decodedCount + "."));
        }
    }

    @Override
    public synchronized AgentRunStatus save(AgentRunStatus status) {
        return snapshots.withExclusiveLock(() -> saveLocked(status));
    }

    private AgentRunStatus saveLocked(AgentRunStatus status) {
        AgentRunStatus normalized = status == null
                ? AgentRunStatus.unknown("", "Cannot record a null run status.")
                : status;
        AgentRunStoreSnapshot snapshot = readStoreSnapshot();
        List<AgentRunStatus> statuses = snapshot.mutableStatuses();
        List<AgentRunEvent> events = snapshot.mutableEvents();
        int index = indexOf(statuses, normalized.handle().runId());
        if (index >= 0) {
            statuses.set(index, normalized);
        } else {
            statuses.add(normalized);
        }
        long sequence = AgentRunEventSequences.nextForRun(events, normalized.handle().runId());
        events.add(AgentRunEvent.fromStatus(normalized, sequence));
        write(statuses, events);
        return normalized;
    }

    @Override
    public synchronized Optional<AgentRunStatus> find(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        return snapshots.withExclusiveLock(() -> readStoreSnapshot().statuses().stream()
                .filter(status -> status.handle().runId().equals(normalizedRunId))
                .findFirst());
    }

    @Override
    public synchronized List<AgentRunStatus> findAll() {
        return snapshots.withExclusiveLock(() -> readStoreSnapshot().statuses());
    }

    @Override
    public synchronized boolean remove(String runId) {
        return snapshots.withExclusiveLock(() -> removeLocked(runId));
    }

    private boolean removeLocked(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        AgentRunStoreSnapshot snapshot = readStoreSnapshot();
        List<AgentRunStatus> statuses = snapshot.mutableStatuses();
        List<AgentRunEvent> events = snapshot.mutableEvents();
        int index = indexOf(statuses, normalizedRunId);
        if (index < 0) {
            return false;
        }
        statuses.remove(index);
        events.removeIf(event -> event.runId().equals(normalizedRunId));
        write(statuses, events);
        return true;
    }

    @Override
    public synchronized AgentRunEvent appendEvent(AgentRunEvent event) {
        return snapshots.withExclusiveLock(() -> appendEventLocked(event));
    }

    private AgentRunEvent appendEventLocked(AgentRunEvent event) {
        AgentRunEvent normalized = event == null
                ? new AgentRunEvent("", 1, "", AgentRunState.UNKNOWN, "", Map.of())
                : event;
        AgentRunStoreSnapshot snapshot = readStoreSnapshot();
        List<AgentRunEvent> events = snapshot.mutableEvents();
        events.add(normalized);
        write(snapshot.statuses(), events);
        return normalized;
    }

    @Override
    public synchronized List<AgentRunEvent> events(String runId) {
        return snapshots.withExclusiveLock(() -> List.copyOf(eventsFor(events(), SdkText.trimToEmpty(runId))));
    }

    private List<AgentRunEvent> events() {
        return readStoreSnapshot().events();
    }

    private AgentRunStoreSnapshot readStoreSnapshot() {
        return readSnapshotProperties()
                .map(this::toRunStoreSnapshot)
                .orElseGet(AgentRunStoreSnapshot::empty);
    }

    private Optional<Properties> readSnapshotProperties() {
        Optional<Properties> properties = snapshots.read();
        if (properties.isEmpty()) {
            return Optional.empty();
        }
        if (unsupportedVersion(properties.get())) {
            snapshots.quarantine("unsupported-version");
            return Optional.empty();
        }
        return properties;
    }

    private AgentRunStoreSnapshot toRunStoreSnapshot(Properties properties) {
        return new AgentRunStoreSnapshot(readStatuses(properties), readEvents(properties));
    }

    private List<AgentRunStatus> readStatuses(Properties properties) {
        int count = intProperty(properties, "count", 0);
        List<AgentRunStatus> statuses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            readStatus(properties, i).ifPresent(statuses::add);
        }
        return statuses;
    }

    private List<AgentRunEvent> readEvents(Properties properties) {
        int count = intProperty(properties, "event.count", 0);
        List<AgentRunEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            readEvent(properties, i).ifPresent(events::add);
        }
        return events;
    }

    private boolean unsupportedVersion(Properties properties) {
        String version = SdkText.trimToEmpty(properties.getProperty("version"));
        return !version.isEmpty() && !STORE_VERSION.equals(version);
    }

    private void write(List<AgentRunStatus> statuses, List<AgentRunEvent> events) {
        AgentRunStoreSnapshot retained = AgentRunStoreRetention.apply(
                new AgentRunStoreSnapshot(statuses, events),
                retentionPolicy);
        Properties properties = new Properties();
        properties.setProperty("version", STORE_VERSION);
        properties.setProperty("count", Integer.toString(retained.statuses().size()));
        for (int i = 0; i < retained.statuses().size(); i++) {
            writeStatus(properties, i, retained.statuses().get(i));
        }
        properties.setProperty("event.count", Integer.toString(retained.events().size()));
        for (int i = 0; i < retained.events().size(); i++) {
            writeEvent(properties, i, retained.events().get(i));
        }
        snapshots.write(properties);
    }

    private void writeStatus(Properties properties, int index, AgentRunStatus status) {
        String prefix = "run." + index + ".";
        properties.setProperty(prefix + "runId", status.handle().runId());
        properties.setProperty(prefix + "state", status.handle().state().name());
        properties.setProperty(prefix + "strategy", status.handle().strategy());
        properties.setProperty(prefix + "known", Boolean.toString(status.known()));
        properties.setProperty(prefix + "message", status.message());
        writeMetadata(properties, prefix, status.metadata());
    }

    private void writeEvent(Properties properties, int index, AgentRunEvent event) {
        String prefix = "event." + index + ".";
        properties.setProperty(prefix + "runId", event.runId());
        properties.setProperty(prefix + "sequence", Long.toString(event.sequence()));
        properties.setProperty(prefix + "type", event.type());
        properties.setProperty(prefix + "state", event.state().name());
        properties.setProperty(prefix + "message", event.message());
        writeMetadata(properties, prefix, event.metadata());
    }

    private void writeMetadata(Properties properties, String prefix, Map<String, Object> metadata) {
        properties.setProperty(prefix + "metadata.count", Integer.toString(metadata.size()));
        int metadataIndex = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            properties.setProperty(prefix + "metadata." + metadataIndex + ".key", entry.getKey());
            properties.setProperty(prefix + "metadata." + metadataIndex + ".value", String.valueOf(entry.getValue()));
            metadataIndex++;
        }
    }

    private Optional<AgentRunStatus> readStatus(Properties properties, int index) {
        String prefix = "run." + index + ".";
        String runId = properties.getProperty(prefix + "runId", "");
        if (SdkText.trimToEmpty(runId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AgentRunStatus(
                new AgentRunHandle(
                        runId,
                        state(properties.getProperty(prefix + "state")),
                        properties.getProperty(prefix + "strategy")),
                Boolean.parseBoolean(properties.getProperty(prefix + "known", "true")),
                properties.getProperty(prefix + "message", ""),
                metadata(properties, prefix)));
    }

    private Optional<AgentRunEvent> readEvent(Properties properties, int index) {
        String prefix = "event." + index + ".";
        String runId = properties.getProperty(prefix + "runId", "");
        if (SdkText.trimToEmpty(runId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AgentRunEvent(
                runId,
                longProperty(properties, prefix + "sequence", index + 1L),
                properties.getProperty(prefix + "type", ""),
                state(properties.getProperty(prefix + "state")),
                properties.getProperty(prefix + "message", ""),
                metadata(properties, prefix)));
    }

    private Map<String, Object> metadata(Properties properties, String prefix) {
        int count = intProperty(properties, prefix + "metadata.count", 0);
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = properties.getProperty(prefix + "metadata." + i + ".key", "");
            String value = properties.getProperty(prefix + "metadata." + i + ".value", "");
            if (!SdkText.trimToEmpty(key).isEmpty()) {
                metadata.put(key, value);
            }
        }
        return metadata;
    }

    private AgentRunState state(String value) {
        try {
            return AgentRunState.valueOf(SdkText.trimToDefault(value, "UNKNOWN"));
        } catch (IllegalArgumentException e) {
            return AgentRunState.UNKNOWN;
        }
    }

    private int intProperty(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long longProperty(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int indexOf(List<AgentRunStatus> statuses, String runId) {
        for (int i = 0; i < statuses.size(); i++) {
            if (statuses.get(i).handle().runId().equals(runId)) {
                return i;
            }
        }
        return -1;
    }

    private List<AgentRunEvent> eventsFor(List<AgentRunEvent> events, String runId) {
        return events.stream()
                .filter(event -> event.runId().equals(runId))
                .toList();
    }
}
