package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe in-memory task store for tests, harnesses, and local deployments.
 */
public final class InMemoryWayangA2aTaskStore implements WayangA2aTaskStore {

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private long nextSequence = 1;

    @Override
    public synchronized A2aTask create(A2aTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        if (entries.containsKey(task.id())) {
            throw new IllegalArgumentException("A2A task already exists: " + task.id());
        }
        Entry entry = new Entry(task);
        entry.events.add(WayangA2aTaskEvent.taskCreated(nextSequence(), task));
        entries.put(task.id(), entry);
        return task;
    }

    @Override
    public synchronized Optional<A2aTask> get(String taskId) {
        Entry entry = entries.get(WayangA2aMaps.required(taskId, "taskId"));
        return Optional.ofNullable(entry == null ? null : entry.task);
    }

    @Override
    public synchronized List<A2aTask> list(WayangA2aTaskQuery query) {
        WayangA2aTaskQuery resolved = query == null ? WayangA2aTaskQuery.all() : query;
        return entries.values().stream()
                .map(entry -> entry.task)
                .filter(resolved::matches)
                .limit(resolved.limit())
                .toList();
    }

    @Override
    public synchronized A2aTask updateStatus(String taskId, A2aTaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        Entry entry = entry(taskId);
        WayangA2aTaskLifecycle.requireTransition(entry.task, status.state());
        entry.task = WayangA2aTaskSnapshots.withStatus(entry.task, status);
        entry.events.add(WayangA2aTaskEvent.status(nextSequence(), entry.task, status, false));
        return entry.task;
    }

    @Override
    public synchronized A2aTask appendMessage(String taskId, A2aMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        Entry entry = entry(taskId);
        WayangA2aTaskLifecycle.requireMutable(entry.task, "append messages");
        entry.task = WayangA2aTaskSnapshots.withAppendedMessage(entry.task, message);
        entry.events.add(WayangA2aTaskEvent.message(nextSequence(), entry.task, message));
        return entry.task;
    }

    @Override
    public synchronized A2aTask appendArtifact(String taskId, A2aArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        Entry entry = entry(taskId);
        WayangA2aTaskLifecycle.requireMutable(entry.task, "append artifacts");
        entry.task = WayangA2aTaskSnapshots.withAppendedArtifact(entry.task, artifact);
        entry.events.add(WayangA2aTaskEvent.artifact(nextSequence(), entry.task, artifact));
        return entry.task;
    }

    @Override
    public synchronized A2aTask cancel(String taskId, A2aMessage message) {
        Entry entry = entry(taskId);
        WayangA2aTaskLifecycle.requireMutable(entry.task, "cancel");
        A2aTaskStatus status = new A2aTaskStatus(
                A2aTaskState.TASK_STATE_CANCELED,
                message,
                Instant.now().toString());
        entry.task = WayangA2aTaskSnapshots.withStatus(entry.task, status);
        entry.events.add(WayangA2aTaskEvent.status(nextSequence(), entry.task, status, true));
        return entry.task;
    }

    @Override
    public synchronized List<WayangA2aTaskEvent> events(String taskId, long afterSequence, int limit) {
        Entry entry = entry(taskId);
        return WayangA2aTaskEventCursor.of(afterSequence, limit).slice(entry.events);
    }

    @Override
    public synchronized WayangA2aPushNotificationConfig putPushNotificationConfig(
            WayangA2aPushNotificationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        Entry entry = entry(config.taskId());
        entry.pushConfigs.put(config.configId(), config);
        return config;
    }

    @Override
    public synchronized Optional<WayangA2aPushNotificationConfig> getPushNotificationConfig(
            String taskId,
            String configId) {
        Entry entry = entry(taskId);
        return Optional.ofNullable(entry.pushConfigs.get(WayangA2aMaps.required(configId, "configId")));
    }

    @Override
    public synchronized List<WayangA2aPushNotificationConfig> listPushNotificationConfigs(String taskId) {
        Entry entry = entry(taskId);
        return List.copyOf(entry.pushConfigs.values());
    }

    @Override
    public synchronized boolean deletePushNotificationConfig(String taskId, String configId) {
        Entry entry = entry(taskId);
        return entry.pushConfigs.remove(WayangA2aMaps.required(configId, "configId")) != null;
    }

    private Entry entry(String taskId) {
        Entry entry = entries.get(WayangA2aMaps.required(taskId, "taskId"));
        if (entry == null) {
            throw new IllegalArgumentException("A2A task not found: " + taskId);
        }
        return entry;
    }

    private long nextSequence() {
        return nextSequence++;
    }

    private static final class Entry {
        private A2aTask task;
        private final List<WayangA2aTaskEvent> events = new ArrayList<>();
        private final Map<String, WayangA2aPushNotificationConfig> pushConfigs = new LinkedHashMap<>();

        private Entry(A2aTask task) {
            this.task = task;
        }
    }
}
