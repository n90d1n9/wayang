package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillArtifactStoreFactoryProviderRegistryTest {

    @Test
    void createsLeafArtifactStoresThroughProviderRegistry(@TempDir Path tempDir) {
        SkillArtifactStoreFactory factory = new SkillArtifactStoreFactory();

        assertThat(factory.create(null)).isInstanceOf(InMemorySkillArtifactStore.class);
        assertThat(factory.create(SkillArtifactStoreConfig.fileSystem(tempDir)))
                .isInstanceOf(FileSystemSkillArtifactStore.class);
    }

    @Test
    void validatesProviderBackedRuntimeDependencies() {
        SkillArtifactStoreFactory factory = new SkillArtifactStoreFactory();

        SkillStoreConfigValidationResult result = factory.validate(
                SkillArtifactStoreConfig.objectStorage("wayang/artifacts"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors())
                .contains("Artifact store kind OBJECT_STORAGE requires dependency: objectStore");
    }

    @Test
    void createsCustomArtifactStoresThroughProviderRegistry() {
        SkillArtifactStore custom = new InMemorySkillArtifactStore();
        SkillArtifactStoreFactory factory = new SkillArtifactStoreFactory(
                Map.of("tenant-artifacts", custom));

        assertThat(factory.create(SkillArtifactStoreConfig.custom("tenant-artifacts"))).isSameAs(custom);
    }
}
