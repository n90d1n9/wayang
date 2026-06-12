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

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearnedSkillPersistenceAdapterTest {

    @Test
    void skillManagementAdapterExposesConfiguredTargets(@TempDir Path tempDir) {
        SkillManagementService service = new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                "definitions", "database",
                "artifacts", "s3",
                "fallback", "file-system"))
                .routePlan()
                .targetPlan();

        HermesLearnedSkillPersistenceAdapter adapter =
                SkillManagementHermesLearnedSkillPersistenceAdapter.from(service, targetPlan);

        assertThat(adapter.adapterId()).isEqualTo("skill-management");
        assertThat(adapter.targetPlan().targetSummary())
                .isEqualTo("definitions=database,artifacts=s3");
        assertThat(adapter.toMetadata())
                .containsEntry("adapterId", "skill-management")
                .containsKey("targetPlan");
    }
}
