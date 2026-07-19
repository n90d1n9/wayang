package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageSkillLifecycleStateStoreTest {

    @Test
    void persistsLifecycleStateThroughObjectStore() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillLifecycleStateStore store =
                new ObjectStorageSkillLifecycleStateStore(objectStore, "tenant-a/lifecycle");
        SkillLifecycleState state = new SkillLifecycleState(
                "planner",
                SkillLifecycleStatus.DISABLED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                7);

        store.save(state);

        ObjectStorageSkillLifecycleStateStore reloaded =
                new ObjectStorageSkillLifecycleStateStore(objectStore, "tenant-a/lifecycle");
        assertThat(reloaded.get("planner")).contains(state);
        assertThat(reloaded.snapshot()).containsEntry("planner", state);
        assertThat(objectStore.list("tenant-a/lifecycle/"))
                .containsExactly("tenant-a/lifecycle/planner.state.properties");
    }

    @Test
    void removesLifecycleStateFromObjectStore() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillLifecycleStateStore store =
                new ObjectStorageSkillLifecycleStateStore(objectStore, "tenant-a/lifecycle");
        store.save(SkillLifecycleState.created("planner"));

        assertThat(store.remove("planner")).isTrue();

        assertThat(new ObjectStorageSkillLifecycleStateStore(objectStore, "tenant-a/lifecycle")
                .get("planner")).isEmpty();
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
