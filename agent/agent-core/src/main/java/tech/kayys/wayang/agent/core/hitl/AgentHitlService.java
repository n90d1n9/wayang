package tech.kayys.wayang.agent.core.hitl;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.*;

/**
 * Human-in-the-Loop Service with Memory Integration
 *
 * Bridges HITL workflows with agent memory to provide:
 * - Decision routing (approve/reject/modify)
 * - Human feedback collection
 * - Workflow state persistence
 * - Decision history tracking
 * - Escalation management
 *
 * Execution Flow:
 * 1. Agent reaches decision point
 * 2. Creates HITL request with context
 * 3. Routes to human reviewer
 * 4. Human provides decision
 * 5. Decision stored in memory
 * 6. Workflow continues with decision
 *
 * Usage:
 * {@code
 * @Inject
 * AgentHitlService hitlService;
 *
 * // Request human review
 * HitlRequest request = HitlRequest.builder()
 *     .agentId("agent-123")
 *     .taskId("task-456")
 *     .action("approve_expense")
 *     .context(Map.of("amount", 5000.00))
 *     .build();
 *
 * HitlDecision decision = hitlService.requestDecision(request)
 *     .await().indefinitely();
 * }
 */
@ApplicationScoped
public class AgentHitlService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentHitlService.class);

    @Inject
    AgentMemoryService memoryService;

    /**
     * Request human decision on agent action
     *
     * @param request The HITL request with context
     * @return Reactive human decision
     */
    public Uni<HitlDecision> requestDecision(HitlRequest request) {
        LOG.info("Requesting HITL decision for agent {}, task {}", 
                request.agentId(), request.taskId());

        // Store request in memory for audit trail
        return storeHitlRequest(request)
                .flatMap(__ -> {
                    // In production: Route to human workflow (HITL system)
                    // For now: Return placeholder
                    return Uni.createFrom().item(new HitlDecision(
                            request.requestId(),
                            request.agentId(),
                            request.taskId(),
                            HitlDecision.Status.PENDING,
                            "Awaiting human review",
                            Map.of(),
                            Instant.now(),
                            null
                    ));
                });
    }

    /**
     * Submit human decision for pending request
     *
     * @param requestId The HITL request ID
     * @param status The decision (APPROVED, REJECTED, MODIFIED)
     * @param feedback Human feedback
     * @param modifications Any modifications to original action
     * @return Decision record
     */
    public Uni<HitlDecision> submitDecision(
            String requestId,
            HitlDecision.Status status,
            String feedback,
            Map<String, Object> modifications) {

        LOG.info("Submitting HITL decision for request {}: {}", requestId, status);

        return Uni.createFrom().item(() -> {
            HitlDecision decision = new HitlDecision(
                    requestId,
                    "agent-id",  // Would be from stored request
                    "task-id",   // Would be from stored request
                    status,
                    feedback,
                    modifications,
                    Instant.now(),
                    Instant.now()
            );

            // Store decision in memory
            storeHitlDecision(decision).await().indefinitely();
            return decision;
        });
    }

    /**
     * Get decision for specific request
     *
     * @param requestId The request ID
     * @return Reactive decision if available
     */
    public Uni<HitlDecision> getDecision(String requestId) {
        return Uni.createFrom().item(() -> {
            // In production: Query HITL system for decision
            LOG.debug("Retrieving decision for request {}", requestId);
            return null;  // Would be from HITL system
        });
    }

    /**
     * Get pending decisions for agent
     *
     * @param agentId The agent ID
     * @return Reactive list of pending decisions
     */
    public Uni<List<HitlRequest>> getPendingRequests(String agentId) {
        return Uni.createFrom().item(() -> {
            LOG.debug("Getting pending HITL requests for agent {}", agentId);
            return new ArrayList<>();  // Would query HITL system
        });
    }

    /**
     * Request escalation for complex decision
     *
     * @param requestId The request ID
     * @param reason Reason for escalation
     * @param suggestedApprover Suggested approver
     * @return Escalation result
     */
    public Uni<EscalationResult> escalate(
            String requestId,
            String reason,
            String suggestedApprover) {

        LOG.info("Escalating request {} to {}", requestId, suggestedApprover);

        return Uni.createFrom().item(new EscalationResult(
                requestId,
                "escalated",
                reason,
                suggestedApprover,
                Instant.now()
        ));
    }

    /**
     * Get HITL metrics for agent
     *
     * @param agentId The agent ID
     * @return HITL statistics
     */
    public Uni<HitlMetrics> getMetrics(String agentId) {
        return Uni.createFrom().item(() -> {
            // In production: Calculate from HITL system + memory
            return new HitlMetrics(
                    agentId,
                    0,
                    0,
                    0,
                    0.0,
                    0L,
                    Instant.now()
            );
        });
    }

    // Helper methods

    private Uni<Void> storeHitlRequest(HitlRequest request) {
        MemoryEntry entry = new MemoryEntry(
                request.requestId(),
                "HITL Request: " + request.action() + " | Context: " + request.context(),
                Instant.now(),
                Map.of(
                        "agentId", request.agentId(),
                        "taskId", request.taskId(),
                        "type", "hitl-request",
                        "action", request.action(),
                        "source", "agent-hitl-workflow"
                )
        );

        return memoryService.vectorAgentMemory()
                .store(request.agentId(), entry)
                .onFailure().recoverWithVoid();
    }

    private Uni<Void> storeHitlDecision(HitlDecision decision) {
        MemoryEntry entry = new MemoryEntry(
                decision.requestId(),
                "HITL Decision: " + decision.status() + " | Feedback: " + decision.feedback(),
                decision.decidedAt(),
                Map.of(
                        "agentId", decision.agentId(),
                        "taskId", decision.taskId(),
                        "type", "hitl-decision",
                        "status", decision.status().name(),
                        "source", "agent-hitl-workflow"
                )
        );

        return memoryService.vectorAgentMemory()
                .store(decision.agentId(), entry)
                .onFailure().recoverWithVoid();
    }

    /**
     * HITL Request record
     */
    public static class HitlRequest {
        private final String requestId;
        private final String agentId;
        private final String taskId;
        private final String action;
        private final Map<String, Object> context;
        private final String priority;
        private final Instant createdAt;

        public HitlRequest(String requestId, String agentId, String taskId,
                         String action, Map<String, Object> context,
                         String priority, Instant createdAt) {
            this.requestId = requestId;
            this.agentId = agentId;
            this.taskId = taskId;
            this.action = action;
            this.context = context;
            this.priority = priority;
            this.createdAt = createdAt;
        }

        public String requestId() { return requestId; }
        public String agentId() { return agentId; }
        public String taskId() { return taskId; }
        public String action() { return action; }
        public Map<String, Object> context() { return context; }
        public String priority() { return priority; }
        public Instant createdAt() { return createdAt; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String requestId = UUID.randomUUID().toString();
            private String agentId;
            private String taskId;
            private String action;
            private Map<String, Object> context = new HashMap<>();
            private String priority = "NORMAL";
            private Instant createdAt = Instant.now();

            public Builder requestId(String requestId) {
                this.requestId = requestId;
                return this;
            }

            public Builder agentId(String agentId) {
                this.agentId = agentId;
                return this;
            }

            public Builder taskId(String taskId) {
                this.taskId = taskId;
                return this;
            }

            public Builder action(String action) {
                this.action = action;
                return this;
            }

            public Builder context(Map<String, Object> context) {
                this.context = context;
                return this;
            }

            public Builder priority(String priority) {
                this.priority = priority;
                return this;
            }

            public HitlRequest build() {
                return new HitlRequest(requestId, agentId, taskId, action, 
                                      context, priority, createdAt);
            }
        }
    }

    /**
     * HITL Decision record
     */
    public record HitlDecision(
            String requestId,
            String agentId,
            String taskId,
            Status status,
            String feedback,
            Map<String, Object> modifications,
            Instant requestedAt,
            Instant decidedAt) {

        public enum Status {
            PENDING, APPROVED, REJECTED, MODIFIED, ESCALATED
        }

        public boolean isApproved() {
            return status == Status.APPROVED || status == Status.MODIFIED;
        }

        public boolean needsApproval() {
            return status == Status.PENDING || status == Status.ESCALATED;
        }
    }

    /**
     * Escalation Result record
     */
    public record EscalationResult(
            String requestId,
            String status,
            String reason,
            String approver,
            Instant escalatedAt) {
    }

    /**
     * HITL Metrics record
     */
    public record HitlMetrics(
            String agentId,
            int totalRequests,
            int approvedCount,
            int rejectedCount,
            double approvalRate,
            long avgDecisionTimeMs,
            Instant recordedAt) {

        public boolean isHighApprovalRate() {
            return approvalRate > 0.8;
        }

        public boolean isLowApprovalRate() {
            return approvalRate < 0.5;
        }
    }
}
