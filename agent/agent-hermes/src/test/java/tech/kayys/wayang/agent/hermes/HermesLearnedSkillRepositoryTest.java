package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillArtifactQuery;
import tech.kayys.wayang.agent.skills.management.SkillArtifactReference;
import tech.kayys.wayang.agent.skills.management.SkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HermesLearnedSkillRepositoryTest {

    @Test
    void delegatesRenderedArtifactThroughPersistenceAdapter() {
        RecordingPersistenceAdapter adapter = new RecordingPersistenceAdapter();
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                adapter,
                new HermesSkillMarkdownRenderer());
        HermesLearningSignal signal = signal();
        SkillDefinition skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = repository.create(skill, signal).await().indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(adapter.createdSkill.id()).isEqualTo(skill.id());
        assertThat(adapter.createdArtifact.reference().skillId()).isEqualTo(skill.id());
        assertThat(adapter.createdArtifact.reference().name()).isEqualTo("SKILL.md");
        assertThat(new String(adapter.createdArtifact.content(), StandardCharsets.UTF_8))
                .contains("learned-from-request: req-repository");
        assertThat(repository.targetPlan().targetSummary())
                .isEqualTo("definitions=skill-management,artifacts=skill-management");
        assertThat(repository.persistenceMetadata())
                .containsEntry("adapterId", "recording")
                .containsKey("targetPlan");
    }

    @Test
    void writesPortableArtifactBeforeActivatingSkill(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir, new FailingSkillArtifactStore());
        HermesSkillDistiller distiller = new HermesSkillDistiller();
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer());
        HermesLearningSignal signal = signal();
        SkillDefinition skill = distiller.distill(signal, HermesAgentModeConfig.defaults());

        Throwable error = catchThrowable(() -> repository.create(skill, signal).await().indefinitely());

        assertThat(error)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to put skill artifact consistently");
        assertThat(service.listSkills().await().indefinitely()).isEmpty();
    }

    @Test
    void readsLineageViewFromLearnedSkillRepository(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir, new InMemorySkillArtifactStore());
        HermesLearningLoop loop = new HermesLearningLoop(service);
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer());
        AgentRequest firstRequest = AgentRequest.builder()
                .requestId("req-lineage-1")
                .prompt("Generate nightly API backup report")
                .build();
        AgentRequest similarRequest = AgentRequest.builder()
                .requestId("req-lineage-2")
                .prompt("Create API backup nightly report")
                .build();

        HermesLearningResult first = loop.learn(firstRequest, response(true, 3)).await().indefinitely();
        HermesLearningResult second = loop.learn(similarRequest, response(true, 4)).await().indefinitely();
        HermesSkillLineageView view = repository.lineage(first.skillId()).await().indefinitely();

        assertThat(second.decision()).isEqualTo(HermesLearningDecision.UPDATED);
        assertThat(view.found()).isTrue();
        assertThat(view.rootSkillId()).isEqualTo(first.skillId());
        assertThat(view.currentSkillId()).isEqualTo(first.skillId());
        assertThat(view.currentRevision()).isEqualTo("2");
        assertThat(view.hasRefinements()).isTrue();
        assertThat(view.sourceRequestIds()).containsExactly("req-lineage-1", "req-lineage-2");
        assertThat(view.mergeStrategies()).containsExactly(HermesSkillRevisionMetadata.REFINEMENT_STRATEGY);
        assertThat(view.entries()).hasSize(1);
        assertThat(view.entries().getFirst().latestRequestId()).isEqualTo("req-lineage-2");
        assertThat(view.toMetadata())
                .containsEntry("found", true)
                .containsEntry("rootSkillId", first.skillId())
                .containsEntry("currentRevision", "2");
    }

    @Test
    void readsLineageCatalogFromLearnedSkillRepository(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir, new InMemorySkillArtifactStore());
        HermesLearningLoop loop = new HermesLearningLoop(service);
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer());

        loop.learn(AgentRequest.builder()
                        .requestId("req-catalog-1")
                        .prompt("Generate nightly API backup report")
                        .build(), response(true, 3))
                .await()
                .indefinitely();
        loop.learn(AgentRequest.builder()
                        .requestId("req-catalog-2")
                        .prompt("Create API backup nightly report")
                        .build(), response(true, 4))
                .await()
                .indefinitely();

        HermesSkillLineageCatalog catalog = repository.lineageCatalog().await().indefinitely();

        assertThat(catalog.learnedSkillCount()).isEqualTo(1);
        assertThat(catalog.rootCount()).isEqualTo(1);
        assertThat(catalog.refinedRootCount()).isEqualTo(1);
        assertThat(catalog.refinedEntryCount()).isEqualTo(1);
        assertThat(catalog.orphanedRootCount()).isZero();
        assertThat(catalog.sourceRequestIds()).containsExactly("req-catalog-1", "req-catalog-2");
        assertThat(catalog.roots()).hasSize(1);
        assertThat(catalog.roots().getFirst().currentRevision()).isEqualTo("2");
        assertThat(catalog.toMetadata())
                .containsEntry("learnedSkillCount", 1)
                .containsEntry("rootCount", 1)
                .containsEntry("refinedRootCount", 1L);
    }

    private static SkillManagementService service(Path tempDir, SkillArtifactStore artifactStore) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                artifactStore,
                SkillManagementEventSink.noop());
    }

    private static HermesLearningSignal signal() {
        return new HermesLearningSignal(
                "req-repository",
                "Archive multi tenant audit evidence",
                "Audit evidence archived",
                true,
                List.of(new AgentState.ReasoningStep(
                        1,
                        "Inspect evidence",
                        new AgentState.AgentAction("rag", "retrieve context", Map.of("query", "audit"), Instant.now()),
                        "Evidence found",
                        10,
                        true)),
                List.of("rag"),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }

    private static AgentResponse response(boolean successful, int stepCount) {
        List<AgentState.ReasoningStep> steps = java.util.stream.IntStream.rangeClosed(1, stepCount)
                .mapToObj(index -> new AgentState.ReasoningStep(
                        index,
                        "Inspect evidence " + index,
                        new AgentState.AgentAction("rag", "retrieve context", Map.of("query", "q" + index), Instant.now()),
                        "Observation " + index,
                        10,
                        true))
                .toList();
        return AgentResponse.builder()
                .runId("run-lineage")
                .requestId("req")
                .answer("Verified report is ready")
                .steps(steps)
                .totalSteps(stepCount)
                .successful(successful)
                .strategy("react")
                .durationMs(25)
                .build();
    }

    private static final class FailingSkillArtifactStore implements SkillArtifactStore {

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
            throw new IllegalStateException("artifact store unavailable");
        }

        @Override
        public boolean deleteArtifact(SkillArtifactReference reference) {
            return false;
        }
    }

    private static final class RecordingPersistenceAdapter implements HermesLearnedSkillPersistenceAdapter {

        private final List<SkillDefinition> skills = new ArrayList<>();
        private SkillDefinition createdSkill;
        private SkillArtifact createdArtifact;

        @Override
        public String adapterId() {
            return "recording";
        }

        @Override
        public Uni<Optional<SkillDefinition>> find(String skillId) {
            return Uni.createFrom().item(skills.stream()
                    .filter(skill -> skill.id().equals(skillId))
                    .findFirst());
        }

        @Override
        public Uni<List<SkillDefinition>> listLearnedSkills() {
            return Uni.createFrom().item(List.copyOf(skills));
        }

        @Override
        public SkillValidation validate(SkillDefinition skill) {
            return SkillValidation.success();
        }

        @Override
        public Uni<SkillDefinition> create(SkillDefinition skill, SkillArtifact artifact) {
            createdSkill = skill;
            createdArtifact = artifact;
            skills.add(skill);
            return Uni.createFrom().item(skill);
        }

        @Override
        public Uni<SkillDefinition> update(String skillId, SkillDefinition skill, SkillArtifact artifact) {
            createdSkill = skill;
            createdArtifact = artifact;
            return Uni.createFrom().item(skill);
        }
    }
}
