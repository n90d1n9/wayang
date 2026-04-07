package tech.kayys.wayang.hitl.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * HumanTask - Aggregate root for human task management
 *
 * Manages the complete lifecycle of human tasks including:
 * - Task assignment and delegation
 * - Approval/rejection workflows
 * - Escalation handling
 * - Timeout management
 * - Audit trail
 */
public class HumanTask {

    // Identity
    private final HumanTaskId id;
    private final String workflowRunId;
    private final String nodeId;
    private final String tenantId;

    // Task details
    private final String taskType;
    private final String title;
    private final String description;
    private final int priority; // 1-5, 5 being highest
    private final Map<String, Object> context;
    private final Map<String, Object> formData;

    // Assignment
    private TaskAssignment currentAssignment;
    private final List<TaskAssignment> assignmentHistory;

    // Lifecycle
    private HumanTaskStatus status;
    private final Instant createdAt;
    private Instant claimedAt;
    private Instant completedAt;
    private Instant dueDate;

    // Completion
    private TaskOutcome outcome;
    private String completedBy;
    private Map<String, Object> completionData;
    private String comments;

    // Escalation
    private EscalationState escalationState;

    // Audit
    private final List<TaskAuditEntry> auditTrail;

    // Events
    private final List<HumanTaskEvent> uncommittedEvents;

    private HumanTask(Builder builder) {
        this.id = HumanTaskId.generate();
        this.workflowRunId = builder.workflowRunId;
        this.nodeId = builder.nodeId;
        this.tenantId = builder.tenantId;

        this.taskType = builder.taskType;
        this.title = builder.title;
        this.description = builder.description;
        this.priority = builder.priority;
        this.context = new HashMap<>(builder.context);
        this.formData = new HashMap<>(builder.formData);

        this.assignmentHistory = new ArrayList<>();
        this.auditTrail = new ArrayList<>();
        this.uncommittedEvents = new ArrayList<>();

        this.status = HumanTaskStatus.CREATED;
        this.createdAt = Instant.now();
        this.dueDate = builder.dueDate;

        // Initial assignment
        this.currentAssignment = builder.initialAssignment;
        if (this.currentAssignment != null) {
            this.assignmentHistory.add(this.currentAssignment);
            this.status = HumanTaskStatus.ASSIGNED;
            raiseEvent(new TaskAssignedEvent(id, currentAssignment, Instant.now()));
        }

        addAuditEntry("CREATED", "Task created", null);
        raiseEvent(new TaskCreatedEvent(id, workflowRunId, nodeId, Instant.now()));
    }

    // ==================== COMMAND HANDLERS ====================

    /**
     * Assign task to user or group
     */
    public void assign(TaskAssignment assignment) {
        validateNotTerminal();

        this.currentAssignment = assignment;
        this.assignmentHistory.add(assignment);
        this.status = HumanTaskStatus.ASSIGNED;

        addAuditEntry("ASSIGNED",
            "Task assigned to " + assignment.getAssigneeIdentifier(),
            assignment.getAssignedBy());

        raiseEvent(new TaskAssignedEvent(id, assignment, Instant.now()));
    }

    /**
     * Claim task (user takes ownership)
     */
    public void claim(String userId) {
        if (status != HumanTaskStatus.ASSIGNED) {
            throw new IllegalStateException(
                "Can only claim tasks in ASSIGNED status, current: " + status);
        }

        // Validate user has permission
        if (!currentAssignment.canClaim(userId)) {
            throw new SecurityException(
                "User " + userId + " cannot claim this task");
        }

        this.status = HumanTaskStatus.IN_PROGRESS;
        this.claimedAt = Instant.now();

        addAuditEntry("CLAIMED", "Task claimed", userId);
        raiseEvent(new TaskClaimedEvent(id, userId, Instant.now()));
    }

    /**
     * Delegate task to another user
     */
    public void delegate(String fromUserId, String toUserId, String reason) {
        validateNotTerminal();

        if (!currentAssignment.getAssigneeIdentifier().equals(fromUserId)) {
            throw new SecurityException("Only assignee can delegate task");
        }

        TaskAssignment newAssignment = TaskAssignment.builder()
            .assigneeType(AssigneeType.USER)
            .assigneeIdentifier(toUserId)
            .assignedBy(fromUserId)
            .assignedAt(Instant.now())
            .delegationReason(reason)
            .build();

        this.currentAssignment = newAssignment;
        this.assignmentHistory.add(newAssignment);
        this.status = HumanTaskStatus.ASSIGNED;

        addAuditEntry("DELEGATED",
            "Task delegated to " + toUserId + ": " + reason,
            fromUserId);

        raiseEvent(new TaskDelegatedEvent(id, fromUserId, toUserId, reason, Instant.now()));
    }

    /**
     * Release task back to pool
     */
    public void release(String userId) {
        if (status != HumanTaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only release IN_PROGRESS tasks");
        }

        this.status = HumanTaskStatus.ASSIGNED;
        this.claimedAt = null;

        addAuditEntry("RELEASED", "Task released back to pool", userId);
        raiseEvent(new TaskReleasedEvent(id, userId, Instant.now()));
    }

    /**
     * Complete task with approval
     */
    public void approve(String userId, Map<String, Object> data, String comments) {
        validateCanComplete(userId);

        this.status = HumanTaskStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.completedBy = userId;
        this.outcome = TaskOutcome.APPROVED;
        this.completionData = new HashMap<>(data);
        this.comments = comments;

        addAuditEntry("APPROVED", "Task approved: " + comments, userId);
        raiseEvent(new TaskApprovedEvent(id, userId, data, comments, Instant.now()));
    }

    /**
     * Complete task with rejection
     */
    public void reject(String userId, String reason, Map<String, Object> data) {
        validateCanComplete(userId);

        this.status = HumanTaskStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.completedBy = userId;
        this.outcome = TaskOutcome.REJECTED;
        this.completionData = new HashMap<>(data);
        this.comments = reason;

        addAuditEntry("REJECTED", "Task rejected: " + reason, userId);
        raiseEvent(new TaskRejectedEvent(id, userId, reason, data, Instant.now()));
    }

    /**
     * Complete task with custom outcome
     */
    public void complete(String userId, TaskOutcome outcome,
                        Map<String, Object> data, String comments) {
        validateCanComplete(userId);

        this.status = HumanTaskStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.completedBy = userId;
        this.outcome = outcome;
        this.completionData = new HashMap<>(data);
        this.comments = comments;

        addAuditEntry("COMPLETED",
            "Task completed with outcome: " + outcome + " - " + comments,
            userId);

        raiseEvent(new TaskCompletedEvent(id, userId, outcome, data, comments, Instant.now()));
    }

    /**
     * Escalate task due to timeout or other reason
     */
    public void escalate(EscalationReason reason, String escalatedTo) {
        validateNotTerminal();

        this.escalationState = new EscalationState(
            reason,
            escalatedTo,
            Instant.now(),
            currentAssignment
        );

        // Create escalation assignment
        TaskAssignment escalationAssignment = TaskAssignment.builder()
            .assigneeType(AssigneeType.USER)
            .assigneeIdentifier(escalatedTo)
            .assignedBy("SYSTEM")
            .assignedAt(Instant.now())
            .build();

        this.currentAssignment = escalationAssignment;
        this.assignmentHistory.add(escalationAssignment);
        this.status = HumanTaskStatus.ESCALATED;

        addAuditEntry("ESCALATED",
            "Task escalated to " + escalatedTo + " - Reason: " + reason,
            "SYSTEM");

        raiseEvent(new TaskEscalatedEvent(id, reason, escalatedTo, Instant.now()));
    }

    /**
     * Cancel task
     */
    public void cancel(String cancelledBy, String reason) {
        validateNotTerminal();

        this.status = HumanTaskStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.comments = reason;

        addAuditEntry("CANCELLED", "Task cancelled: " + reason, cancelledBy);
        raiseEvent(new TaskCancelledEvent(id, cancelledBy, reason, Instant.now()));
    }

    /**
     * Mark task as expired (timeout)
     */
    public void expire() {
        if (status.isTerminal()) {
            return; // Already in terminal state
        }

        this.status = HumanTaskStatus.EXPIRED;
        this.completedAt = Instant.now();

        addAuditEntry("EXPIRED", "Task expired due to timeout", "SYSTEM");
        raiseEvent(new TaskExpiredEvent(id, Instant.now()));
    }

    /**
     * Add comment to task
     */
    public void addComment(String userId, String comment) {
        validateNotTerminal();

        addAuditEntry("COMMENT", comment, userId);
        raiseEvent(new TaskCommentAddedEvent(id, userId, comment, Instant.now()));
    }

    // ==================== QUERIES ====================

    public boolean isOverdue() {
        return !status.isTerminal() &&
               dueDate != null &&
               Instant.now().isAfter(dueDate);
    }

    public boolean isAssignedTo(String userId) {
        return currentAssignment != null &&
               currentAssignment.getAssigneeIdentifier().equals(userId);
    }

    public boolean canBeClaimedBy(String userId) {
        return status == HumanTaskStatus.ASSIGNED &&
               currentAssignment != null &&
               currentAssignment.canClaim(userId);
    }

    public Duration getTimeToComplete() {
        if (completedAt == null || claimedAt == null) {
            return null;
        }
        return Duration.between(claimedAt, completedAt);
    }

    public Duration getTimeOpen() {
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return Duration.between(createdAt, endTime);
    }

    // ==================== VALIDATION ====================

    private void validateNotTerminal() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                "Cannot modify task in terminal status: " + status);
        }
    }

    private void validateCanComplete(String userId) {
        if (status != HumanTaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Can only complete tasks in IN_PROGRESS status, current: " + status);
        }

        if (!currentAssignment.getAssigneeIdentifier().equals(userId)) {
            throw new SecurityException(
                "User " + userId + " is not assigned to this task");
        }
    }

    // ==================== AUDIT ====================

    private void addAuditEntry(String action, String details, String performedBy) {
        TaskAuditEntry entry = new TaskAuditEntry(
            UUID.randomUUID().toString(),
            action,
            details,
            performedBy,
            Instant.now()
        );
        auditTrail.add(entry);
    }

    // ==================== EVENTS ====================

    private void raiseEvent(HumanTaskEvent event) {
        uncommittedEvents.add(event);
    }

    public List<HumanTaskEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String workflowRunId;
        private String nodeId;
        private String tenantId;
        private String taskType;
        private String title;
        private String description;
        private int priority = 3;
        private Map<String, Object> context = new HashMap<>();
        private Map<String, Object> formData = new HashMap<>();
        private TaskAssignment initialAssignment;
        private Instant dueDate;

        public Builder workflowRunId(String workflowRunId) {
            this.workflowRunId = workflowRunId;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder taskType(String taskType) {
            this.taskType = taskType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder priority(int priority) {
            if (priority < 1 || priority > 5) {
                throw new IllegalArgumentException("Priority must be between 1 and 5");
            }
            this.priority = priority;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public Builder formData(Map<String, Object> formData) {
            this.formData = formData;
            return this;
        }

        public Builder assignTo(TaskAssignment assignment) {
            this.initialAssignment = assignment;
            return this;
        }

        public Builder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public HumanTask build() {
            Objects.requireNonNull(workflowRunId, "workflowRunId is required");
            Objects.requireNonNull(nodeId, "nodeId is required");
            Objects.requireNonNull(tenantId, "tenantId is required");
            Objects.requireNonNull(taskType, "taskType is required");
            Objects.requireNonNull(title, "title is required");

            return new HumanTask(this);
        }
    }

    // ==================== GETTERS ====================

    public HumanTaskId getId() { return id; }
    public String getWorkflowRunId() { return workflowRunId; }
    public String getNodeId() { return nodeId; }
    public String getTenantId() { return tenantId; }
    public String getTaskType() { return taskType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public Map<String, Object> getContext() { return Collections.unmodifiableMap(context); }
    public Map<String, Object> getFormData() { return Collections.unmodifiableMap(formData); }
    public TaskAssignment getCurrentAssignment() { return currentAssignment; }
    public List<TaskAssignment> getAssignmentHistory() {
        return Collections.unmodifiableList(assignmentHistory);
    }
    public HumanTaskStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getDueDate() { return dueDate; }
    public TaskOutcome getOutcome() { return outcome; }
    public String getCompletedBy() { return completedBy; }
    public Map<String, Object> getCompletionData() {
        return completionData != null ?
            Collections.unmodifiableMap(completionData) : null;
    }
    public String getComments() { return comments; }
    public EscalationState getEscalationState() { return escalationState; }
    public List<TaskAuditEntry> getAuditTrail() {
        return Collections.unmodifiableList(auditTrail);
    }
}