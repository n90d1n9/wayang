package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-store-backed event sink/reader for cloud and S3-compatible history.
 */
public final class ObjectStorageSkillManagementEventStore
        implements SkillManagementEventSink, SkillManagementEventReader, SkillManagementEventPruner {

    public static final String DEFAULT_PREFIX = "skill-management/events";

    private final SkillManagementObjectStore objectStore;
    private final String prefix;
    private final int maxEvents;

    public ObjectStorageSkillManagementEventStore(SkillManagementObjectStore objectStore) {
        this(objectStore, DEFAULT_PREFIX);
    }

    public ObjectStorageSkillManagementEventStore(SkillManagementObjectStore objectStore, String prefix) {
        this(objectStore, prefix, InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public ObjectStorageSkillManagementEventStore(
            SkillManagementObjectStore objectStore,
            String prefix,
            int maxEvents) {
        this.objectStore = Objects.requireNonNull(objectStore, "objectStore");
        this.prefix = SkillManagementObjectKeys.normalizePrefix(prefix, DEFAULT_PREFIX);
        this.maxEvents = SkillManagementEventRetention.normalizeCapacity(maxEvents);
    }

    @Override
    public synchronized void record(SkillManagementEvent event) {
        if (event == null) {
            return;
        }
        SkillManagementObjectStoreSupport.put(
                objectStore,
                keyFor(event),
                SkillManagementEventPropertiesCodec.toBytes(event));
        pruneToCapacity();
    }

    @Override
    public synchronized SkillManagementEventPage query(SkillManagementEventQuery query) {
        return SkillManagementEventPages.from(events(), query);
    }

    public synchronized List<SkillManagementEvent> events() {
        return eventKeys().stream()
                .map(this::readKey)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public synchronized SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
        SkillManagementEventPruneOptions resolved =
                SkillManagementEventRetention.resolve(options, maxEvents);
        List<String> keys = eventKeys();
        List<String> targets = SkillManagementEventRetention.oldestToPrune(keys, resolved.keepLatestEvents());
        if (!resolved.dryRun()) {
            targets.forEach(this::deleteKey);
        }
        return SkillManagementEventPruneResult.success(resolved, keys.size(), targets);
    }

    private void pruneToCapacity() {
        SkillManagementEventRetention.oldestToPrune(eventKeys(), maxEvents)
                .forEach(this::deleteKey);
    }

    private List<String> eventKeys() {
        return SkillManagementObjectStoreSupport.keys(
                objectStore,
                prefix,
                SkillManagementEventReferences::hasStorageExtension);
    }

    private Optional<SkillManagementEvent> readKey(String key) {
        return SkillManagementObjectStoreSupport.read(
                objectStore,
                key,
                content -> SkillManagementEventPropertiesCodec.fromBytes(content, key));
    }

    private void deleteKey(String key) {
        SkillManagementObjectStoreSupport.delete(objectStore, key);
    }

    private String keyFor(SkillManagementEvent event) {
        return prefix + SkillManagementEventReferences.storageReference(event);
    }
}
