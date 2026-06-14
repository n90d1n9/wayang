package tech.kayys.wayang.gollek.sdk;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for agent run statuses and timeline events.
 */
public interface AgentRunStore {

    AgentRunStatus save(AgentRunStatus status);

    Optional<AgentRunStatus> find(String runId);

    List<AgentRunStatus> findAll();

    default boolean remove(String runId) {
        return false;
    }

    default AgentRunEvent appendEvent(AgentRunEvent event) {
        return event;
    }

    default List<AgentRunEvent> events(String runId) {
        AgentRunStatus status = status(runId);
        return status.known() ? List.of(AgentRunEvent.fromStatus(status, 1)) : List.of();
    }

    default AgentRunEvents timeline(String runId) {
        return timeline(runId, AgentRunEventsQuery.all());
    }

    default AgentRunEvents timeline(String runId, AgentRunEventsQuery query) {
        return AgentRunLifecycleService.create(this).events(runId, query);
    }

    default AgentRunStatus save(AgentRunResult result) {
        return AgentRunLifecycleService.create(this).record(result);
    }

    default AgentRunStatus status(String runId) {
        return AgentRunLifecycleService.create(this).status(runId);
    }

    default AgentRunForgetResult forget(String runId) {
        return AgentRunLifecycleService.create(this).forget(runId);
    }

    default AgentRunCancelResult cancel(String runId, String reason) {
        return AgentRunLifecycleService.create(this).cancel(runId, reason);
    }

    default AgentRunHistory history() {
        return history(AgentRunHistoryQuery.all());
    }

    default AgentRunHistory history(AgentRunHistoryQuery query) {
        return AgentRunLifecycleService.create(this).history(query);
    }

    default AgentRunStoreDiagnostics diagnostics() {
        List<AgentRunStatus> statuses = findAll();
        List<AgentRunEvent> events = statuses.stream()
                .flatMap(status -> events(status.handle().runId()).stream())
                .toList();
        AgentRunStoreSnapshot snapshot = new AgentRunStoreSnapshot(statuses, events);
        AgentRunStoreRetentionPolicy policy = AgentRunStoreRetentionPolicy.unlimited();
        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(snapshot, policy);
        return new AgentRunStoreDiagnostics(
                "custom",
                false,
                "",
                "",
                false,
                false,
                "",
                false,
                assessment.totalRuns(),
                statuses.size(),
                events.size(),
                policy,
                assessment,
                AgentRunStoreBackupRetentionPolicy.unlimited(),
                AgentRunStoreBackupInventory.empty());
    }

    default AgentRunStoreVerification verification() {
        return AgentRunStoreVerifications.fromDiagnostics(diagnostics());
    }

    default AgentRunStoreCompactionPreview compactionPreview() {
        return AgentRunStoreCompactionPreviews.fromVerification(verification(), true);
    }

    default AgentRunStoreCompactionResult compact() {
        return AgentRunStoreCompactionResults.skipped(verification());
    }

    static AgentRunStore memory() {
        return new InMemoryAgentRunStore();
    }

    static AgentRunStore file(String path) {
        return new FileAgentRunStore(Path.of(SdkText.trimToDefault(path, "wayang-runs.properties")));
    }

    static AgentRunStore file(String path, AgentRunStoreRetentionPolicy retentionPolicy) {
        return file(path, retentionPolicy, AgentRunStoreBackupRetentionPolicy.defaults());
    }

    static AgentRunStore file(
            String path,
            AgentRunStoreRetentionPolicy retentionPolicy,
            AgentRunStoreBackupRetentionPolicy backupRetentionPolicy) {
        return new FileAgentRunStore(
                Path.of(SdkText.trimToDefault(path, "wayang-runs.properties")),
                retentionPolicy,
                backupRetentionPolicy);
    }

    static AgentRunStore configured(WayangGollekSdkConfig config) {
        WayangGollekSdkConfig resolved = config == null ? WayangGollekSdkConfig.local() : config;
        WayangStorageConfig storage = resolved.storage();
        String filePath = storage.effectiveFilePath();
        if (!filePath.isBlank()) {
            return file(filePath, storage.retentionPolicy(), storage.backupRetentionPolicy());
        }
        return memory();
    }

}
