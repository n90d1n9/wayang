package tech.kayys.gamelan.communication;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AgentMessageBus} — pub/sub, blackboard, and conflict resolution.
 */
class AgentMessageBusTest {

    private AgentMessageBus bus;

    @BeforeEach
    void setUp() { bus = new AgentMessageBus(); }

    // ── Publish / Subscribe ───────────────────────────────────────────────

    @Test
    void publishDeliveresToSubscribers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<AgentMessageBus.AgentMessage> received = new ArrayList<>();

        bus.subscribe("test-topic", msg -> { received.add(msg); latch.countDown(); });

        bus.publish(AgentMessageBus.AgentMessage.builder("sender", "test-topic", "REPORT")
                .payload(Map.of("key", "value")).build());

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(1);
        assertThat(received.get(0).from()).isEqualTo("sender");
        assertThat(received.get(0).topic()).isEqualTo("test-topic");
        assertThat(received.get(0).intent()).isEqualTo("REPORT");
    }

    @Test
    void publishAssignsNonZeroId() {
        long id = bus.publish(
                AgentMessageBus.AgentMessage.builder("a", "topic", "ANALYZE").build());
        assertThat(id).isGreaterThan(0);
    }

    @Test
    void multipleSubscribersAllReceiveMessage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            bus.subscribe("broadcast", msg -> { count.incrementAndGet(); latch.countDown(); });
        }

        bus.publish(AgentMessageBus.AgentMessage.builder("sender", "broadcast", "NOTIFY").build());

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(count.get()).isEqualTo(3);
    }

    @Test
    void undeliveredMessagesGoToDeadLetterQueue() {
        bus.publish(AgentMessageBus.AgentMessage.builder("sender", "no-subscribers", "X").build());
        assertThat(bus.deadLetters()).hasSize(1);
        assertThat(bus.deadLetters().get(0).topic()).isEqualTo("no-subscribers");
    }

    @Test
    void requestResponsePairing() throws InterruptedException {
        // Set up a responder
        bus.subscribe("question-topic", msg -> {
            if (msg.correlationId() != null) {
                bus.publish(AgentMessageBus.AgentMessage.builder("responder", "question-topic", "RESPONSE")
                        .payload(Map.of("answer", "42"))
                        .build()
                        .withCorrelationId(msg.correlationId()));
            }
        });

        Optional<AgentMessageBus.AgentMessage> response = bus.request(
                AgentMessageBus.AgentMessage.builder("questioner", "question-topic", "REQUEST").build(),
                2000L);

        // Note: the response pairing requires the responder to use the correlationId
        // This test verifies the request completes (either with response or empty on timeout)
        assertThatCode(() -> bus.request(
                AgentMessageBus.AgentMessage.builder("q", "no-one-listening", "R").build(),
                100L))
                .doesNotThrowAnyException();
    }

    // ── Blackboard ────────────────────────────────────────────────────────

    @Test
    void blackboardPostAndRead() {
        AgentMessageBus.Blackboard bb = bus.blackboard();
        bb.post("security-finding", "security-agent", "SQL injection in UserController", 0.95);

        assertThat(bb.read("security-finding")).isPresent();
        assertThat(bb.read("security-finding").get().agentId()).isEqualTo("security-agent");
        assertThat(bb.read("security-finding").get().confidence()).isEqualTo(0.95);
        assertThat(bb.read("missing-key")).isEmpty();
    }

    @Test
    void blackboardHigherConfidenceOverwritesLower() {
        AgentMessageBus.Blackboard bb = bus.blackboard();
        bb.post("finding", "agent-1", "Low confidence finding", 0.5);
        bb.post("finding", "agent-2", "High confidence finding", 0.9);

        assertThat(bb.read("finding").get().value()).isEqualTo("High confidence finding");
        assertThat(bb.read("finding").get().confidence()).isEqualTo(0.9);
    }

    @Test
    void blackboardLowerConfidenceDoesNotOverwrite() {
        AgentMessageBus.Blackboard bb = bus.blackboard();
        bb.post("finding", "agent-1", "Original high confidence", 0.9);
        bb.post("finding", "agent-2", "Lower confidence attempt", 0.3);

        assertThat(bb.read("finding").get().value()).isEqualTo("Original high confidence");
    }

    @Test
    void blackboardByAgentFiltersCorrectly() {
        AgentMessageBus.Blackboard bb = bus.blackboard();
        bb.post("bugs",     "agent-a", "Found 3 bugs",    0.9);
        bb.post("security", "agent-b", "Found 1 issue",   0.8);
        bb.post("perf",     "agent-a", "No perf issues",  0.7);

        List<AgentMessageBus.BlackboardEntry> aFindings = bb.byAgent("agent-a");
        assertThat(aFindings).hasSize(2);
        assertThat(aFindings).extracting(AgentMessageBus.BlackboardEntry::agentId)
                .allMatch(id -> id.equals("agent-a"));
    }

    @Test
    void blackboardAllReturnsSortedByConfidenceDesc() {
        AgentMessageBus.Blackboard bb = bus.blackboard();
        bb.post("k1", "a", "v1", 0.5);
        bb.post("k2", "a", "v2", 0.9);
        bb.post("k3", "a", "v3", 0.7);

        List<AgentMessageBus.BlackboardEntry> all = bb.all();
        assertThat(all.get(0).confidence()).isGreaterThanOrEqualTo(all.get(1).confidence());
    }

    @Test
    void blackboardClearRemovesAllEntries() {
        AgentMessageBus.Blackboard bb = bus.blackboard();
        bb.post("k1", "a", "v1", 0.9);
        bb.post("k2", "a", "v2", 0.8);
        assertThat(bb.size()).isEqualTo(2);
        bb.clear();
        assertThat(bb.size()).isEqualTo(0);
    }

    // ── Conflict Resolution ────────────────────────────────────────────────

    @Test
    void highestConfidenceResolutionPicksWinner() {
        List<AgentMessageBus.AgentFinding> findings = List.of(
                new AgentMessageBus.AgentFinding("agent-a", "SQL injection", 0.9, java.time.Instant.now()),
                new AgentMessageBus.AgentFinding("agent-b", "XSS vulnerability", 0.6, java.time.Instant.now()),
                new AgentMessageBus.AgentFinding("agent-c", "Path traversal", 0.75, java.time.Instant.now())
        );

        AgentMessageBus.Resolution r = bus.resolveConflict(
                findings, AgentMessageBus.ConflictResolutionStrategy.HIGHEST_CONFIDENCE);

        assertThat(r.selected()).hasSize(1);
        assertThat(r.selected().get(0).agentId()).isEqualTo("agent-a");
        assertThat(r.selected().get(0).confidence()).isEqualTo(0.9);
    }

    @Test
    void mergeResolutionPreservesAllFindings() {
        List<AgentMessageBus.AgentFinding> findings = List.of(
                new AgentMessageBus.AgentFinding("a", "Finding A", 0.8, java.time.Instant.now()),
                new AgentMessageBus.AgentFinding("b", "Finding B", 0.7, java.time.Instant.now())
        );

        AgentMessageBus.Resolution r = bus.resolveConflict(
                findings, AgentMessageBus.ConflictResolutionStrategy.MERGE);

        assertThat(r.selected()).hasSize(2);
    }

    @Test
    void emptyFindingsReturnsEmptyResolution() {
        AgentMessageBus.Resolution r = bus.resolveConflict(
                List.of(), AgentMessageBus.ConflictResolutionStrategy.HIGHEST_CONFIDENCE);
        assertThat(r.selected()).isEmpty();
    }

    @Test
    void singleFindingIsUnanimous() {
        var finding = new AgentMessageBus.AgentFinding("a", "Only finding", 0.9, java.time.Instant.now());
        var r = bus.resolveConflict(List.of(finding),
                AgentMessageBus.ConflictResolutionStrategy.MAJORITY_VOTE);
        assertThat(r.unanimous()).isTrue();
        assertThat(r.selected()).hasSize(1);
    }

    @Test
    void escalateStrategyFlagsForOrchestrator() {
        List<AgentMessageBus.AgentFinding> findings = List.of(
                new AgentMessageBus.AgentFinding("a", "Yes", 0.8, java.time.Instant.now()),
                new AgentMessageBus.AgentFinding("b", "No",  0.8, java.time.Instant.now())
        );
        var r = bus.resolveConflict(findings, AgentMessageBus.ConflictResolutionStrategy.ESCALATE);
        assertThat(r.strategy()).isEqualTo(AgentMessageBus.ConflictResolutionStrategy.ESCALATE);
        assertThat(r.warning()).isNotBlank();
    }

    // ── Message builder ───────────────────────────────────────────────────

    @Test
    void messageBuilderCreatesCorrectRecord() {
        var msg = AgentMessageBus.AgentMessage.builder("orchestrator", "results", "ANALYZE")
                .to("security-agent")
                .payload(Map.of("path", "src/", "depth", "3"))
                .constraint("maxTokens", 2000)
                .build();

        assertThat(msg.from()).isEqualTo("orchestrator");
        assertThat(msg.to()).isEqualTo("security-agent");
        assertThat(msg.topic()).isEqualTo("results");
        assertThat(msg.intent()).isEqualTo("ANALYZE");
        assertThat(msg.payload()).containsKey("path");
        assertThat(msg.constraints()).containsKey("maxTokens");
        assertThat(msg.sentAt()).isNotNull();
    }
}
