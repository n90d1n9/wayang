package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillDefinitionStoreFactoryProviderRegistryTest {

    @Test
    void createsLeafDefinitionStoresThroughProviderRegistry(@TempDir Path tempDir) {
        SkillDefinitionStoreFactory factory = new SkillDefinitionStoreFactory(null);

        SkillDefinitionStore store = factory.create(SkillDefinitionStoreConfig.fileSystem(tempDir));

        assertThat(store).isInstanceOf(FileSystemSkillDefinitionStore.class);
    }

    @Test
    void validatesProviderBackedRuntimeDependencies() {
        SkillDefinitionStoreFactory factory = new SkillDefinitionStoreFactory(null);

        SkillStoreConfigValidationResult result = factory.validate(
                SkillDefinitionStoreConfig.objectStorage("wayang/skills"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors())
                .contains("Skill store kind OBJECT_STORAGE requires dependency: objectStore");
    }

    @Test
    void createsCustomDefinitionStoresThroughProviderRegistry() {
        SkillDefinitionStore custom = new TestSkillDefinitionStore();
        SkillDefinitionStoreFactory factory = new SkillDefinitionStoreFactory(
                null,
                null,
                Map.of("tenant-store", custom));

        assertThat(factory.create(SkillDefinitionStoreConfig.custom("tenant-store"))).isSameAs(custom);
    }
}
