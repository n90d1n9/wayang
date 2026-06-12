package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillLifecycleStateStoreFactoryProviderRegistryTest {

    @Test
    void createsLeafLifecycleStoresThroughProviderRegistry(@TempDir Path tempDir) {
        SkillLifecycleStateStoreFactory factory = new SkillLifecycleStateStoreFactory();

        assertThat(factory.create(null)).isInstanceOf(InMemorySkillLifecycleStateStore.class);
        assertThat(factory.create(SkillLifecycleStateStoreConfig.fileSystem(tempDir)))
                .isInstanceOf(FileSystemSkillLifecycleStateStore.class);
    }

    @Test
    void validatesProviderBackedRuntimeDependencies() {
        SkillLifecycleStateStoreFactory factory = new SkillLifecycleStateStoreFactory();

        SkillStoreConfigValidationResult result = factory.validate(
                SkillLifecycleStateStoreConfig.objectStorage("wayang/lifecycle"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors())
                .contains("Lifecycle store kind OBJECT_STORAGE requires dependency: objectStore");
    }

    @Test
    void createsCustomLifecycleStoresThroughProviderRegistry() {
        SkillLifecycleStateStore custom = new InMemorySkillLifecycleStateStore();
        SkillLifecycleStateStoreFactory factory = new SkillLifecycleStateStoreFactory(
                null,
                Map.of("tenant-lifecycle", custom));

        assertThat(factory.create(SkillLifecycleStateStoreConfig.custom("tenant-lifecycle"))).isSameAs(custom);
    }
}
