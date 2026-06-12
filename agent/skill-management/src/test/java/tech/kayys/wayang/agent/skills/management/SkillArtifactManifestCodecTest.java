package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactManifestCodecTest {

    private final SkillArtifactManifestCodec codec = new SkillArtifactManifestCodec();

    @Test
    void roundTripsArtifactManifestMetadata() {
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = new SkillArtifact(
                reference,
                "hello".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("source", "test", "tenant", "tenant-a"));

        SkillArtifactManifest manifest = codec.fromBytes(codec.toBytes(artifact), "manifest");

        assertThat(manifest.reference()).isEqualTo(reference);
        assertThat(manifest.contentType()).isEqualTo("text/plain");
        assertThat(manifest.sizeBytes()).isEqualTo(5);
        assertThat(manifest.sha256())
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(manifest.metadata())
                .containsEntry("source", "test")
                .containsEntry("tenant", "tenant-a");
    }

    @Test
    void rebuildsArtifactWithManifestMetadata() {
        SkillArtifactReference reference = SkillArtifactReference.packageArtifact("planner", "v2");
        SkillArtifactManifest manifest =
                new SkillArtifactManifest(
                        reference,
                        "application/zip",
                        Map.of("sha256", "abc"),
                        3,
                        SkillArtifact.sha256(new byte[] {1, 2, 3}));

        SkillArtifact artifact = manifest.toArtifact(new byte[] {1, 2, 3});

        assertThat(artifact.reference()).isEqualTo(reference);
        assertThat(artifact.contentType()).isEqualTo("application/zip");
        assertThat(artifact.metadata()).containsEntry("sha256", "abc");
        assertThat(artifact.content()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsPayloadWithUnexpectedSize() {
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactManifest manifest =
                new SkillArtifactManifest(reference, "text/plain", Map.of(), 5, "");

        assertThatThrownBy(() -> manifest.toArtifact(new byte[] {1, 2}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("content size mismatch")
                .hasMessageContaining("planner:resource:prompt:v1");
    }

    @Test
    void rejectsPayloadWithUnexpectedSha256() {
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactManifest manifest =
                new SkillArtifactManifest(reference, "text/plain", Map.of(), 2, "deadbeef");

        assertThatThrownBy(() -> manifest.toArtifact(new byte[] {1, 2}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 mismatch")
                .hasMessageContaining("planner:resource:prompt:v1");
    }
}
