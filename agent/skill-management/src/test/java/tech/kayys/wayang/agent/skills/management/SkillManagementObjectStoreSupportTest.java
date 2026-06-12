package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementObjectStoreSupportTest {

    @Test
    void listsSortedMatchingKeysWithinPrefix() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        objectStore.put("tenant/skills/b.properties", bytes("b"));
        objectStore.put("tenant/skills/a.properties", bytes("a"));
        objectStore.put("tenant/skills/ignore.txt", bytes("x"));
        objectStore.put("tenant/other/c.properties", bytes("c"));

        assertThat(SkillManagementObjectStoreSupport.keysWithExtension(
                objectStore,
                "tenant/skills/",
                ".properties"))
                .containsExactly("tenant/skills/a.properties", "tenant/skills/b.properties");
    }

    @Test
    void readsPutsAndDeletesObjects() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();

        SkillManagementObjectStoreSupport.put(objectStore, "tenant/value.txt", bytes("hello"));

        assertThat(SkillManagementObjectStoreSupport.read(
                objectStore,
                "tenant/value.txt",
                content -> new String(content, StandardCharsets.UTF_8)))
                .contains("hello");
        assertThat(SkillManagementObjectStoreSupport.delete(objectStore, "tenant/value.txt")).isTrue();
        assertThat(SkillManagementObjectStoreSupport.read(
                objectStore,
                "tenant/value.txt",
                content -> new String(content, StandardCharsets.UTF_8)))
                .isEmpty();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
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
