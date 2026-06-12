package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventStoreFactoryProviderRegistryTest {

    @Test
    void createsLeafEventStoresThroughProviderRegistry(@TempDir Path tempDir) {
        SkillManagementEventStoreFactory factory = new SkillManagementEventStoreFactory();

        assertThat(factory.create(null)).isNotNull();
        assertThat(factory.create(SkillManagementEventStoreConfig.memory()))
                .isInstanceOf(InMemorySkillManagementEventSink.class);
        assertThat(factory.create(SkillManagementEventStoreConfig.fileSystem(tempDir)))
                .isInstanceOf(FileSystemSkillManagementEventStore.class);
    }

    @Test
    void validatesProviderBackedRuntimeDependencies() {
        SkillManagementEventStoreFactory factory = new SkillManagementEventStoreFactory();

        SkillStoreConfigValidationResult result = factory.validate(
                SkillManagementEventStoreConfig.objectStorage("wayang/events"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors())
                .contains("Object-storage event store requires a SkillManagementObjectStore");
    }

    @Test
    void createsCustomEventStoresThroughProviderRegistry() {
        SkillManagementEventSink custom = new InMemorySkillManagementEventSink();
        SkillManagementEventStoreFactory factory = new SkillManagementEventStoreFactory(
                Map.of("audit-events", custom));

        assertThat(factory.create(SkillManagementEventStoreConfig.custom("audit-events"))).isSameAs(custom);
    }
}
