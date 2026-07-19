package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectStorageSkillManagementEventStoreTest {

    @Test
    void persistsAndQueriesEventsThroughObjectStore() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillManagementEventStore writer =
                new ObjectStorageSkillManagementEventStore(objectStore, "tenant-a/events", 10);
        writer.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        writer.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        ObjectStorageSkillManagementEventStore reader =
                new ObjectStorageSkillManagementEventStore(objectStore, "tenant-a/events", 10);
        SkillManagementEventPage failures = reader.query(SkillManagementEventQuery.failures(10));

        assertThat(objectStore.objects).hasSize(2);
        assertThat(reader.events()).hasSize(2);
        assertThat(failures.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
        assertThat(failures.events().get(0).attributes()).containsEntry("status", "DELETE_SKILL");
    }

    @Test
    void prunesOldestEventsWhenRetentionIsExceeded() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillManagementEventStore store =
                new ObjectStorageSkillManagementEventStore(objectStore, "tenant-a/events", 2);

        store.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        assertThat(store.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
        assertThat(objectStore.objects).hasSize(2);
    }

    @Test
    void prunesOldestEventsOnDemandWithDryRun() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillManagementEventStore store =
                new ObjectStorageSkillManagementEventStore(objectStore, "tenant-a/events", 10);
        store.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventPruneResult preview =
                store.pruneEvents(SkillManagementEventPruneOptions.dryRun(1));

        assertThat(preview.prunedEvents()).isEqualTo(2);
        assertThat(objectStore.objects).hasSize(3);

        SkillManagementEventPruneResult result =
                store.pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(result.changed()).isTrue();
        assertThat(result.prunedEventReferences()).allMatch(key -> key.startsWith("tenant-a/events/"));
        assertThat(store.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
    }

    @Test
    void ignoresNullEvents() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillManagementEventStore store =
                new ObjectStorageSkillManagementEventStore(objectStore, "tenant-a/events", 10);

        store.record(null);

        assertThat(store.events()).isEmpty();
        assertThat(store.latest().events()).isEmpty();
    }

    @Test
    void factoryBuildsObjectStorageEventStore() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        SkillManagementEventSink sink = new SkillManagementEventStoreFactory(objectStore)
                .create(SkillManagementEventStoreConfig.objectStorage("tenant-a/events", 10));

        sink.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));

        assertThat(sink).isInstanceOf(SkillManagementEventReader.class);
        assertThat(((SkillManagementEventReader) sink).latest().events()).hasSize(1);
    }

    @Test
    void factoryRejectsObjectStorageEventStoreWithoutObjectStore() {
        SkillManagementEventStoreFactory factory = new SkillManagementEventStoreFactory();

        assertThatThrownBy(() -> factory.create(SkillManagementEventStoreConfig.objectStorage("tenant-a/events")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Object-storage event store");
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return new SkillManagementEvent(
                Instant.parse(occurredAt),
                operation,
                skillId,
                success,
                Map.of("status", operation.name()));
    }

    private static final class InMemoryObjectStore implements SkillManagementObjectStore {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        @Override
        public Optional<byte[]> get(String key) {
            return Optional.ofNullable(objects.get(key));
        }

        @Override
        public List<String> list(String prefix) {
            String normalizedPrefix = prefix == null ? "" : prefix;
            return objects.keySet().stream()
                    .filter(key -> key.startsWith(normalizedPrefix))
                    .toList();
        }

        @Override
        public void put(String key, byte[] content) {
            objects.put(key, content);
        }

        @Override
        public boolean delete(String key) {
            return objects.remove(key) != null;
        }
    }
}
