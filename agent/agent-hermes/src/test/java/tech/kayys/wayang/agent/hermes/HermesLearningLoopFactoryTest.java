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
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningLoopFactoryTest {

    @Test
    void wiresConfiguredPromotionReceiptLedger(@TempDir Path tempDir) {
        Path receiptLedgerPath = tempDir.resolve("promotion-receipts.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "receipt-ledger-store", "file-system",
                        "receipt-ledger-path", receiptLedgerPath.toString(),
                        "receipt-ledger-max-records", "5"))
                .build();
        SkillManagementService service = service(tempDir);
        HermesLearnedSkillRepository learnedSkills = new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer(),
                config.skillPersistenceStrategy().routePlan().targetPlan());
        HermesLearningLoop loop = HermesLearningLoopFactory.create(
                learnedSkills,
                config,
                Optional.empty(),
                Optional.empty());

        HermesLearningResult result = loop.learn(
                        AgentRequest.builder()
                                .requestId("req-loop-factory")
                                .prompt("Generate weekly platform readiness report")
                                .build(),
                        response())
                .await()
                .indefinitely();

        String idempotencyKey = String.valueOf(result.metadataView()
                .promotionReceipt()
                .get("idempotencyKey"));
        HermesLearningPromotionReceiptLedger replayLedger = HermesLearningPromotionReceiptLedgerResolver.resolve(config);
        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(Files.exists(receiptLedgerPath)).isTrue();
        assertThat(replayLedger.find(idempotencyKey))
                .isPresent()
                .get()
                .extracting(HermesLearningPromotionReceipt::skillId)
                .isEqualTo(result.skillId());
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

    private static AgentResponse response() {
        List<AgentState.ReasoningStep> steps = java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(index -> new AgentState.ReasoningStep(
                        index,
                        "Inspect readiness signal " + index,
                        new AgentState.AgentAction(
                                "rag",
                                "retrieve readiness context",
                                Map.of("query", "readiness-" + index),
                                Instant.now()),
                        "Readiness observation " + index,
                        12,
                        true))
                .toList();
        return AgentResponse.builder()
                .runId("run-loop-factory")
                .requestId("req-loop-factory")
                .answer("Readiness report is ready")
                .steps(steps)
                .totalSteps(steps.size())
                .successful(true)
                .strategy("react")
                .durationMs(30)
                .build();
    }
}
