package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectStorageSkillArtifactStoreTest {

    @Test
    void persistsArtifactsThroughObjectStore() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillArtifactStore writer =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = new SkillArtifact(
                reference,
                "hello".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("tenant", "tenant-a"));

        writer.putArtifact(artifact);

        ObjectStorageSkillArtifactStore reader =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        assertThat(reader.getArtifact(reference))
                .hasValueSatisfying(reloaded -> {
                    assertThat(new String(reloaded.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
                    assertThat(reloaded.contentType()).isEqualTo("text/plain");
                    assertThat(reloaded.metadata()).containsEntry("tenant", "tenant-a");
                });
        assertThat(reader.listArtifacts("planner")).containsExactly(reference);
        assertThat(objectStore.list("tenant-a/artifacts/"))
                .containsExactlyInAnyOrder(
                        "tenant-a/artifacts/planner/resource/prompt/v1/content.bin",
                        "tenant-a/artifacts/planner/resource/prompt/v1/artifact.properties");
    }

    @Test
    void listsArtifactsByQueryFromManifests() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillArtifactStore store =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        SkillArtifactReference b = SkillArtifactReference.resource("planner", "b", "v1");
        SkillArtifactReference a = SkillArtifactReference.resource("planner", "a", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");
        SkillArtifactReference otherSkill = SkillArtifactReference.resource("writer", "a", "v1");

        store.putArtifact(SkillArtifact.of(b, new byte[] {2}));
        store.putArtifact(SkillArtifact.of(a, new byte[] {1}));
        store.putArtifact(SkillArtifact.of(packageArtifact, new byte[] {3}));
        store.putArtifact(SkillArtifact.of(otherSkill, new byte[] {4}));

        assertThat(store.listArtifacts(SkillArtifactQuery.forKind("planner", SkillArtifactKind.RESOURCE, 10)))
                .containsExactly(a, b);
        assertThat(store.listArtifacts(SkillArtifactQuery.forSkill("planner", 2)))
                .containsExactly(packageArtifact, a);
    }

    @Test
    void removesPayloadAndManifest() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillArtifactStore store =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        SkillArtifactReference reference = SkillArtifactReference.mcpDescriptor("planner", "tools", "v1");

        store.putArtifact(SkillArtifact.text(reference, "tools"));

        assertThat(store.deleteArtifact(reference)).isTrue();
        assertThat(store.deleteArtifact(reference)).isFalse();
        assertThat(store.getArtifact(reference)).isEmpty();
        assertThat(objectStore.list("tenant-a/artifacts/")).isEmpty();
    }

    @Test
    void readsPayloadWhenManifestIsMissing() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillArtifactStore store =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        SkillArtifactReference reference = SkillArtifactReference.ragIndex("planner", "kb", "v1");
        objectStore.put(
                "tenant-a/artifacts/planner/rag-index/kb/v1/content.bin",
                new byte[] {1, 2});

        assertThat(store.getArtifact(reference))
                .hasValueSatisfying(artifact -> {
                    assertThat(artifact.content()).containsExactly(1, 2);
                    assertThat(artifact.contentType()).isEqualTo(SkillArtifact.DEFAULT_CONTENT_TYPE);
                    assertThat(artifact.metadata()).isEmpty();
        });
        assertThat(store.listArtifacts("planner")).isEmpty();
    }

    @Test
    void rejectsCorruptedPayloadWhenManifestHasDigest() {
        InMemoryObjectStore objectStore = new InMemoryObjectStore();
        ObjectStorageSkillArtifactStore store =
                new ObjectStorageSkillArtifactStore(objectStore, "tenant-a/artifacts");
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        store.putArtifact(SkillArtifact.text(reference, "hello"));
        objectStore.objects.put(
                "tenant-a/artifacts/planner/resource/prompt/v1/content.bin",
                "other".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> store.getArtifact(reference))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 mismatch")
                .hasMessageContaining("planner:resource:prompt:v1");
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
