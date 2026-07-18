package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemSkillArtifactStoreTest {

    @Test
    void persistsArtifactsAcrossStoreInstances(@TempDir Path tempDir) throws Exception {
        FileSystemSkillArtifactStore writer = new FileSystemSkillArtifactStore(tempDir);
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = new SkillArtifact(
                reference,
                "hello".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("tenant", "tenant-a"));

        writer.putArtifact(artifact);

        FileSystemSkillArtifactStore reader = new FileSystemSkillArtifactStore(tempDir);
        assertThat(reader.getArtifact(reference))
                .hasValueSatisfying(reloaded -> {
                    assertThat(new String(reloaded.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
                    assertThat(reloaded.contentType()).isEqualTo("text/plain");
                    assertThat(reloaded.metadata()).containsEntry("tenant", "tenant-a");
                });
        assertThat(reader.listArtifacts("planner")).containsExactly(reference);
        assertThat(Files.isRegularFile(tempDir.resolve("planner/resource/prompt/v1/content.bin"))).isTrue();
        assertThat(Files.isRegularFile(tempDir.resolve("planner/resource/prompt/v1/artifact.properties"))).isTrue();
    }

    @Test
    void listsArtifactsByQueryRecursively(@TempDir Path tempDir) {
        FileSystemSkillArtifactStore store = new FileSystemSkillArtifactStore(tempDir);
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
    void removesPayloadManifestAndEmptyDirectories(@TempDir Path tempDir) {
        FileSystemSkillArtifactStore store = new FileSystemSkillArtifactStore(tempDir);
        SkillArtifactReference reference = SkillArtifactReference.mcpDescriptor("planner", "tools", "v1");

        store.putArtifact(SkillArtifact.text(reference, "tools"));

        assertThat(store.deleteArtifact(reference)).isTrue();
        assertThat(store.deleteArtifact(reference)).isFalse();
        assertThat(store.getArtifact(reference)).isEmpty();
        assertThat(store.listArtifacts("planner")).isEmpty();
        assertThat(Files.exists(tempDir.resolve("planner"))).isFalse();
    }

    @Test
    void readsPayloadWhenManifestIsMissing(@TempDir Path tempDir) throws Exception {
        FileSystemSkillArtifactStore store = new FileSystemSkillArtifactStore(tempDir);
        SkillArtifactReference reference = SkillArtifactReference.ragIndex("planner", "kb", "v1");
        Path artifactDirectory = tempDir.resolve("planner/rag-index/kb/v1");
        Files.createDirectories(artifactDirectory);
        Files.write(artifactDirectory.resolve("content.bin"), new byte[] {1, 2});

        assertThat(store.getArtifact(reference))
                .hasValueSatisfying(artifact -> {
                    assertThat(artifact.content()).containsExactly(1, 2);
                    assertThat(artifact.contentType()).isEqualTo(SkillArtifact.DEFAULT_CONTENT_TYPE);
                    assertThat(artifact.metadata()).isEmpty();
        });
        assertThat(store.listArtifacts("planner")).isEmpty();
    }

    @Test
    void rejectsCorruptedPayloadWhenManifestHasDigest(@TempDir Path tempDir) throws Exception {
        FileSystemSkillArtifactStore store = new FileSystemSkillArtifactStore(tempDir);
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        store.putArtifact(SkillArtifact.text(reference, "hello"));
        Files.write(
                tempDir.resolve("planner/resource/prompt/v1/content.bin"),
                "other".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> store.getArtifact(reference))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 mismatch")
                .hasMessageContaining("planner:resource:prompt:v1");
    }
}
