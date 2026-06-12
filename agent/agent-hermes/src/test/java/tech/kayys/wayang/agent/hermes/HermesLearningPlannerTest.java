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
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningPlannerTest {

    @Test
    void plansCreateForEligibleNewSkill(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesLearningSignal signal = signal(
                "req-plan-1",
                "Generate nightly API backup report",
                List.of("rag", "terminal"));
        HermesLearningPlanner planner = planner(repository, HermesAgentModeConfig.defaults());

        HermesLearningPlan plan = planner.plan(signal).await().indefinitely();

        assertThat(plan.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(plan.persistsSkill()).isTrue();
        assertThat(plan.skill().id()).startsWith("hermes-generate-nightly-api-backup-report");
        assertThat(plan.lifecycleReport().completedStages())
                .containsExactly(
                        HermesLearningStageCatalog.SIGNAL_DETECTION,
                        HermesLearningStageCatalog.ELIGIBILITY_ASSESSMENT,
                        HermesLearningStageCatalog.SKILL_DISTILLATION,
                        HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                        HermesLearningStageCatalog.REUSE_MATCH);
        assertThat(plan.lifecycleReport().skippedStages()).isEmpty();
        assertThat(plan.skill().metadata())
                .containsEntry("hermes.revision", "1")
                .containsEntry("hermes.latestRequestId", "req-plan-1");
    }

    @Test
    void plansSkipWhenPolicyRejectsRun(@TempDir Path tempDir) {
        HermesLearningPlanner planner = planner(repository(tempDir), HermesAgentModeConfig.defaults());

        HermesLearningPlan plan = planner.plan(simpleSignal()).await().indefinitely();

        assertThat(plan.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(plan.persistsSkill()).isFalse();
        assertThat(plan.reason()).isEqualTo("run had 1 step(s), below learning threshold 3");
        assertThat(plan.lifecycleReport().completedStages())
                .containsExactly(
                        HermesLearningStageCatalog.SIGNAL_DETECTION,
                        HermesLearningStageCatalog.ELIGIBILITY_ASSESSMENT);
        assertThat(plan.lifecycleReport().skippedStages())
                .containsExactly(
                        HermesLearningStageCatalog.SKILL_DISTILLATION,
                        HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                        HermesLearningStageCatalog.REUSE_MATCH);
    }

    @Test
    void plansUpdateForExistingSkill(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesAgentModeConfig config = HermesAgentModeConfig.defaults();
        HermesSkillDistiller distiller = new HermesSkillDistiller();
        HermesLearningSignal signal = signal(
                "req-plan-existing",
                "Audit MCP server health",
                List.of("rag", "terminal"));
        SkillDefinition existing = distiller.distill(signal, config);
        repository.create(existing, signal).await().indefinitely();

        HermesLearningPlan plan = planner(repository, config).plan(signal).await().indefinitely();

        assertThat(plan.decision()).isEqualTo(HermesLearningDecision.UPDATED);
        assertThat(plan.skill().id()).isEqualTo(existing.id());
        assertThat(plan.lifecycleReport().completedStages())
                .contains(
                        HermesLearningStageCatalog.SKILL_DISTILLATION,
                        HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                        HermesLearningStageCatalog.REUSE_MATCH);
        assertThat(plan.skill().metadata())
                .containsEntry("hermes.revision", "2")
                .containsEntry("hermes.mergeReason", "skill already exists");
    }

    @Test
    void plansReusableSkillUpdateInsteadOfDuplicate(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesAgentModeConfig config = HermesAgentModeConfig.defaults();
        HermesSkillDistiller distiller = new HermesSkillDistiller();
        HermesLearningSignal first = signal(
                "req-plan-reuse-1",
                "Generate nightly API backup report",
                List.of("rag"));
        SkillDefinition existing = distiller.distill(first, config);
        repository.create(existing, first).await().indefinitely();
        HermesLearningSignal similar = signal(
                "req-plan-reuse-2",
                "Create API backup nightly report",
                List.of("rag"));

        HermesLearningPlan plan = planner(repository, config).plan(similar).await().indefinitely();

        assertThat(plan.decision()).isEqualTo(HermesLearningDecision.UPDATED);
        assertThat(plan.skill().id()).isEqualTo(existing.id());
        assertThat(plan.skill().metadata())
                .containsEntry("hermes.latestRequestId", "req-plan-reuse-2")
                .containsKey("hermes.derivedFromSkillId");
    }

    @Test
    void plansSkipWhenSelfImprovementIsDisabledForExistingSkill(@TempDir Path tempDir) {
        HermesLearnedSkillRepository repository = repository(tempDir);
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillSelfImprovementEnabled(false)
                .build();
        HermesLearningSignal signal = signal(
                "req-plan-skip-existing",
                "Archive multi tenant audit evidence",
                List.of("rag", "terminal"));
        SkillDefinition existing = new HermesSkillDistiller().distill(signal, config);
        repository.create(existing, signal).await().indefinitely();

        HermesLearningPlan plan = planner(repository, config).plan(signal).await().indefinitely();

        assertThat(plan.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(plan.reason()).isEqualTo("skill already exists");
        assertThat(plan.skillDefinition()).isEmpty();
    }

    private static HermesLearningPlanner planner(
            HermesLearnedSkillRepository repository,
            HermesAgentModeConfig config) {
        return new HermesLearningPlanner(
                repository,
                new HermesSkillDistiller(),
                config,
                new HermesSkillReusePolicy());
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

    private static HermesLearningSignal simpleSignal() {
        return new HermesLearningSignal(
                "req-plan-simple",
                "Say hello",
                "Hello",
                true,
                List.of(new AgentState.ReasoningStep(
                        1,
                        "Answer",
                        null,
                        "Answered",
                        1,
                        true)),
                List.of(),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
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
}
