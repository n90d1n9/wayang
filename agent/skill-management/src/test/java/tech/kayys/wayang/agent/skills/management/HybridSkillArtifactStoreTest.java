package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridSkillArtifactStoreTest {

    @Test
    void readPathsUseFallbackWhenPrimaryIsUnavailable() {
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        fallback.putArtifact(SkillArtifact.text(reference, "fallback"));
        HybridSkillArtifactStore hybrid = new HybridSkillArtifactStore(
                new FailingReadSkillArtifactStore(),
                fallback);

        assertThat(hybrid.getArtifact(reference))
                .hasValueSatisfying(artifact -> assertThat(text(artifact)).isEqualTo("fallback"));
        assertThat(hybrid.listArtifacts("planner")).containsExactly(reference);
    }

    @Test
    void primaryArtifactsOverrideFallbackCatalogEntries() {
        InMemorySkillArtifactStore primary = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        fallback.putArtifact(SkillArtifact.text(packageArtifact, "package"));
        fallback.putArtifact(SkillArtifact.text(prompt, "old"));
        primary.putArtifact(SkillArtifact.text(prompt, "new"));
        HybridSkillArtifactStore hybrid = new HybridSkillArtifactStore(primary, fallback);

        assertThat(hybrid.getArtifact(prompt))
                .hasValueSatisfying(artifact -> assertThat(text(artifact)).isEqualTo("new"));
        assertThat(hybrid.listArtifacts("planner"))
                .containsExactly(packageArtifact, prompt);
    }

    @Test
    void fallbackReadRepairsMissingPrimaryArtifact() {
        InMemorySkillArtifactStore primary = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        fallback.putArtifact(SkillArtifact.text(prompt, "fallback"));
        HybridSkillArtifactStore hybrid = new HybridSkillArtifactStore(primary, fallback);

        assertThat(hybrid.getArtifact(prompt))
                .hasValueSatisfying(artifact -> assertThat(text(artifact)).isEqualTo("fallback"));

        assertThat(primary.getArtifact(prompt))
                .hasValueSatisfying(artifact -> assertThat(text(artifact)).isEqualTo("fallback"));
    }

    @Test
    void listLimitIsAppliedAfterMergedOrdering() {
        InMemorySkillArtifactStore primary = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");
        SkillArtifactReference firstResource = SkillArtifactReference.resource("planner", "a", "v1");
        SkillArtifactReference secondResource = SkillArtifactReference.resource("planner", "b", "v1");
        fallback.putArtifact(SkillArtifact.of(secondResource, new byte[] {2}));
        primary.putArtifact(SkillArtifact.of(firstResource, new byte[] {1}));
        primary.putArtifact(SkillArtifact.of(packageArtifact, new byte[] {3}));
        HybridSkillArtifactStore hybrid = new HybridSkillArtifactStore(primary, fallback);

        assertThat(hybrid.listArtifacts(SkillArtifactQuery.forSkill("planner", 2)))
                .containsExactly(packageArtifact, firstResource);
    }

    @Test
    void writesStillRequirePrimary() {
        HybridSkillArtifactStore hybrid = new HybridSkillArtifactStore(
                new FailingWriteSkillArtifactStore(),
                new InMemorySkillArtifactStore());

        assertThatThrownBy(() -> hybrid.putArtifact(SkillArtifact.text(
                SkillArtifactReference.resource("planner", "prompt", "v1"),
                "payload")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("primary write unavailable");
    }

    @Test
    void deletesFromPrimaryAndFallback() {
        InMemorySkillArtifactStore primary = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        SkillArtifactReference reference = SkillArtifactReference.mcpDescriptor("planner", "tools", "v1");
        primary.putArtifact(SkillArtifact.text(reference, "primary"));
        fallback.putArtifact(SkillArtifact.text(reference, "fallback"));
        HybridSkillArtifactStore hybrid = new HybridSkillArtifactStore(primary, fallback);

        assertThat(hybrid.deleteArtifact(reference)).isTrue();

        assertThat(primary.getArtifact(reference)).isEmpty();
        assertThat(fallback.getArtifact(reference)).isEmpty();
    }

    private static String text(SkillArtifact artifact) {
        return new String(artifact.content(), StandardCharsets.UTF_8);
    }

    private static final class FailingReadSkillArtifactStore implements SkillArtifactStore {
        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            throw new IllegalStateException("primary read unavailable");
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            throw new IllegalStateException("primary read unavailable");
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            return false;
        }
    }

    private static final class FailingWriteSkillArtifactStore implements SkillArtifactStore {
        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            return Optional.empty();
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            return List.of();
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
            throw new IllegalStateException("primary write unavailable");
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            throw new IllegalStateException("primary write unavailable");
        }
    }
}
