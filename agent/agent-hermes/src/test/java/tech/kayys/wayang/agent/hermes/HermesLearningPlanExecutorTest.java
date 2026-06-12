package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningPlanExecutorTest {

    @Test
    void executesCreatePlanThroughLearnedSkillRepository(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesLearningPlanExecutor executor = new HermesLearningPlanExecutor(repository);
        HermesLearningSignal signal = signal(
                "req-execute-create",
                "Generate nightly API backup report",
                List.of("rag"));
        SkillDefinition skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = executor.execute(HermesLearningPlan.create(skill), signal)
                .await()
                .indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(result.skillId()).isEqualTo(skill.id());
        Map<String, Object> promotion = result.metadataView().promotion();
        Map<String, Object> receipt = result.metadataView().promotionReceipt();
        assertThat(promotion)
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("planDecision", "created")
                .containsEntry("skillId", skill.id())
                .containsEntry("sourceRequestId", "req-execute-create")
                .containsEntry("revision", "1")
                .containsEntry("lineageRootSkillId", skill.id());
        assertThat((String) promotion.get("promotionId")).startsWith("hermes-learning-promotion-");
        assertThat((String) promotion.get("idempotencyKey")).startsWith("learning-promotion-");
        assertThat(receipt)
                .containsEntry("promotionId", promotion.get("promotionId"))
                .containsEntry("idempotencyKey", promotion.get("idempotencyKey"))
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_PERSISTED)
                .containsEntry("persisted", true)
                .containsEntry("adapterId", "skill-management")
                .containsEntry("targetSummary", "definitions=skill-management,artifacts=skill-management");
        assertThat(result.metadataView().skillIndexingReceipt())
                .containsEntry("status", HermesLearningIndexingReceipt.STATUS_SKIPPED)
                .containsEntry("outcome", HermesLearningIndexingReceipt.OUTCOME_SKIPPED)
                .containsEntry("indexed", false)
                .containsEntry("adapterId", "noop")
                .containsEntry("reason", "no learned-skill indexer configured");
        assertThat(result.metadataView().lifecycleReport().completedStages())
                .contains(
                        HermesLearningStageCatalog.PROMOTION_DECISION,
                        HermesLearningStageCatalog.SKILL_PERSISTENCE,
                        HermesLearningStageCatalog.PROMOTION_RECEIPT);
        assertThat(result.metadataView().lifecycleReport().skippedStages())
                .contains(HermesLearningStageCatalog.SKILL_INDEXING);
        assertThat(result.metadataView().lifecycle()).containsEntry(
                HermesLearningMetadataKeys.TERMINAL_STAGE,
                HermesLearningStageCatalog.SKILL_INDEXING);
        assertThat(repository.find(skill.id()).await().indefinitely()).isPresent();
    }

    @Test
    void executesConfiguredSkillIndexerAfterPersistence(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesLearnedSkillIndexer indexer = request -> Uni.createFrom().item(
                HermesLearningIndexingReceipt.indexed(
                        request,
                        "test-indexer",
                        Map.of("indexName", "hermes-learned-skills")));
        HermesLearningPlanExecutor executor = new HermesLearningPlanExecutor(repository, null, indexer);
        HermesLearningSignal signal = signal(
                "req-execute-index",
                "Generate nightly API backup report",
                List.of("rag"));
        SkillDefinition skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = executor.execute(HermesLearningPlan.create(skill), signal)
                .await()
                .indefinitely();

        assertThat(result.metadataView().skillIndexingReceipt())
                .containsEntry("status", HermesLearningIndexingReceipt.STATUS_INDEXED)
                .containsEntry("outcome", HermesLearningIndexingReceipt.OUTCOME_INDEXED)
                .containsEntry("indexed", true)
                .containsEntry("adapterId", "test-indexer");
        assertThat(result.metadataView().lifecycleReport().completedStages())
                .contains(HermesLearningStageCatalog.SKILL_INDEXING);
        assertThat(result.metadataView().lifecycleReport().skippedStages())
                .doesNotContain(HermesLearningStageCatalog.SKILL_INDEXING);
    }

    @Test
    void executesUpdatePlanThroughLearnedSkillRepository(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesLearningPlanExecutor executor = new HermesLearningPlanExecutor(repository);
        HermesSkillDistiller distiller = new HermesSkillDistiller();
        HermesLearningSignal initialSignal = signal(
                "req-execute-update-1",
                "Audit MCP server health",
                List.of("rag"));
        SkillDefinition existing = distiller.distill(initialSignal, HermesAgentModeConfig.defaults());
        repository.create(existing, initialSignal).await().indefinitely();
        HermesLearningSignal refinementSignal = signal(
                "req-execute-update-2",
                "Audit MCP server health",
                List.of("terminal"));
        SkillDefinition candidate = distiller.distill(refinementSignal, HermesAgentModeConfig.defaults());
        SkillDefinition refined = distiller.refine(existing, candidate, refinementSignal, "skill already exists");

        HermesLearningResult result = executor.execute(HermesLearningPlan.update(refined), refinementSignal)
                .await()
                .indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.UPDATED);
        assertThat(result.skillId()).isEqualTo(existing.id());
        Map<String, Object> promotion = result.metadataView().promotion();
        Map<String, Object> receipt = result.metadataView().promotionReceipt();
        assertThat(promotion)
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("planDecision", "updated")
                .containsEntry("sourceRequestId", "req-execute-update-2")
                .containsEntry("revision", "2")
                .containsEntry("lineageRootSkillId", existing.id());
        assertThat((String) promotion.get("promotionId")).startsWith("hermes-learning-promotion-");
        assertThat((String) promotion.get("idempotencyKey")).startsWith("learning-promotion-");
        assertThat(receipt)
                .containsEntry("promotionId", promotion.get("promotionId"))
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_PERSISTED)
                .containsEntry("persisted", true);
        assertThat(repository.find(existing.id()).await().indefinitely().orElseThrow().metadata())
                .containsEntry("hermes.revision", "2")
                .containsEntry("hermes.latestRequestId", "req-execute-update-2");
    }

    @Test
    void executesSkipPlanWithoutPersistingSkill(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesLearningPlanExecutor executor = new HermesLearningPlanExecutor(repository);

        HermesLearningResult result = executor.execute(
                        HermesLearningPlan.skipped("learning explicitly skipped"),
                        null)
                .await()
                .indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(result.reason()).isEqualTo("learning explicitly skipped");
        assertThat(result.metadataView().promotion())
                .containsEntry("status", HermesLearningPromotion.STATUS_SKIPPED)
                .containsEntry("planDecision", "skipped")
                .containsEntry("plannedPersistence", false);
        assertThat(result.metadataView().promotionReceipt())
                .containsEntry("status", HermesLearningPromotion.STATUS_SKIPPED)
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_SKIPPED)
                .containsEntry("persisted", false)
                .containsEntry("targetSummary", "definitions=skill-management,artifacts=skill-management");
        assertThat(result.metadataView().lifecycleReport().completedStages())
                .contains(
                        HermesLearningStageCatalog.PROMOTION_DECISION,
                        HermesLearningStageCatalog.PROMOTION_RECEIPT);
        assertThat(result.metadataView().lifecycleReport().skippedStages())
                .contains(
                        HermesLearningStageCatalog.SKILL_PERSISTENCE,
                        HermesLearningStageCatalog.SKILL_INDEXING);
        assertThat(repository.lineageCatalog().await().indefinitely().learnedSkillCount()).isZero();
    }

    @Test
    void rejectsInvalidPromotionBeforePersistence() {
        RejectingPersistenceAdapter adapter = new RejectingPersistenceAdapter();
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                adapter,
                new HermesSkillMarkdownRenderer());
        HermesLearningPlanExecutor executor = new HermesLearningPlanExecutor(repository);
        HermesLearningSignal signal = signal(
                "req-reject-promotion",
                "Generate nightly API backup report",
                List.of("rag"));
        SkillDefinition skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = executor.execute(HermesLearningPlan.create(skill), signal)
                .await()
                .indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(result.reason()).isEqualTo("learned skill promotion failed validation: invalid promoted skill");
        assertThat(adapter.created).isFalse();
        Map<String, Object> promotion = result.metadataView().promotion();
        Map<String, Object> receipt = result.metadataView().promotionReceipt();
        assertThat(promotion)
                .containsEntry("status", HermesLearningPromotion.STATUS_REJECTED)
                .containsEntry("planDecision", "created")
                .containsEntry("plannedPersistence", true)
                .containsEntry("persistsSkill", false)
                .containsEntry("skillId", skill.id())
                .containsEntry("sourceRequestId", "req-reject-promotion")
                .containsEntry("revision", "1");
        assertThat((String) promotion.get("promotionId")).startsWith("hermes-learning-promotion-");
        assertThat((String) promotion.get("idempotencyKey")).startsWith("learning-promotion-");
        assertThat(receipt)
                .containsEntry("promotionId", promotion.get("promotionId"))
                .containsEntry("status", HermesLearningPromotion.STATUS_REJECTED)
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_REJECTED)
                .containsEntry("persisted", false)
                .containsEntry("adapterId", "RejectingPersistenceAdapter")
                .containsEntry("targetSummary", "definitions=skill-management,artifacts=skill-management");
        assertThat(result.metadataView().lifecycleReport().completedStages())
                .contains(
                        HermesLearningStageCatalog.PROMOTION_DECISION,
                        HermesLearningStageCatalog.PROMOTION_RECEIPT);
        assertThat(result.metadataView().lifecycleReport().skippedStages())
                .contains(
                        HermesLearningStageCatalog.SKILL_PERSISTENCE,
                        HermesLearningStageCatalog.SKILL_INDEXING);
    }

    @Test
    void promotionIdentityIsStableForEquivalentPlans() {
        HermesLearningSignal signal = signal(
                "req-stable-promotion",
                "Generate nightly API backup report",
                List.of("rag"));
        SkillDefinition skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());
        HermesLearningPlan plan = HermesLearningPlan.create(skill);

        HermesLearningPromotion first = HermesLearningPromotion.approved(plan);
        HermesLearningPromotion second = HermesLearningPromotion.approved(plan);

        assertThat(first.identity()).isEqualTo(second.identity());
        assertThat(first.toMetadata())
                .containsEntry("promotionId", second.identity().promotionId())
                .containsEntry("idempotencyKey", second.identity().idempotencyKey())
                .containsEntry("sourceRequestId", "req-stable-promotion");
    }

    private static HermesLearnedSkillRepository repository(Path tempDir) {
        return new HermesLearnedSkillRepository(
                new SkillManagementService(
                        new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                        new SkillDefinitionStoreInspector(),
                        new InMemorySkillLifecycleStateStore(),
                        new SkillLifecycleStateStoreInspector(),
                        new InMemorySkillArtifactStore(),
                        SkillManagementEventSink.noop()),
                new HermesSkillMarkdownRenderer());
    }

    private static HermesLearningSignal signal(String requestId, String task, List<String> toolIds) {
        List<AgentState.ReasoningStep> steps = java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(index -> new AgentState.ReasoningStep(
                        index,
                        "Step " + index,
                        new AgentState.AgentAction(
                                toolIds.get(Math.min(index - 1, toolIds.size() - 1)),
                                "complete step " + index,
                                Map.of("query", "q" + index),
                                Instant.parse("2026-06-02T00:00:00Z")),
                        "Observation " + index,
                        10,
                        true))
                .toList();
        return new HermesLearningSignal(
                requestId,
                task,
                "Completed " + task,
                true,
                steps,
                toolIds,
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }

    private static final class RejectingPersistenceAdapter implements HermesLearnedSkillPersistenceAdapter {

        private boolean created;

        @Override
        public Uni<Optional<SkillDefinition>> find(String skillId) {
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<List<SkillDefinition>> listLearnedSkills() {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public SkillValidation validate(SkillDefinition skill) {
            return SkillValidation.error("invalid promoted skill");
        }

        @Override
        public Uni<SkillDefinition> create(SkillDefinition skill, SkillArtifact artifact) {
            created = true;
            return Uni.createFrom().item(skill);
        }

        @Override
        public Uni<SkillDefinition> update(String skillId, SkillDefinition skill, SkillArtifact artifact) {
            created = true;
            return Uni.createFrom().item(skill);
        }
    }
}
