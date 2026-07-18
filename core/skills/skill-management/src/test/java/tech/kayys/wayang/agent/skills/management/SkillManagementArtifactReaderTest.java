package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementArtifactReaderTest {

    @Test
    void getReturnsStoredArtifact() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillManagementArtifactReader reader = new SkillManagementArtifactReader(store);
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        store.putArtifact(SkillArtifact.text(reference, "hello"));

        assertThat(reader.get(reference))
                .hasValueSatisfying(artifact -> assertThat(new String(
                        artifact.content(),
                        StandardCharsets.UTF_8)).isEqualTo("hello"));
    }

    @Test
    void listDelegatesQueryFilteringAndOrdering() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillManagementArtifactReader reader = new SkillManagementArtifactReader(store);
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");
        SkillArtifactReference writerPrompt = SkillArtifactReference.resource("writer", "prompt", "v1");
        store.putArtifact(SkillArtifact.text(writerPrompt, "writer"));
        store.putArtifact(SkillArtifact.text(prompt, "prompt"));
        store.putArtifact(SkillArtifact.text(packageArtifact, "package"));

        assertThat(reader.list(SkillArtifactQuery.forSkill("planner")))
                .containsExactly(packageArtifact, prompt);
        assertThat(reader.list(SkillArtifactQuery.forKind("planner", SkillArtifactKind.RESOURCE, 10)))
                .containsExactly(prompt);
    }

    @Test
    void listBySkillUsesStoreConvenienceQuery() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillManagementArtifactReader reader = new SkillManagementArtifactReader(store);
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference writerPrompt = SkillArtifactReference.resource("writer", "prompt", "v1");
        store.putArtifact(SkillArtifact.text(writerPrompt, "writer"));
        store.putArtifact(SkillArtifact.text(prompt, "prompt"));

        assertThat(reader.list("planner")).containsExactly(prompt);
    }
}
