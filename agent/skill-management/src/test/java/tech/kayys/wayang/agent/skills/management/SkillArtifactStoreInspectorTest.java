package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkillArtifactStoreInspectorTest {

    private final SkillArtifactStoreInspector inspector = new SkillArtifactStoreInspector();

    @Test
    void inspectsArtifactStoreCatalog() {
        InMemorySkillArtifactStore store = new InMemorySkillArtifactStore();
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");
        SkillArtifactReference resource = SkillArtifactReference.resource("planner", "prompt", "v1");
        store.putArtifact(SkillArtifact.of(resource, new byte[] {1}));
        store.putArtifact(SkillArtifact.of(packageArtifact, new byte[] {2}));

        SkillArtifactStoreInspection inspection = inspector.inspect("artifacts", store);

        assertThat(inspection.status()).isEqualTo(SkillArtifactStoreHealthStatus.READY);
        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.artifactCount()).isEqualTo(2);
        assertThat(inspection.artifactReferences())
                .containsExactly(
                        "planner:package:package:v1",
                        "planner:resource:prompt:v1");
        assertThat(inspection.kindCounts())
                .containsEntry("package", 1)
                .containsEntry("resource", 1);
        assertThat(inspection.capabilities().names()).contains("read", "write", "delete", "list");
    }

    @Test
    void reportsUnavailableArtifactStoreWithoutThrowing() {
        SkillArtifactStoreInspection inspection = inspector.inspect("artifacts", new FailingArtifactStore());

        assertThat(inspection.status()).isEqualTo(SkillArtifactStoreHealthStatus.UNAVAILABLE);
        assertThat(inspection.ready()).isFalse();
        assertThat(inspection.failure()).contains("artifact unavailable");
    }

    @Test
    void includesHybridArtifactStoreChildren() {
        InMemorySkillArtifactStore primary = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        primary.putArtifact(SkillArtifact.of(SkillArtifactReference.resource("planner", "prompt", "v2"),
                new byte[] {2}));
        fallback.putArtifact(SkillArtifact.of(SkillArtifactReference.resource("planner", "prompt", "v1"),
                new byte[] {1}));

        SkillArtifactStoreInspection inspection = inspector.inspect(
                "hybrid",
                new HybridSkillArtifactStore(primary, fallback));

        assertThat(inspection.capabilities().names()).contains("primary-fallback");
        assertThat(inspection.children()).hasSize(2);
        assertThat(inspection.children()).extracting(SkillArtifactStoreInspection::name)
                .containsExactly("primary", "fallback");
        assertThat(inspection.children().get(0).artifactReferences())
                .containsExactly("planner:resource:prompt:v2");
        assertThat(inspection.children().get(1).artifactReferences())
                .containsExactly("planner:resource:prompt:v1");
    }

    private static final class FailingArtifactStore implements SkillArtifactStore {
        @Override
        public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
            return Optional.empty();
        }

        @Override
        public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
            throw new IllegalStateException("artifact unavailable");
        }

        @Override
        public void putArtifact(SkillArtifact artifact) {
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            return false;
        }
    }
}
