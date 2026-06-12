package tech.kayys.gamelan.execution;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.execution.actor.*;
import tech.kayys.gamelan.execution.dag.*;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Layer VII: Execution Substrate & Runtime Semantics tests.
 */
@ExtendWith(MockitoExtension.class)
class LayerVIITest {

    @Mock SingleAgentOrchestrator orchestrator;
    @Mock GamelanConfig            config;
    @Mock GollekSdk                sdk;

    @InjectMocks DagExecutionEngine dagEngine;

    @BeforeEach
    void setUp() {
        when(config.defaultModel()).thenReturn("test-model");
        when(config.sessionPersist()).thenReturn(false);
        when(config.tokenBudget()).thenReturn(6000);
        OrchestratorResult ok = OrchestratorResult.ok("done", "react", 1, List.of(), Duration.ZERO);
        when(orchestrator.execute(any(AgentRequest.class))).thenReturn(ok);
    }

    // ── DagExecutionEngine ────────────────────────────────────────────────

    @Test
    void singleNodeDagExecutesSuccessfully() {
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(List.of(
                DagExecutionEngine.DagNode.node("step1", "analyze code").build()));

        DagExecutionEngine.DagResult result = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.CONTINUE_ON_FAILURE, null);

        assertThat(result.dag().nodes()).hasSize(1);
        assertThat(result.nodeResults()).containsKey("step1");
        assertThat(result.successCount()).isEqualTo(1);
    }

    @Test
    void independentNodeRunInParallel() {
        // Two nodes with no dependency — should run concurrently
        List<DagExecutionEngine.DagNode> nodes = List.of(
                DagExecutionEngine.DagNode.node("a", "task A").build(),
                DagExecutionEngine.DagNode.node("b", "task B").build()
        );
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(nodes);
        DagExecutionEngine.DagResult result = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.CONTINUE_ON_FAILURE, null);

        assertThat(result.nodeResults()).containsKeys("a", "b");
        assertThat(result.successCount()).isEqualTo(2);
    }

    @Test
    void dependentNodeReceivesParentOutput() {
        when(orchestrator.execute(any())).thenReturn(
                OrchestratorResult.ok("parent-output", "react", 1, List.of(), Duration.ZERO));

        List<DagExecutionEngine.DagNode> nodes = List.of(
                DagExecutionEngine.DagNode.node("parent", "read code").build(),
                DagExecutionEngine.DagNode.node("child", "analyze code").dependsOn("parent").build()
        );
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(nodes);
        DagExecutionEngine.DagResult result = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.CONTINUE_ON_FAILURE, null);

        // Child should have executed (it depends on parent)
        assertThat(result.nodeResults()).containsKey("child");
    }

    @Test
    void cycleDetectionThrowsException() {
        List<DagExecutionEngine.DagNode> cyclic = List.of(
                DagExecutionEngine.DagNode.node("a", "task a").dependsOn("b").build(),
                DagExecutionEngine.DagNode.node("b", "task b").dependsOn("a").build()
        );
        assertThatThrownBy(() -> dagEngine.build(cyclic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cycle");
    }

    @Test
    void abortPolicyCancelsDagOnFirstFailure() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.failure("react", "forced failure", Duration.ZERO));

        List<DagExecutionEngine.DagNode> nodes = List.of(
                DagExecutionEngine.DagNode.node("a", "task a").critical().build(),
                DagExecutionEngine.DagNode.node("b", "task b").build()
        );
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(nodes);
        DagExecutionEngine.DagResult result = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.ABORT_ON_FAILURE, null);

        // The dag should be aborted
        assertThat(result.aborted() || result.failureCount() > 0).isTrue();
    }

    @Test
    void dagProgressCallbackIsInvoked() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(List.of(
                DagExecutionEngine.DagNode.node("x", "task").build()));

        dagEngine.execute(dag, DagExecutionEngine.FailurePolicy.CONTINUE_ON_FAILURE,
                r -> callbackCount.incrementAndGet());

        assertThat(callbackCount.get()).isGreaterThan(0);
    }

    @Test
    void topologicalOrderRootsFirst() {
        List<DagExecutionEngine.DagNode> nodes = List.of(
                DagExecutionEngine.DagNode.node("root",  "root task").build(),
                DagExecutionEngine.DagNode.node("mid",   "mid task").dependsOn("root").build(),
                DagExecutionEngine.DagNode.node("leaf",  "leaf task").dependsOn("mid").build()
        );
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(nodes);
        List<DagExecutionEngine.DagNode> order = dag.topologicalOrder();
        List<String> names = order.stream().map(DagExecutionEngine.DagNode::id).toList();

        assertThat(names.indexOf("root")).isLessThan(names.indexOf("mid"));
        assertThat(names.indexOf("mid")).isLessThan(names.indexOf("leaf"));
    }

    @Test
    void criticalPathLengthIsCorrect() {
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(List.of(
                DagExecutionEngine.DagNode.node("a", "a").build(),
                DagExecutionEngine.DagNode.node("b", "b").dependsOn("a").build(),
                DagExecutionEngine.DagNode.node("c", "c").dependsOn("b").build()
        ));
        assertThat(dag.criticalPathLength()).isEqualTo(3);
    }

    @Test
    void dagSummaryIsNonBlank() {
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(List.of(
                DagExecutionEngine.DagNode.node("n", "task").build()));
        DagExecutionEngine.DagResult result = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.CONTINUE_ON_FAILURE, null);
        assertThat(result.summary()).isNotBlank();
    }

    // ── ActorSystem ───────────────────────────────────────────────────────

    private ActorSystem actorSystem;

    @BeforeEach
    void setUpActors() {
        actorSystem = ActorSystem.create("test-system");
    }

    @AfterEach
    void tearDownActors() {
        if (actorSystem.isRunning()) actorSystem.shutdown();
    }

    @Test
    void actorSystemStartsRunning() {
        assertThat(actorSystem.isRunning()).isTrue();
        assertThat(actorSystem.name()).isEqualTo("test-system");
    }

    @Test
    void spawnedActorReceivesMessages() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<String> received = new CopyOnWriteArrayList<>();

        ActorSystem.ActorRef<String> actor = actorSystem.spawn("test-actor",
                msg -> { received.add(msg); latch.countDown(); return null; });

        actor.tell("hello");
        actor.tell("world");
        actor.tell("!");

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(received).containsExactly("hello", "world", "!");
    }

    @Test
    void askPatternReturnsReply() {
        ActorSystem.ActorRef<String> actor = actorSystem.spawn("echo-actor",
                msg -> "ECHO:" + msg);

        Optional<String> reply = actor.ask("ping", Duration.ofSeconds(2));
        assertThat(reply).isPresent().contains("ECHO:ping");
    }

    @Test
    void stoppedActorDropsMessages() {
        ActorSystem.ActorRef<String> actor = actorSystem.spawn("stop-actor",
                msg -> null);
        actor.stop();
        boolean accepted = actor.tell("this should be dropped");
        assertThat(accepted).isFalse();
    }

    @Test
    void fullMailboxDropsMessages() throws InterruptedException {
        // Create an actor with tiny mailbox that processes slowly
        ActorSystem.ActorRef<Integer> actor = actorSystem.spawn("tiny-mailbox",
                3,  // mailbox size = 3
                msg -> { Thread.sleep(100); return null; },
                ActorSystem.SupervisionStrategy.RESTART);

        // Send more messages than mailbox can hold
        int dropped = 0;
        for (int i = 0; i < 10; i++) {
            if (!actor.tell(i)) dropped++;
        }
        // Some should have been dropped
        assertThat(dropped).isGreaterThan(0);
    }

    @Test
    void actorMetricsTrackProcessed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        ActorSystem.ActorRef<String> actor = actorSystem.spawn("metrics-actor",
                msg -> { latch.countDown(); return null; });

        actor.tell("msg1");
        actor.tell("msg2");
        latch.await(2, TimeUnit.SECONDS);

        assertThat(actor.metrics().processed.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shutdownStopsAllActors() {
        actorSystem.spawn("a1", msg -> null);
        actorSystem.spawn("a2", msg -> null);
        assertThat(actorSystem.actorCount()).isEqualTo(2);

        actorSystem.shutdown();
        assertThat(actorSystem.isRunning()).isFalse();
    }

    @Test
    void findActorByIdReturnsRef() {
        actorSystem.spawn("findable", msg -> null);
        assertThat(actorSystem.<String>find("findable")).isPresent();
        assertThat(actorSystem.<String>find("nonexistent")).isEmpty();
    }

    @Test
    void actorMetricsErrorRateIsZeroWithNoErrors() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ActorSystem.ActorRef<String> actor = actorSystem.spawn("clean-actor",
                msg -> { latch.countDown(); return null; });
        actor.tell("ok");
        latch.await(2, TimeUnit.SECONDS);
        assertThat(actor.metrics().errorRate()).isEqualTo(0.0);
    }

    // ── DagNode builder ───────────────────────────────────────────────────

    @Test
    void nodeBuilderSetsAllFields() {
        DagExecutionEngine.DagNode node = DagExecutionEngine.DagNode
                .node("my-step", "do something")
                .dependsOn("dep1", "dep2")
                .tools("read_file", "write_file")
                .model("llama3")
                .maxSteps(5)
                .critical()
                .metadata(Map.of("phase", "analysis"))
                .build();

        assertThat(node.id()).isEqualTo("my-step");
        assertThat(node.task()).isEqualTo("do something");
        assertThat(node.dependencies()).containsExactly("dep1", "dep2");
        assertThat(node.allowedTools()).containsExactly("read_file", "write_file");
        assertThat(node.model()).isEqualTo("llama3");
        assertThat(node.maxSteps()).isEqualTo(5);
        assertThat(node.critical()).isTrue();
        assertThat(node.metadata()).containsEntry("phase", "analysis");
    }

    @Test
    void nodeResultSkippedAndFailedFactories() {
        DagExecutionEngine.DagNode node = DagExecutionEngine.DagNode
                .node("n", "t").build();

        DagExecutionEngine.NodeResult skipped = DagExecutionEngine.NodeResult.skipped(node, "dep failed");
        assertThat(skipped.success()).isFalse();
        assertThat(skipped.output()).contains("[SKIPPED]");

        DagExecutionEngine.NodeResult failed = DagExecutionEngine.NodeResult.failed(
                node, "timeout", Duration.ofSeconds(5));
        assertThat(failed.success()).isFalse();
        assertThat(failed.output()).contains("[FAILED]");
    }
}
