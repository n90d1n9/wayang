package tech.kayys.wayang.agent.hermes;

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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HermesLearningLoopTest {

    @Test
    void createsPortableSkillAndSkillMdArtifactAfterComplexSuccess(@TempDir Path tempDir) {
        InMemorySkillArtifactStore artifactStore = new InMemorySkillArtifactStore();
        SkillManagementService service = service(tempDir, artifactStore);
        HermesLearningLoop loop = new HermesLearningLoop(service);
        AgentRequest request = AgentRequest.builder()
                .requestId("req-1")
                .prompt("Generate nightly API backup report")
                .build();

        HermesLearningResult result = loop.learn(request, response(true, 3)).await().indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(result.skillId()).startsWith("hermes-generate-nightly-api-backup-report");
        SkillDefinition learned = service.getSkill(result.skillId()).await().indefinitely().orElseThrow();
        assertThat(learned.category()).isEqualTo(HermesAgentMode.LEARNED_SKILL_CATEGORY);
        assertThat(learned.metadata())
                .containsEntry("agentskills.compatible", "true")
                .containsEntry("hermes.requestId", "req-1")
                .containsEntry("hermes.revision", "1")
                .containsEntry("hermes.revisionStatus", "initial")
                .containsEntry("hermes.lineageRootSkillId", learned.id())
                .containsEntry("hermes.lineageDepth", "1")
                .containsEntry("hermes.createdRequestId", "req-1")
                .containsEntry("hermes.latestRequestId", "req-1")
                .containsEntry("hermes.sourceRequestIds", "req-1")
                .containsEntry("hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY)
                .containsEntry("hermes.learningQualityThreshold", "0.60")
                .containsKey("hermes.learningQualityScore")
                .containsKey("hermes.learningQualityReusePotential")
                .containsKey("hermes.learningQualityNoisePenalty");
        assertThat(learned.systemPrompt())
                .contains("## Procedure")
                .contains("Use `rag`")
                .contains("## Verification");
        assertThat(artifactStore.listArtifacts(SkillArtifactQuery.forSkill(result.skillId()))).hasSize(1);
        String markdown = new String(
                artifactStore.getArtifact(artifactStore.listArtifacts(result.skillId()).getFirst())
                        .orElseThrow()
                        .content(),
                StandardCharsets.UTF_8);
        assertThat(markdown).contains("name: " + result.skillId()).contains("# Generate nightly API backup report");
    }

    @Test
    void skipsFailedOrSimpleRunsWithPolicyReasons(@TempDir Path tempDir) {
        HermesLearningLoop loop = new HermesLearningLoop(service(tempDir, new InMemorySkillArtifactStore()));
        AgentRequest request = AgentRequest.builder()
                .requestId("req-2")
                .prompt("Say hello")
                .build();

        HermesLearningResult failed = loop.learn(request, response(false, 4)).await().indefinitely();
        HermesLearningResult simple = loop.learn(request, response(true, 1)).await().indefinitely();

        assertThat(failed.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(failed.reason()).isEqualTo("run was not successful");
        assertThat(simple.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(simple.reason()).isEqualTo("run had 1 step(s), below learning threshold 3");
    }

    @Test
    void updatesExistingSkillWhenSelfImprovementIsEnabled(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir, new InMemorySkillArtifactStore());
        HermesLearningLoop loop = new HermesLearningLoop(service);
        AgentRequest request = AgentRequest.builder()
                .requestId("req-3")
                .prompt("Audit MCP server health")
                .build();

        HermesLearningResult first = loop.learn(request, response(true, 3)).await().indefinitely();
        HermesLearningResult second = loop.learn(request, response(true, 4)).await().indefinitely();

        assertThat(first.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(second.decision()).isEqualTo(HermesLearningDecision.UPDATED);
        assertThat(service.getSkill(first.skillId()).await().indefinitely().orElseThrow().metadata())
                .containsEntry("hermes.revision", "2")
                .containsEntry("hermes.previousRevision", "1")
                .containsEntry("hermes.supersedesRevision", "1")
                .containsEntry("hermes.revisionStatus", "refined")
                .containsEntry("hermes.lineageRootSkillId", first.skillId())
                .containsEntry("hermes.lineageDepth", "2")
                .containsEntry("hermes.createdRequestId", "req-3")
                .containsEntry("hermes.latestRequestId", "req-3")
                .containsEntry("hermes.sourceRequestIds", "req-3")
                .containsEntry("hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)
                .containsEntry("hermes.mergeReason", "skill already exists")
                .containsEntry("hermes.supersedesSkillId", first.skillId())
                .containsEntry("hermes.lastCandidateSkillId", first.skillId());
    }

    @Test
    void reusesSimilarLearnedSkillInsteadOfCreatingDuplicate(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir, new InMemorySkillArtifactStore());
        HermesLearningLoop loop = new HermesLearningLoop(service);
        AgentRequest firstRequest = AgentRequest.builder()
                .requestId("req-reuse-1")
                .prompt("Generate nightly API backup report")
                .build();
        AgentRequest similarRequest = AgentRequest.builder()
                .requestId("req-reuse-2")
                .prompt("Create API backup nightly report")
                .build();

        HermesLearningResult first = loop.learn(firstRequest, response(true, 3)).await().indefinitely();
        HermesLearningResult second = loop.learn(similarRequest, response(true, 4)).await().indefinitely();

        assertThat(first.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(second.decision()).isEqualTo(HermesLearningDecision.UPDATED);
        assertThat(second.skillId()).isEqualTo(first.skillId());
        assertThat(service.listByCategory(HermesAgentMode.LEARNED_SKILL_CATEGORY).await().indefinitely())
                .hasSize(1);
        assertThat(service.getSkill(first.skillId()).await().indefinitely().orElseThrow().metadata())
                .containsEntry("hermes.revision", "2")
                .containsEntry("hermes.previousRevision", "1")
                .containsEntry("hermes.supersedesRevision", "1")
                .containsEntry("hermes.revisionStatus", "refined")
                .containsEntry("hermes.lineageRootSkillId", first.skillId())
                .containsEntry("hermes.lineageDepth", "2")
                .containsEntry("hermes.createdRequestId", "req-reuse-1")
                .containsEntry("hermes.latestRequestId", "req-reuse-2")
                .containsEntry("hermes.sourceRequestIds", "req-reuse-1,req-reuse-2")
                .containsEntry("hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)
                .containsEntry("hermes.supersedesSkillId", first.skillId())
                .containsEntry("hermes.task", "Create API backup nightly report")
                .containsEntry("hermes.requestId", "req-reuse-2")
                .containsKey("hermes.derivedFromSkillId")
                .containsKey("hermes.lastCandidateSkillId");
    }

    @Test
    void doesNotActivateLearnedSkillWhenSkillMdArtifactCannotBePersisted(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir, new FailingSkillArtifactStore());
        HermesLearningLoop loop = new HermesLearningLoop(service);
        AgentRequest request = AgentRequest.builder()
                .requestId("req-4")
                .prompt("Archive multi tenant audit evidence")
                .build();

        Throwable error = catchThrowable(() -> loop.learn(request, response(true, 3)).await().indefinitely());

        assertThat(error)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to put skill artifact consistently");
        assertThat(service.listSkills().await().indefinitely()).isEmpty();
    }

    private SkillManagementService service(Path tempDir, SkillArtifactStore artifactStore) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                artifactStore,
                SkillManagementEventSink.noop());
    }

    private AgentResponse response(boolean successful, int stepCount) {
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
                .runId("run-1")
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
}
