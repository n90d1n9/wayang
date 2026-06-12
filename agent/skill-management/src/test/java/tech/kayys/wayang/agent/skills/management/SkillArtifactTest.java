package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactTest {

    @Test
    void defensivelyCopiesContentAndMetadata() {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("z", "last");
        metadata.put("a", "first");

        SkillArtifact artifact = new SkillArtifact(
                SkillArtifactReference.resource("planner", "prompt", "v1"),
                content,
                " text/plain ",
                metadata);

        content[0] = 'x';
        metadata.put("b", "changed");
        byte[] returned = artifact.content();
        returned[1] = 'x';

        assertThat(new String(artifact.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
        assertThat(artifact.contentType()).isEqualTo("text/plain");
        assertThat(artifact.metadata())
                .hasSize(2)
                .containsEntry("a", "first")
                .containsEntry("z", "last");
        assertThatThrownBy(() -> artifact.metadata().put("c", "blocked"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaultsEmptyContentAndContentType() {
        SkillArtifact artifact = SkillArtifact.of(
                SkillArtifactReference.packageArtifact("planner", null),
                null);

        assertThat(artifact.content()).isEmpty();
        assertThat(artifact.contentType()).isEqualTo(SkillArtifact.DEFAULT_CONTENT_TYPE);
        assertThat(artifact.sizeBytes()).isZero();
    }

    @Test
    void buildsTextArtifacts() {
        SkillArtifact artifact = SkillArtifact.text(
                SkillArtifactReference.mcpDescriptor("planner", "tools", "v1"),
                "tool-list");

        assertThat(new String(artifact.content(), StandardCharsets.UTF_8)).isEqualTo("tool-list");
        assertThat(artifact.contentType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(artifact.sizeBytes()).isEqualTo(9);
    }

    @Test
    void computesStableSha256Digest() {
        SkillArtifact artifact = SkillArtifact.text(
                SkillArtifactReference.resource("planner", "prompt", "v1"),
                "hello");

        assertThat(artifact.sha256())
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void requiresReference() {
        assertThatThrownBy(() -> SkillArtifact.of(null, new byte[] {1}))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reference");
    }
}
