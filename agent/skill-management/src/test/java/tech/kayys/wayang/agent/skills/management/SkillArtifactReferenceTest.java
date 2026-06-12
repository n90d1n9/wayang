package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactReferenceTest {

    @Test
    void normalizesReferenceSegmentsAndDefaultsNameAndVersion() {
        SkillArtifactReference reference = new SkillArtifactReference(
                " planner ",
                SkillArtifactKind.DEFINITION,
                null,
                null);

        assertThat(reference.skillId()).isEqualTo("planner");
        assertThat(reference.kind()).isEqualTo(SkillArtifactKind.DEFINITION);
        assertThat(reference.name()).isEqualTo("definition");
        assertThat(reference.version()).isEqualTo("current");
        assertThat(reference.pathSegments())
                .containsExactly("planner", "definition", "definition", "current");
    }

    @Test
    void buildsStableRelativePathsStorageKeysAndQualifiedNames() {
        SkillArtifactReference reference = SkillArtifactReference.resource(
                "planner",
                "prompt-templates",
                "v1");

        assertThat(reference.relativePath())
                .isEqualTo("planner/resource/prompt-templates/v1");
        assertThat(reference.storageKey())
                .isEqualTo("skill-management/artifacts/planner/resource/prompt-templates/v1");
        assertThat(reference.storageKey(" /tenant-a/skills "))
                .isEqualTo("tenant-a/skills/planner/resource/prompt-templates/v1");
        assertThat(reference.storageKey(null, ""))
                .isEqualTo("planner/resource/prompt-templates/v1");
        assertThat(reference.qualifiedName())
                .isEqualTo("planner:resource:prompt-templates:v1");
    }

    @Test
    void exposesFactoriesForKnownArtifactSurfaces() {
        assertThat(SkillArtifactReference.definition("planner").pathSegments())
                .containsExactly("planner", "definition", "definition", "current");
        assertThat(SkillArtifactReference.lifecycleState("planner").pathSegments())
                .containsExactly("planner", "lifecycle-state", "lifecycle-state", "current");
        assertThat(SkillArtifactReference.eventHistory("planner").pathSegments())
                .containsExactly("planner", "event-history", "event-history", "current");
        assertThat(SkillArtifactReference.packageArtifact("planner", "v2").pathSegments())
                .containsExactly("planner", "package", "package", "v2");
        assertThat(SkillArtifactReference.ragIndex("planner", "kb", "2026-06").pathSegments())
                .containsExactly("planner", "rag-index", "kb", "2026-06");
        assertThat(SkillArtifactReference.mcpDescriptor("planner", "tools", "v1").pathSegments())
                .containsExactly("planner", "mcp-descriptor", "tools", "v1");
    }

    @Test
    void rejectsPathLikeSegments() {
        assertThatThrownBy(() -> SkillArtifactReference.definition("team/planner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skill artifact reference skill id");
        assertThatThrownBy(() -> SkillArtifactReference.resource("planner", "../templates", "v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skill artifact reference name");
        assertThatThrownBy(() -> SkillArtifactReference.resource("planner", "templates", "v1/latest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skill artifact reference version");
    }

    @Test
    void rejectsMissingKind() {
        assertThatThrownBy(() -> new SkillArtifactReference("planner", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skill artifact kind is required");
    }
}
