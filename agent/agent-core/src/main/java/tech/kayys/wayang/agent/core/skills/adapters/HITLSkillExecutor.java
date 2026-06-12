package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HITL (Human-in-the-Loop) skill executor.
 * 
 * Wraps skill execution with human feedback collection, allowing users to
 * provide corrections, refinements, and approval for skill-based actions.
 */
public class HITLSkillExecutor {

    private final SkillContext context;
    private final List<FeedbackHandler> feedbackHandlers;
    private final Map<String, ApprovalRequest> approvalRequests;
    private final Map<String, FeedbackRequest> feedbackRequests;
    private final List<ApprovalRecord> approvalHistory;
    private final AtomicLong sequence;

    public HITLSkillExecutor(SkillContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.feedbackHandlers = new CopyOnWriteArrayList<>();
        this.approvalRequests = new ConcurrentHashMap<>();
        this.feedbackRequests = new ConcurrentHashMap<>();
        this.approvalHistory = new CopyOnWriteArrayList<>();
        this.sequence = new AtomicLong();
    }

    /**
     * Register a feedback handler (e.g., user approval, corrections).
     */
    public HITLSkillExecutor addFeedbackHandler(FeedbackHandler handler) {
        if (handler != null) {
            feedbackHandlers.add(handler);
        }
        return this;
    }

    /**
     * Initialize the in-process HITL boundary.
     */
    public Uni<HITLSkillExecutor> initialize() {
        return Uni.createFrom().item(this);
    }

    /**
     * Collect human feedback on skill execution.
     */
    public Uni<FeedbackResult> collectFeedback(SkillResult result) {
        if (feedbackHandlers.isEmpty()) {
            return Uni.createFrom().item(new FeedbackResult(true, Map.of()));
        }
        return Uni.join()
            .all(feedbackHandlers.stream()
                .map(handler -> safeHandle(handler, result))
                .toList())
            .andCollectFailures()
            .map(HITLSkillExecutor::mergeFeedback);
    }

    /**
     * Request human approval before skill execution.
     */
    public Uni<Boolean> requestApproval() {
        createApprovalRequest(context.skillId(), context.inputs());
        return Uni.createFrom().item(true);
    }

    /**
     * Request human refinement of skill input/context.
     */
    public Uni<Map<String, Object>> requestRefinement(Map<String, Object> input) {
        return Uni.createFrom().item(copyMap(input));
    }

    /**
     * Create a durable approval request for a planned skill execution.
     */
    public ApprovalRequest createApprovalRequest(String skillId, Map<String, Object> executionPlan) {
        ApprovalRequest request = new ApprovalRequest(
                nextId("apr"),
                normalizeSkillId(skillId),
                copyMap(executionPlan),
                Instant.now());
        approvalRequests.put(request.requestId(), request);
        return request;
    }

    /**
     * Record the human approval decision for a pending request.
     */
    public Uni<Map<String, Object>> executeWithApproval(
            String requestId,
            boolean approved,
            Map<String, Object> reviewerInput) {
        return Uni.createFrom().item(() -> {
            ApprovalRequest request = approvalRequests.get(normalizeId(requestId));
            if (request == null) {
                return Map.of(
                        SkillContextKeys.HITL_REQUEST_ID, normalizeId(requestId),
                        SkillContextKeys.HITL_APPROVED, false,
                        SkillContextKeys.HITL_ERROR, "Approval request not found");
            }
            ApprovalRecord record = new ApprovalRecord(
                    request.requestId(),
                    request.skillId(),
                    approved,
                    copyMap(reviewerInput),
                    Instant.now());
            approvalHistory.add(record);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put(SkillContextKeys.HITL_REQUEST_ID, request.requestId());
            response.put(SkillContextKeys.WIRE_SKILL_ID, request.skillId());
            response.put(SkillContextKeys.HITL_APPROVED, approved);
            response.put(SkillContextKeys.HITL_EXECUTION_ID, "exec-" + request.requestId());
            if (!record.reviewerInput().isEmpty()) {
                response.put(SkillContextKeys.HITL_REVIEWER_INPUT, record.reviewerInput());
            }
            return Map.copyOf(response);
        });
    }

    /**
     * Create a feedback request for a completed skill execution.
     */
    public FeedbackRequest collectUserFeedback(String skillId, Map<String, Object> executionResult) {
        FeedbackRequest request = new FeedbackRequest(
                nextId("fbk"),
                normalizeSkillId(skillId),
                copyMap(executionResult),
                Instant.now());
        feedbackRequests.put(request.feedbackId(), request);
        return request;
    }

    /**
     * Record user refinements against a feedback request.
     */
    public Uni<Map<String, Object>> executeWithUserRefinement(
            String feedbackId,
            Map<String, Object> refinements) {
        return Uni.createFrom().item(() -> {
            FeedbackRequest request = feedbackRequests.get(normalizeId(feedbackId));
            if (request == null) {
                return Map.of(
                        SkillContextKeys.HITL_FEEDBACK_ID, normalizeId(feedbackId),
                        SkillContextKeys.HITL_ERROR, "Feedback request not found");
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put(SkillContextKeys.HITL_FEEDBACK_ID, request.feedbackId());
            response.put(SkillContextKeys.WIRE_SKILL_ID, request.skillId());
            response.put(SkillContextKeys.HITL_ORIGINAL_RESULT, request.executionResult());
            response.put(SkillContextKeys.HITL_REFINED_RESULT, copyMap(refinements));
            return Map.copyOf(response);
        });
    }

    /**
     * Return immutable approval history for a skill.
     */
    public List<ApprovalRecord> getApprovalHistory(String skillId) {
        String normalizedSkillId = normalizeSkillId(skillId);
        return approvalHistory.stream()
                .filter(record -> normalizedSkillId.isBlank() || record.skillId().equals(normalizedSkillId))
                .toList();
    }

    private static FeedbackResult mergeFeedback(List<?> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return new FeedbackResult(true, Map.of());
        }
        boolean approved = true;
        Map<String, Object> corrections = new LinkedHashMap<>();
        for (Object feedback : feedbacks) {
            if (feedback instanceof FeedbackResult result) {
                approved = approved && result.approved();
                corrections.putAll(result.corrections());
            } else if (feedback instanceof Boolean result) {
                approved = approved && result;
            } else if (feedback instanceof Map<?, ?> map) {
                corrections.putAll(copyMap(map));
            } else {
                approved = false;
            }
        }
        return new FeedbackResult(approved, corrections);
    }

    private Uni<Object> safeHandle(FeedbackHandler handler, SkillResult result) {
        try {
            Uni<?> response = handler.handle(context, result);
            if (response == null) {
                return Uni.createFrom().item(Boolean.FALSE);
            }
            return response
                    .onItem().transform(item -> item == null ? Boolean.FALSE : item)
                    .onFailure().recoverWithItem(Boolean.FALSE)
                    .map(item -> (Object) item);
        } catch (RuntimeException error) {
            return Uni.createFrom().item(Boolean.FALSE);
        }
    }

    private String nextId(String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }

    private String normalizeSkillId(String skillId) {
        if (hasText(skillId)) {
            return skillId.trim();
        }
        return SkillContextKeys.normalizedSkillId(context.skillId());
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }

    private static Map<String, Object> copyMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && hasText(String.valueOf(key)) && value != null) {
                copied.put(String.valueOf(key).trim(), snapshotValue(value));
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }

    private static Object snapshotValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            return List.copyOf(list.stream()
                    .filter(Objects::nonNull)
                    .map(HITLSkillExecutor::snapshotValue)
                    .toList());
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Feedback result from human.
     */
    public record FeedbackResult(
        boolean approved,
        Map<String, Object> corrections
    ) {
        public FeedbackResult {
            corrections = copyMap(corrections);
        }
    }

    /**
     * Human approval request for a planned skill execution.
     */
    public record ApprovalRequest(
            String requestId,
            String skillId,
            Map<String, Object> executionPlan,
            Instant requestedAt) {
        public ApprovalRequest {
            requestId = normalizeId(requestId);
            skillId = SkillContextKeys.normalizedSkillId(skillId);
            executionPlan = copyMap(executionPlan);
            requestedAt = requestedAt == null ? Instant.now() : requestedAt;
        }
    }

    /**
     * Human approval decision history.
     */
    public record ApprovalRecord(
            String requestId,
            String skillId,
            boolean approved,
            Map<String, Object> reviewerInput,
            Instant decidedAt) {
        public ApprovalRecord {
            requestId = normalizeId(requestId);
            skillId = SkillContextKeys.normalizedSkillId(skillId);
            reviewerInput = copyMap(reviewerInput);
            decidedAt = decidedAt == null ? Instant.now() : decidedAt;
        }
    }

    /**
     * Human feedback request for a completed skill execution.
     */
    public record FeedbackRequest(
            String feedbackId,
            String skillId,
            Map<String, Object> executionResult,
            Instant requestedAt) {
        public FeedbackRequest {
            feedbackId = normalizeId(feedbackId);
            skillId = SkillContextKeys.normalizedSkillId(skillId);
            executionResult = copyMap(executionResult);
            requestedAt = requestedAt == null ? Instant.now() : requestedAt;
        }
    }

    /**
     * Feedback handler interface.
     */
    public interface FeedbackHandler {
        Uni<?> handle(SkillContext context, SkillResult result);
    }
}
