package tech.kayys.wayang.hitl.repository;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import tech.kayys.wayang.hitl.domain.AssigneeType;
import tech.kayys.wayang.hitl.domain.EscalationState;
import tech.kayys.wayang.hitl.domain.HumanTask;
import tech.kayys.wayang.hitl.domain.HumanTaskStatus;
import tech.kayys.wayang.hitl.domain.TaskAssignment;
import tech.kayys.wayang.hitl.domain.TaskOutcome;

import java.time.Instant;
import java.util.*;

/**
 * HumanTaskEntity - JPA entity for human tasks
 */
@Entity
@Table(name = "human_tasks", indexes = {
        @Index(name = "idx_ht_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_ht_assignee", columnList = "assignee_identifier, status"),
        @Index(name = "idx_ht_workflow", columnList = "workflow_run_id"),
        @Index(name = "idx_ht_due_date", columnList = "due_date, status"),
        @Index(name = "idx_ht_created_at", columnList = "created_at")
})
public class HumanTaskEntity extends PanacheEntity {

    @Column(name = "task_id", unique = true, nullable = false)
    public String taskId;

    @Column(name = "workflow_run_id", nullable = false)
    public String workflowRunId;

    @Column(name = "node_id", nullable = false)
    public String nodeId;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(name = "task_type", nullable = false)
    public String taskType;

    @Column(name = "title", nullable = false, length = 500)
    public String title;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "priority")
    public int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public HumanTaskStatus status;

    // Assignment
    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type")
    public AssigneeType assigneeType;

    @Column(name = "assignee_identifier")
    public String assigneeIdentifier;

    @Column(name = "assigned_by")
    public String assignedBy;

    @Column(name = "assigned_at")
    public Instant assignedAt;

    // Temporal
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "claimed_at")
    public Instant claimedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "due_date")
    public Instant dueDate;

    // Completion
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    public TaskOutcome outcome;

    @Column(name = "completed_by")
    public String completedBy;

    @Column(name = "comments", columnDefinition = "TEXT")
    public String comments;

    // JSON data
    @Column(name = "context_data", columnDefinition = "jsonb")
    public String contextData;

    @Column(name = "form_data", columnDefinition = "jsonb")
    public String formData;

    @Column(name = "completion_data", columnDefinition = "jsonb")
    public String completionData;

    // Escalation
    @Column(name = "escalated")
    public boolean escalated;

    @Column(name = "escalation_reason")
    public String escalationReason;

    @Column(name = "escalated_to")
    public String escalatedTo;

    @Column(name = "escalated_at")
    public Instant escalatedAt;

    @Version
    @Column(name = "version")
    public long version;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Convert domain model to entity
     */
    public static HumanTaskEntity fromDomain(HumanTask task) {
        HumanTaskEntity entity = new HumanTaskEntity();

        entity.taskId = task.getId().value();
        entity.workflowRunId = task.getWorkflowRunId();
        entity.nodeId = task.getNodeId();
        entity.tenantId = task.getTenantId();
        entity.taskType = task.getTaskType();
        entity.title = task.getTitle();
        entity.description = task.getDescription();
        entity.priority = task.getPriority();
        entity.status = task.getStatus();

        // Assignment
        if (task.getCurrentAssignment() != null) {
            TaskAssignment assignment = task.getCurrentAssignment();
            entity.assigneeType = assignment.getAssigneeType();
            entity.assigneeIdentifier = assignment.getAssigneeIdentifier();
            entity.assignedBy = assignment.getAssignedBy();
            entity.assignedAt = assignment.getAssignedAt();
        }

        // Temporal
        entity.createdAt = task.getCreatedAt();
        entity.claimedAt = task.getClaimedAt();
        entity.completedAt = task.getCompletedAt();
        entity.dueDate = task.getDueDate();

        // Completion
        entity.outcome = task.getOutcome();
        entity.completedBy = task.getCompletedBy();
        entity.comments = task.getComments();

        // JSON data
        entity.contextData = mapToJson(task.getContext());
        entity.formData = mapToJson(task.getFormData());
        entity.completionData = task.getCompletionData() != null ? mapToJson(task.getCompletionData()) : null;

        // Escalation
        if (task.getEscalationState() != null) {
            EscalationState escalation = task.getEscalationState();
            entity.escalated = true;
            entity.escalationReason = escalation.reason().name();
            entity.escalatedTo = escalation.escalatedTo();
            entity.escalatedAt = escalation.escalatedAt();
        }

        entity.updatedAt = Instant.now();

        return entity;
    }

    private static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new JsonObject(map).encode();
    }

    private static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        return new JsonObject(json).getMap();
    }
}