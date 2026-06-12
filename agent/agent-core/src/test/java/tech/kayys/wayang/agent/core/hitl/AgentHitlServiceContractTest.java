package tech.kayys.wayang.agent.core.hitl;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.memory.impl.VectorAgentMemory;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentHitlServiceContractTest {

    @Test
    void requestDecisionSnapshotsRequestAndStoresPendingState() {
        RecordingMemoryService memoryService = new RecordingMemoryService();
        AgentHitlService service = service(memoryService);
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("amount", 5000);
        context.put("payload", nested);

        AgentHitlService.HitlRequest request = AgentHitlService.HitlRequest.builder()
                .requestId(" req-1 ")
                .agentId(" agent-a ")
                .taskId(" task-1 ")
                .action(" approve-expense ")
                .priority(" HIGH ")
                .context(context)
                .build();
        context.put("late", "ignored");
        nested.put("amount", 10);

        AgentHitlService.HitlDecision decision = service.requestDecision(request).await().indefinitely();

        assertEquals("req-1", decision.requestId());
        assertEquals("agent-a", decision.agentId());
        assertEquals("task-1", decision.taskId());
        assertEquals(AgentHitlService.HitlDecision.Status.PENDING, decision.status());
        assertTrue(decision.needsApproval());
        assertEquals(decision, service.getDecision(" req-1 ").await().indefinitely());

        List<AgentHitlService.HitlRequest> pending = service.getPendingRequests("agent-a").await().indefinitely();
        assertEquals(1, pending.size());
        assertSame(request, pending.get(0));
        @SuppressWarnings("unchecked")
        Map<String, Object> storedPayload = (Map<String, Object>) request.context().get("payload");
        assertEquals(5000, storedPayload.get("amount"));
        assertFalse(request.context().containsKey("late"));
        assertThrows(UnsupportedOperationException.class, () -> request.context().put("later", "nope"));
        assertThrows(UnsupportedOperationException.class, () -> storedPayload.put("later", "nope"));

        assertEquals(1, memoryService.vector.entries().size());
        MemoryEntry entry = memoryService.vector.entries().get(0).entry();
        assertEquals("hitl-request", entry.metadata().get("type"));
        assertEquals("agent-a", entry.metadata().get("agentId"));
    }

    @Test
    void submitDecisionUsesOriginalRequestAndStoresDecision() {
        RecordingMemoryService memoryService = new RecordingMemoryService();
        AgentHitlService service = service(memoryService);
        service.requestDecision(request("req-2", "agent-a", "task-2")).await().indefinitely();
        Map<String, Object> modifications = new LinkedHashMap<>();
        modifications.put("approvedLimit", 1000);

        AgentHitlService.HitlDecision decision = service.submitDecision(
                " req-2 ",
                AgentHitlService.HitlDecision.Status.APPROVED,
                " approved ",
                modifications)
                .await().indefinitely();
        modifications.put("approvedLimit", 1);

        assertEquals("agent-a", decision.agentId());
        assertEquals("task-2", decision.taskId());
        assertTrue(decision.isApproved());
        assertFalse(decision.needsApproval());
        assertEquals("approved", decision.feedback());
        assertEquals(1000, decision.modifications().get("approvedLimit"));
        assertThrows(UnsupportedOperationException.class, () -> decision.modifications().put("later", "nope"));
        assertEquals(decision, service.getDecision("req-2").await().indefinitely());
        assertTrue(service.getPendingRequests("agent-a").await().indefinitely().isEmpty());

        assertEquals(2, memoryService.vector.entries().size());
        assertEquals("hitl-decision", memoryService.vector.entries().get(1).entry().metadata().get("type"));
        assertEquals("APPROVED", memoryService.vector.entries().get(1).entry().metadata().get("status"));
    }

    @Test
    void failsWhenSubmittingOrEscalatingMissingRequest() {
        AgentHitlService service = new AgentHitlService();

        IllegalArgumentException submitError = assertThrows(IllegalArgumentException.class,
                () -> service.submitDecision(
                        "missing",
                        AgentHitlService.HitlDecision.Status.APPROVED,
                        "ok",
                        Map.of()).await().indefinitely());
        IllegalArgumentException escalationError = assertThrows(IllegalArgumentException.class,
                () -> service.escalate("missing", "needs review", "lead").await().indefinitely());

        assertTrue(submitError.getMessage().contains("HITL request not found"));
        assertTrue(escalationError.getMessage().contains("HITL request not found"));
    }

    @Test
    void escalationKeepsRequestPendingAndRecordsDecision() {
        AgentHitlService service = service(new RecordingMemoryService());
        service.requestDecision(request("req-3", "agent-a", "task-3")).await().indefinitely();

        AgentHitlService.EscalationResult escalation = service.escalate(
                "req-3",
                " needs lead ",
                " lead ")
                .await().indefinitely();

        assertEquals("req-3", escalation.requestId());
        assertEquals("needs lead", escalation.reason());
        assertEquals("lead", escalation.approver());

        AgentHitlService.HitlDecision decision = service.getDecision("req-3").await().indefinitely();
        assertEquals(AgentHitlService.HitlDecision.Status.ESCALATED, decision.status());
        assertTrue(decision.needsApproval());
        assertEquals("lead", decision.modifications().get("approver"));
        assertEquals(1, service.getPendingRequests("agent-a").await().indefinitely().size());
    }

    @Test
    void metricsSummarizeRequestsAndTerminalDecisions() {
        AgentHitlService service = new AgentHitlService();
        service.requestDecision(request("req-a", "agent-m", "task-a")).await().indefinitely();
        service.requestDecision(request("req-b", "agent-m", "task-b")).await().indefinitely();
        service.requestDecision(request("req-c", "agent-m", "task-c")).await().indefinitely();
        service.requestDecision(request("req-d", "agent-m", "task-d")).await().indefinitely();

        service.submitDecision("req-a", AgentHitlService.HitlDecision.Status.APPROVED, "ok", Map.of())
                .await().indefinitely();
        service.submitDecision("req-b", AgentHitlService.HitlDecision.Status.MODIFIED, "ok", Map.of("change", true))
                .await().indefinitely();
        service.submitDecision("req-c", AgentHitlService.HitlDecision.Status.REJECTED, "no", Map.of())
                .await().indefinitely();

        AgentHitlService.HitlMetrics metrics = service.getMetrics(" agent-m ").await().indefinitely();

        assertEquals("agent-m", metrics.agentId());
        assertEquals(4, metrics.totalRequests());
        assertEquals(2, metrics.approvedCount());
        assertEquals(1, metrics.rejectedCount());
        assertEquals(2.0 / 3.0, metrics.approvalRate(), 0.0001);
        assertTrue(metrics.avgDecisionTimeMs() >= 0);
    }

    @Test
    void worksWithoutInjectedMemoryService() {
        AgentHitlService service = new AgentHitlService();

        AgentHitlService.HitlDecision pending =
                service.requestDecision(request("req-4", "agent-z", "task-4")).await().indefinitely();
        AgentHitlService.HitlDecision approved = service.submitDecision(
                pending.requestId(),
                AgentHitlService.HitlDecision.Status.APPROVED,
                "ok",
                Map.of())
                .await().indefinitely();

        assertTrue(approved.isApproved());
        assertTrue(service.getPendingRequests("agent-z").await().indefinitely().isEmpty());
    }

    private static AgentHitlService service(RecordingMemoryService memoryService) {
        AgentHitlService service = new AgentHitlService();
        service.memoryService = memoryService;
        return service;
    }

    private static AgentHitlService.HitlRequest request(String requestId, String agentId, String taskId) {
        return AgentHitlService.HitlRequest.builder()
                .requestId(requestId)
                .agentId(agentId)
                .taskId(taskId)
                .action("review")
                .context(Map.of("created", Instant.parse("2026-01-02T03:04:05Z").toString()))
                .build();
    }

    private static final class RecordingMemoryService extends AgentMemoryService {
        private final RecordingVectorAgentMemory vector = new RecordingVectorAgentMemory();

        @Override
        public VectorAgentMemory vectorAgentMemory() {
            return vector;
        }
    }

    private static final class RecordingVectorAgentMemory extends VectorAgentMemory {
        private final List<StoredEntry> entries = new CopyOnWriteArrayList<>();

        @Override
        public Uni<Void> store(String agentId, MemoryEntry entry) {
            entries.add(new StoredEntry(agentId, entry));
            return Uni.createFrom().voidItem();
        }

        private List<StoredEntry> entries() {
            return List.copyOf(entries);
        }
    }

    private record StoredEntry(String agentId, MemoryEntry entry) {
    }
}
