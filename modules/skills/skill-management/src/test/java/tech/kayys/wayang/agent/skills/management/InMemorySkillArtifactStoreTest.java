package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemorySkillArtifactStoreTest {

    @Test
    void storesRetrievesListsAndDeletesArtifacts() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");

        store.putArtifact(SkillArtifact.text(prompt, "hello"));
        store.putArtifact(SkillArtifact.of(packageArtifact, new byte[] {1, 2, 3}));

        assertThat(store.getArtifact(prompt))
                .hasValueSatisfying(artifact -> assertThat(new String(
                        artifact.content(),
                        StandardCharsets.UTF_8)).isEqualTo("hello"));
        assertThat(store.listArtifacts("planner"))
                .containsExactly(packageArtifact, prompt);
        assertThat(store.listArtifacts(SkillArtifactQuery.forKind("planner", SkillArtifactKind.RESOURCE, 10)))
                .containsExactly(prompt);

        assertThat(store.deleteArtifact(prompt)).isTrue();
        assertThat(store.deleteArtifact(prompt)).isFalse();
        assertThat(store.getArtifact(prompt)).isEmpty();
        assertThat(store.listArtifacts()).containsExactly(packageArtifact);
    }

    @Test
    void honorsQueryLimitAndOrdering() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillArtifactReference b = SkillArtifactReference.resource("planner", "b", "v1");
        SkillArtifactReference a = SkillArtifactReference.resource("planner", "a", "v1");
        SkillArtifactReference c = SkillArtifactReference.resource("planner", "c", "v1");

        store.putArtifact(SkillArtifact.of(b, new byte[] {2}));
        store.putArtifact(SkillArtifact.of(a, new byte[] {1}));
        store.putArtifact(SkillArtifact.of(c, new byte[] {3}));

        assertThat(store.listArtifacts(SkillArtifactQuery.forSkill("planner", 2)))
                .containsExactly(a, b);
    }

    @Test
    void isolatesStoredPayloadsFromCallerMutation() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        store.putArtifact(SkillArtifact.of(reference, content));
        content[0] = 'x';
        SkillArtifact stored = store.getArtifact(reference).orElseThrow();
        byte[] returned = stored.content();
        returned[1] = 'x';

        assertThat(new String(store.getArtifact(reference).orElseThrow().content(), StandardCharsets.UTF_8))
                .isEqualTo("hello");
    }

    @Test
    void rejectsNullInputs() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();

        assertThatThrownBy(() -> store.getArtifact(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reference");
        assertThatThrownBy(() -> store.putArtifact(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("artifact");
        assertThatThrownBy(() -> store.deleteArtifact(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reference");
    }

    @Test
    void treatsNullQueryAsDefaultListing() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.definition("planner");

        store.putArtifact(SkillArtifact.of(reference, List.of(1, 2, 3).toString().getBytes(StandardCharsets.UTF_8)));

        assertThat(store.listArtifacts((SkillArtifactQuery) null)).containsExactly(reference);
    }
}
