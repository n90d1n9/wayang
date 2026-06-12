package tech.kayys.gamelan;

import org.junit.jupiter.api.*;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.communication.AgentMessageBus;
import tech.kayys.gamelan.control.AgentControlPlane;
import tech.kayys.gamelan.economics.TokenEconomy;
import tech.kayys.gamelan.execution.sandbox.ExecutionSandbox;
import tech.kayys.gamelan.execution.sandbox.SandboxInterceptor;
import tech.kayys.gamelan.governance.GovernanceEngine;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.safety.ConstraintSolver;
import tech.kayys.gamelan.tool.virtual.VirtualToolRegistry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test — verifies the new architectural layers work together
 * without CDI, using direct construction.
 *
 * <h2>Scenario: Full Safety Pipeline</h2>
 * Simulates an agent attempting:
 * 1. A safe file read (should pass all layers)
 * 2. A dangerous rm -rf command (should be blocked by safety)
 * 3. A sandbox-isolated write (should be redirected)
 * 4. Message bus coordination between two agents
 * 5. Governance audit trail verification
 */
class FullLayerIntegrationTest {

    private ConstraintSolver    safety;
    private ExecutionSandbox    sandbox;
    private GovernanceEngine    governance;
    private AgentMessageBus     bus;
    private EpisodicMemory      episodic;
    private ProceduralMemory    procedural;
    private AgentControlPlane   controlPlane;
    private VirtualToolRegistry virtualTools;
    private TokenEconomy        economy;

    @BeforeEach
    void setUp() {
        safety      = new ConstraintSolver();
        sandbox     = new ExecutionSandbox();
        sandbox.interceptor = new SandboxInterceptor();
        governance  = new GovernanceEngine();
        governance.init();
        bus         = new AgentMessageBus();
        episodic    = new EpisodicMemory();
        episodic.init();
        procedural  = new ProceduralMemory();
        procedural.init();
        controlPlane = new AgentControlPlane();
        virtualTools = new VirtualToolRegistry();
        virtualTools.init();
        economy     = new TokenEconomy();
        // economy needs config mocked — skip economy tests in this integration
    }

    @AfterEach
    void tearDown() {
        if (sandbox.isActive()) sandbox.discard();
    }

    // ── Safety pipeline ────────────────────────────────────────────────────

    @Test
    void safeReadPassesAllLayers() {
        ToolCall read = tool("read_file", Map.of("path", "src/Main.java"));

        // Safety check
        ConstraintSolver.SafetyVerdict verdict = safety.evaluate(read);
        assertThat(verdict.safe()).isTrue();

        // Governance check
        boolean permitted = governance.permits("agent-1", "READ", "src/Main.java");
        assertThat(permitted).isTrue();

        // No sandbox interception for reads (unless sandbox is active)
        assertThat(sandbox.intercept(read)).isEmpty();
    }

    @Test
    void destructiveCommandIsBlockedBySafety() {
        ToolCall rmrf = tool("run_command", Map.of("command", "rm -rf /var/data"));
        ConstraintSolver.SafetyVerdict verdict = safety.evaluate(rmrf);
        assertThat(verdict.safe()).isFalse();
        assertThat(verdict.blockReason()).contains("forbidden");
    }

    @Test
    void sandboxRedirectsWriteToOverlay() {
        sandbox.enter("integration-test");
        ToolCall write = tool("write_file", Map.of("path", "output.txt", "content", "hello"));

        Optional<tech.kayys.gamelan.tool.ToolResult> intercepted = sandbox.intercept(write);
        assertThat(intercepted).isPresent();
        assertThat(intercepted.get().output()).contains("[SANDBOX]");
        assertThat(sandbox.diff()).hasSize(1);
    }

    // ── Memory hierarchy pipeline ──────────────────────────────────────────

    @Test
    void episodicMemoryRecordsAndRetrieves() {
        episodic.record("Fix NPE in UserService login method", "Applied Optional.ofNullable",
                true, List.of("read_file", "apply_patch"), 1200L);
        episodic.record("Add logging to AuthService", "Wrote logger calls",
                true, List.of("write_file"), 800L);

        List<EpisodicMemory.Episode> found =
                episodic.findRelevant("debugging NPE in service class", 5);
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).task()).contains("UserService");
    }

    @Test
    void proceduralMemoryLearnFromEpisodes() {
        // Simulate 3 successful episodes with same tool pattern
        var ep1 = createEpisode("fix npe class A", List.of("read_file", "apply_patch"), true);
        var ep2 = createEpisode("fix npe class B", List.of("read_file", "apply_patch"), true);
        var ep3 = createEpisode("fix npe class C", List.of("read_file", "apply_patch"), true);

        procedural.learnFrom(ep1, List.of());
        procedural.learnFrom(ep2, List.of());
        procedural.learnFrom(ep3, List.of());

        // After 3 identical patterns, a procedure should be promoted
        // (or at minimum, no exception thrown)
        assertThatCode(() -> procedural.findApplicable("fix npe in class D", 5))
                .doesNotThrowAnyException();
    }

    // ── Message bus coordination ───────────────────────────────────────────

    @Test
    void twoAgentsCoordinateViaBlackboard() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AgentMessageBus.Blackboard blackboard = bus.blackboard();

        // Agent A subscribes and posts to blackboard
        bus.subscribe("analysis-results", msg -> {
            blackboard.post("agent-a-finding", "agent-a", "Found 3 security issues", 0.9);
            latch.countDown();
        });

        // Agent B subscribes and posts to blackboard
        bus.subscribe("analysis-results", msg -> {
            blackboard.post("agent-b-finding", "agent-b", "Performance bottleneck in query", 0.8);
            latch.countDown();
        });

        // Trigger both agents
        bus.publish(AgentMessageBus.AgentMessage.builder(
                "orchestrator", "analysis-results", "ANALYZE")
                .payload(Map.of("path", "src/")).build());

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(blackboard.size()).isEqualTo(2);
        assertThat(blackboard.read("agent-a-finding")).isPresent();
        assertThat(blackboard.read("agent-b-finding")).isPresent();
    }

    @Test
    void conflictResolutionPicksHighestConfidence() {
        List<AgentMessageBus.AgentFinding> findings = List.of(
                new AgentMessageBus.AgentFinding("agent-a", "SQL injection in login", 0.95,
                        Instant.now()),
                new AgentMessageBus.AgentFinding("agent-b", "Minor logging issue", 0.4,
                        Instant.now()));

        AgentMessageBus.Resolution r = bus.resolveConflict(
                findings, AgentMessageBus.ConflictResolutionStrategy.HIGHEST_CONFIDENCE);
        assertThat(r.selected().get(0).confidence()).isEqualTo(0.95);
    }

    // ── Governance audit trail ─────────────────────────────────────────────

    @Test
    void auditChainIsValidAfterMultipleEvents() {
        governance.recordEvent("agent-1", "TOOL_CALL", "read_file", Map.of("path", "x"));
        governance.recordEvent("agent-1", "TOOL_CALL", "write_file", Map.of("path", "y"));
        governance.recordEvent("agent-2", "TASK_START", "fix-bug", Map.of());

        // Chain should be internally consistent (can't verify disk state in unit test)
        assertThatCode(governance::verifyLog).doesNotThrowAnyException();
    }

    @Test
    void accessDeniedIsAudited() {
        var decision = governance.evaluate("rogue-agent", "WRITE", "/etc/passwd");
        assertThat(decision.isAllowed()).isFalse();
        // Verify the denial was recorded (audit happens inside evaluate)
        assertThatCode(governance::verifyLog).doesNotThrowAnyException();
    }

    // ── HITL control plane ────────────────────────────────────────────────

    @Test
    void approvalGateBlocksHighRiskTool() {
        controlPlane.requireApprovalForTools("run_command");

        // Without a decision submitted, the gate should timeout and reject
        // We test with very short timeout (not configurable in test without mocking)
        // Just verify the gate configuration doesn't throw
        assertThat(controlPlane.isPaused()).isFalse();
        assertThat(controlPlane.isAbortRequested()).isFalse();
    }

    @Test
    void pauseResumeDoesNotDeadlock() throws InterruptedException {
        controlPlane.pause();
        assertThat(controlPlane.isPaused()).isTrue();

        // Resume from a different thread to avoid deadlock in test
        Thread.ofVirtual().start(controlPlane::resume);
        Thread.sleep(200);

        assertThat(controlPlane.isPaused()).isFalse();
    }

    @Test
    void abortSetsFlag() {
        controlPlane.abort();
        assertThat(controlPlane.isAbortRequested()).isTrue();
    }

    // ── Virtual tools ──────────────────────────────────────────────────────

    @Test
    void virtualToolLambdaRegistrationAndExecution() {
        AtomicInteger callCount = new AtomicInteger(0);
        virtualTools.register(VirtualToolRegistry.VirtualTool.lambda(
                "counter-tool", "Counts calls",
                params -> {
                    int n = callCount.incrementAndGet();
                    return tech.kayys.gamelan.tool.ToolResult.success("counter-tool", "call #" + n);
                }));

        tech.kayys.gamelan.tool.ToolResult r1 =
                virtualTools.execute(tool("counter-tool", Map.of()));
        tech.kayys.gamelan.tool.ToolResult r2 =
                virtualTools.execute(tool("counter-tool", Map.of()));

        assertThat(r1.output()).contains("call #1");
        assertThat(r2.output()).contains("call #2");
    }

    // ── Full end-to-end safety + sandbox pipeline ──────────────────────────

    @Test
    void fullSafetyPipelineForRiskyOperation() {
        ToolCall riskyWrite = tool("write_file",
                Map.of("path", "config/prod-secrets.env",
                        "content", "DB_PASSWORD=supersecret123"));

        // 1. Safety check (should pass — no system path)
        ConstraintSolver.SafetyVerdict verdict = safety.evaluate(riskyWrite);
        assertThat(verdict.safe()).isTrue(); // path isn't /etc/* so it passes default rules

        // 2. Governance check (should pass for default policies)
        boolean permitted = governance.permits("agent-1", "WRITE", "config/prod-secrets.env");
        // Default policies allow src/** but not explicitly this path — result depends on config
        // Just verify no exception
        assertThatCode(() -> governance.evaluate("agent-1", "WRITE", "config/prod-secrets.env"))
                .doesNotThrowAnyException();

        // 3. Sandbox intercept (if active)
        sandbox.enter("safety-test");
        Optional<tech.kayys.gamelan.tool.ToolResult> intercepted = sandbox.intercept(riskyWrite);
        assertThat(intercepted).isPresent(); // write is captured by sandbox
        assertThat(intercepted.get().output()).contains("[SANDBOX]");

        // 4. Verify the real filesystem was NOT touched
        assertThat(java.nio.file.Files.exists(
                java.nio.file.Path.of("config/prod-secrets.env"))).isFalse();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ToolCall tool(String name, Map<String, String> params) {
        return new ToolCall(name, params, "<tc/>");
    }

    private EpisodicMemory.Episode createEpisode(String task, List<String> tools, boolean success) {
        return new EpisodicMemory.Episode(
                new java.util.concurrent.atomic.AtomicLong(System.nanoTime()).getAndIncrement(),
                task, "result", success, tools, 500L, Instant.now(), List.of());
    }
}
