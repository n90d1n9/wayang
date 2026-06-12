package tech.kayys.wayang.agent.core.hitl;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, HitlRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, HitlDecision> decisions = new ConcurrentHashMap<>();
    private final Map<String, EscalationResult> escalations = new ConcurrentHashMap<>();

    /**
     * Request human decision on agent action
     *
     * @param request The HITL request with context
     * @return Reactive human decision
     */
    public Uni<HitlDecision> requestDecision(HitlRequest request) {
        Objects.requireNonNull(request, "request");
        requests.put(request.requestId(), request);

        HitlDecision pendingDecision = new HitlDecision(
                request.requestId(),
                request.agentId(),
                request.taskId(),
                HitlDecision.Status.PENDING,
                "Awaiting human review",
                Map.of(),
                request.createdAt(),
                null
        );
        decisions.put(request.requestId(), pendingDecision);

        LOG.info("Requesting HITL decision for agent {}, task {}",
                request.agentId(), request.taskId());

        return storeHitlRequest(request)
                .replaceWith(pendingDecision);
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

        return lookupRequest(requestId)
                .flatMap(request -> {
                    HitlDecision decision = new HitlDecision(
                            request.requestId(),
                            request.agentId(),
                            request.taskId(),
                            status,
                            feedback,
                            modifications,
                            request.createdAt(),
                            Instant.now()
                    );
                    decisions.put(request.requestId(), decision);
                    return storeHitlDecision(decision).replaceWith(decision);
                });
    }

    /**
     * Get decision for specific request
     *
     * @param requestId The request ID
     * @return Reactive decision if available
     */
    public Uni<HitlDecision> getDecision(String requestId) {
        return Uni.createFrom().item(() -> decisions.get(normalizeId(requestId)));
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
            return requests.values().stream()
                    .filter(request -> !hasText(agentId) || request.agentId().equals(agentId.trim()))
                    .filter(request -> {
                        HitlDecision decision = decisions.get(request.requestId());
                        return decision == null || decision.needsApproval();
                    })
                    .sorted(Comparator
                            .comparing(HitlRequest::createdAt)
                            .thenComparing(HitlRequest::requestId))
                    .toList();
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

        return lookupRequest(requestId)
                .flatMap(request -> {
                    EscalationResult escalation = new EscalationResult(
                            request.requestId(),
                            "escalated",
                            reason,
                            suggestedApprover,
                            Instant.now()
                    );
                    escalations.put(request.requestId(), escalation);

                    HitlDecision decision = new HitlDecision(
                            request.requestId(),
                            request.agentId(),
                            request.taskId(),
                            HitlDecision.Status.ESCALATED,
                            escalation.reason(),
                            Map.of("approver", escalation.approver()),
                            request.createdAt(),
                            escalation.escalatedAt()
                    );
                    decisions.put(request.requestId(), decision);
                    return storeHitlDecision(decision).replaceWith(escalation);
                });
    }

    /**
     * Get HITL metrics for agent
     *
     * @param agentId The agent ID
     * @return HITL statistics
     */
    public Uni<HitlMetrics> getMetrics(String agentId) {
        return Uni.createFrom().item(() -> {
            String normalizedAgentId = normalizeAgentId(agentId);
            List<HitlRequest> agentRequests = requests.values().stream()
                    .filter(request -> request.agentId().equals(normalizedAgentId))
                    .toList();
            List<HitlDecision> agentDecisions = decisions.values().stream()
                    .filter(decision -> decision.agentId().equals(normalizedAgentId))
                    .toList();
            List<HitlDecision> terminalDecisions = agentDecisions.stream()
                    .filter(decision -> !decision.needsApproval())
                    .toList();
            int approvedCount = (int) terminalDecisions.stream()
                    .filter(HitlDecision::isApproved)
                    .count();
            int rejectedCount = (int) terminalDecisions.stream()
                    .filter(decision -> decision.status() == HitlDecision.Status.REJECTED)
                    .count();
            long avgDecisionTimeMs = averageDecisionTimeMs(terminalDecisions);
            return new HitlMetrics(
                    normalizedAgentId,
                    agentRequests.size(),
                    approvedCount,
                    rejectedCount,
                    terminalDecisions.isEmpty() ? 0.0 : (double) approvedCount / terminalDecisions.size(),
                    avgDecisionTimeMs,
                    Instant.now()
            );
        });
    }

    // Helper methods

    private Uni<Void> storeHitlRequest(HitlRequest request) {
        if (!hasMemory()) {
            return Uni.createFrom().voidItem();
        }
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
                .onFailure().recoverWithItem((Void) null);
    }

    private Uni<Void> storeHitlDecision(HitlDecision decision) {
        if (!hasMemory()) {
            return Uni.createFrom().voidItem();
        }
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
                .onFailure().recoverWithItem((Void) null);
    }

    private Uni<HitlRequest> lookupRequest(String requestId) {
        return Uni.createFrom().item(() -> {
            String normalizedRequestId = normalizeId(requestId);
            if (!hasText(normalizedRequestId)) {
                throw new IllegalArgumentException("HITL request ID must not be blank");
            }
            HitlRequest request = requests.get(normalizedRequestId);
            if (request == null) {
                throw new IllegalArgumentException("HITL request not found: " + normalizedRequestId);
            }
            return request;
        });
    }

    private boolean hasMemory() {
        return memoryService != null && memoryService.vectorAgentMemory() != null;
    }

    private static long averageDecisionTimeMs(List<HitlDecision> terminalDecisions) {
        return Math.round(terminalDecisions.stream()
                .filter(decision -> decision.requestedAt() != null && decision.decidedAt() != null)
                .mapToLong(decision -> Math.max(0, Duration.between(
                        decision.requestedAt(),
                        decision.decidedAt()).toMillis()))
                .average()
                .orElse(0.0));
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeAgentId(String agentId) {
        return hasText(agentId) ? agentId.trim() : "unknown-agent";
    }

    private static String normalizeTaskId(String taskId) {
        return hasText(taskId) ? taskId.trim() : "unknown-task";
    }

    private static String normalizeAction(String action) {
        return hasText(action) ? action.trim() : "unknown-action";
    }

    private static String normalizePriority(String priority) {
        return hasText(priority) ? priority.trim() : "NORMAL";
    }

    private static String normalizeFeedback(String feedback) {
        return feedback == null ? "" : feedback.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (hasText(key) && value != null) {
                copied.put(key.trim(), snapshotValue(value));
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }

    private static Object snapshotValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copied = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (key != null && hasText(String.valueOf(key)) && item != null) {
                    copied.put(String.valueOf(key).trim(), snapshotValue(item));
                }
            });
            return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
        }
        if (value instanceof List<?> list) {
            return List.copyOf(list.stream()
                    .filter(Objects::nonNull)
                    .map(AgentHitlService::snapshotValue)
                    .toList());
        }
        return value;
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
            this.requestId = hasText(requestId) ? requestId.trim() : UUID.randomUUID().toString();
            this.agentId = normalizeAgentId(agentId);
            this.taskId = normalizeTaskId(taskId);
            this.action = normalizeAction(action);
            this.context = copyMap(context);
            this.priority = normalizePriority(priority);
            this.createdAt = createdAt == null ? Instant.now() : createdAt;
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

        public HitlDecision {
            requestId = hasText(requestId) ? requestId.trim() : "unknown-request";
            agentId = normalizeAgentId(agentId);
            taskId = normalizeTaskId(taskId);
            status = status == null ? Status.PENDING : status;
            feedback = normalizeFeedback(feedback);
            modifications = copyMap(modifications);
            requestedAt = requestedAt == null ? Instant.now() : requestedAt;
        }

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
        public EscalationResult {
            requestId = hasText(requestId) ? requestId.trim() : "unknown-request";
            status = hasText(status) ? status.trim() : "escalated";
            reason = normalizeFeedback(reason);
            approver = hasText(approver) ? approver.trim() : "unassigned";
            escalatedAt = escalatedAt == null ? Instant.now() : escalatedAt;
        }
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

        public HitlMetrics {
            agentId = normalizeAgentId(agentId);
            approvalRate = Math.max(0.0, Math.min(1.0, approvalRate));
            avgDecisionTimeMs = Math.max(0L, avgDecisionTimeMs);
            recordedAt = recordedAt == null ? Instant.now() : recordedAt;
        }

        public boolean isHighApprovalRate() {
            return approvalRate > 0.8;
        }

        public boolean isLowApprovalRate() {
            return approvalRate < 0.5;
        }
    }
}
