package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearnedSkillRepositoryFactoryTest {

    @Test
    void createsDefaultSkillManagementRepositoryWhenConfigIsMissing(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = HermesLearnedSkillRepositoryFactory.create(
                service(tempDir),
                null,
                Optional.empty(),
                Optional.empty());

        assertThat(repository.persistenceMetadata())
                .containsEntry("adapterId", "skill-management");
        assertThat(repository.targetPlan().targetSummary())
                .isEqualTo("definitions=skill-management,artifacts=skill-management");
    }

    @Test
    void createsFileBackedRepositoryFromPersistenceHints(@TempDir Path tempDir) {
        Path root = tempDir.resolve("learned");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        HermesSkillPersistenceHintKeys.DEFINITIONS, "file-system",
                        HermesSkillPersistenceHintKeys.ARTIFACTS, "file-system",
                        HermesSkillPersistenceHintKeys.FALLBACK, "file-system",
                        HermesSkillPersistenceHintKeys.FILE_ROOT, root.toString()))
                .build();

        HermesLearnedSkillRepository repository = HermesLearnedSkillRepositoryFactory.create(
                service(tempDir),
                config,
                Optional.empty(),
                Optional.empty());

        assertThat(repository.persistenceMetadata())
                .containsEntry("adapterId", "file-system")
                .containsEntry("storageFamily", "file-system")
                .containsEntry("definitionDirectory", root.resolve("definitions").toAbsolutePath().normalize().toString())
                .containsEntry("artifactDirectory", root.resolve("artifacts").toAbsolutePath().normalize().toString());
        assertThat(repository.targetPlan().targetSummary())
                .isEqualTo("definitions=file-system,artifacts=file-system");
    }

    private static SkillManagementService service(Path tempDir) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
    }
}
