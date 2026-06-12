package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.SkillArtifactQuery;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemHermesLearnedSkillPersistenceAdapterTest {

    @Test
    void persistsLearnedSkillDefinitionsAndArtifactsOnDisk(@TempDir Path tempDir) {
        FileSystemHermesLearnedSkillPersistenceAdapter adapter =
                FileSystemHermesLearnedSkillPersistenceAdapter.at(tempDir);
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                adapter,
                new HermesSkillMarkdownRenderer());
        HermesLearningSignal signal = signal();
        SkillDefinition skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = repository.create(skill, signal).await().indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(adapter.find(skill.id()).await().indefinitely()).contains(skill);
        assertThat(adapter.listLearnedSkills().await().indefinitely())
                .extracting(SkillDefinition::id)
                .containsExactly(skill.id());
        assertThat(new FileSystemSkillArtifactStore(adapter.artifactDirectory())
                        .listArtifacts(SkillArtifactQuery.forSkill(skill.id())))
                .hasSize(1);
        assertThat(repository.targetPlan().targetSummary())
                .isEqualTo("definitions=file-system,artifacts=file-system");
    }

    @Test
    void metadataDescribesFileSystemTargetDirectories(@TempDir Path tempDir) {
        FileSystemHermesLearnedSkillPersistenceAdapter adapter =
                new FileSystemHermesLearnedSkillPersistenceAdapter(
                        tempDir.resolve("defs"),
                        tempDir.resolve("arts"));

        assertThat(adapter.toMetadata())
                .containsEntry("adapterId", "file-system")
                .containsEntry("storageFamily", "file-system")
                .containsEntry("definitionDirectory", tempDir.resolve("defs").toAbsolutePath().normalize().toString())
                .containsEntry("artifactDirectory", tempDir.resolve("arts").toAbsolutePath().normalize().toString())
                .containsKey("targetPlan");
    }

    private static HermesLearningSignal signal() {
        return new HermesLearningSignal(
                "req-file-adapter",
                "Archive multi tenant audit evidence",
                "Audit evidence archived",
                true,
                List.of(),
                List.of("rag"),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }
}
