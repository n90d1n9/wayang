package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Volatile run store for tests and local sessions that do not need durable
 * lifecycle history.
 */
public final class InMemoryAgentRunStore implements AgentRunStore {

    private final Map<String, AgentRunStatus> statuses = new LinkedHashMap<>();
    private final Map<String, List<AgentRunEvent>> events = new LinkedHashMap<>();

    @Override
    public synchronized AgentRunStatus save(AgentRunStatus status) {
        AgentRunStatus normalized = status == null
                ? AgentRunStatus.unknown("", "Cannot record a null run status.")
                : status;
        statuses.put(normalized.handle().runId(), normalized);
        long sequence = AgentRunEventSequences.nextForRun(
                events(normalized.handle().runId()),
                normalized.handle().runId());
        appendEvent(AgentRunEvent.fromStatus(normalized, sequence));
        return normalized;
    }

    @Override
    public synchronized Optional<AgentRunStatus> find(String runId) {
        return Optional.ofNullable(statuses.get(SdkText.trimToEmpty(runId)));
    }

    @Override
    public synchronized List<AgentRunStatus> findAll() {
        return List.copyOf(statuses.values());
    }

    @Override
    public synchronized boolean remove(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        events.remove(normalizedRunId);
        return statuses.remove(normalizedRunId) != null;
    }

    @Override
    public synchronized AgentRunEvent appendEvent(AgentRunEvent event) {
        AgentRunEvent normalized = event == null
                ? new AgentRunEvent("", 1, "", AgentRunState.UNKNOWN, "", Map.of())
                : event;
        List<AgentRunEvent> current = new ArrayList<>(events(normalized.runId()));
        current.add(normalized);
        events.put(normalized.runId(), List.copyOf(current));
        return normalized;
    }

    @Override
    public synchronized List<AgentRunEvent> events(String runId) {
        return List.copyOf(events.getOrDefault(SdkText.trimToEmpty(runId), List.of()));
    }

    @Override
    public synchronized AgentRunStoreDiagnostics diagnostics() {
        List<AgentRunStatus> statusSnapshot = List.copyOf(statuses.values());
        List<AgentRunEvent> eventSnapshot = events.values().stream()
                .flatMap(List::stream)
                .toList();
        AgentRunStoreSnapshot snapshot = new AgentRunStoreSnapshot(statusSnapshot, eventSnapshot);
        AgentRunStoreRetentionPolicy policy = AgentRunStoreRetentionPolicy.unlimited();
        AgentRunStoreRetentionAssessment assessment = AgentRunStoreRetention.assess(snapshot, policy);
        return new AgentRunStoreDiagnostics(
                "memory",
                false,
                "",
                "",
                false,
                false,
                "",
                false,
                assessment.totalRuns(),
                statusSnapshot.size(),
                eventSnapshot.size(),
                policy,
                assessment,
                AgentRunStoreBackupRetentionPolicy.unlimited(),
                AgentRunStoreBackupInventory.empty());
    }
}
