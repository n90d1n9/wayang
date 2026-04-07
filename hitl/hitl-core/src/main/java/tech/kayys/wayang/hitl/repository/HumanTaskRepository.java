package tech.kayys.wayang.hitl.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.hitl.domain.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * HumanTaskRepository - Reactive repository for human tasks
 */
@ApplicationScoped
public class HumanTaskRepository implements PanacheRepository<HumanTaskEntity> {

    /**
     * Save human task with audit trail
     */
    public Uni<HumanTask> save(HumanTask task) {
        HumanTaskEntity entity = HumanTaskEntity.fromDomain(task);

        return persist(entity)
                .flatMap(persisted -> saveAssignmentHistory(task))
                .flatMap(v -> saveAuditTrail(task))
                .replaceWith(task)
                .invoke(() -> task.markEventsAsCommitted());
    }

    /**
     * Find task by ID
     */
    public Uni<HumanTaskEntity> findByTaskId(String taskId, String tenantId) {
        return find("taskId = ?1 and tenantId = ?2", taskId, tenantId)
                .firstResult();
    }

    /**
     * Find tasks assigned to user
     */
    public Uni<List<HumanTaskEntity>> findAssignedToUser(
            String userId,
            String tenantId,
            List<HumanTaskStatus> statuses) {

        String statusList = statuses.stream()
                .map(Enum::name)
                .reduce((a, b) -> a + "','" + b)
                .orElse("");

        return find(
                "assigneeIdentifier = ?1 and tenantId = ?2 and status in ('" + statusList + "')",
                Sort.by("priority").descending().and("createdAt"),
                userId, tenantId).list();
    }

    /**
     * Find tasks by workflow run
     */
    public Uni<List<HumanTaskEntity>> findByWorkflowRun(
            String workflowRunId,
            String tenantId) {

        return find("workflowRunId = ?1 and tenantId = ?2",
                Sort.by("createdAt"),
                workflowRunId, tenantId)
                .list();
    }

    /**
     * Find overdue tasks
     */
    public Uni<List<HumanTaskEntity>> findOverdueTasks(String tenantId) {
        return find(
                "tenantId = ?1 and dueDate < ?2 and status not in (?3)",
                Sort.by("dueDate"),
                tenantId,
                Instant.now(),
                List.of(
                        HumanTaskStatus.COMPLETED,
                        HumanTaskStatus.CANCELLED,
                        HumanTaskStatus.EXPIRED))
                .list();
    }

    /**
     * Find tasks for escalation
     */
    public Uni<List<HumanTaskEntity>> findTasksForEscalation(
            String tenantId,
            Instant escalationThreshold) {

        return find(
                "tenantId = ?1 and createdAt < ?2 and status = ?3 and escalated = false",
                Sort.by("createdAt"),
                tenantId,
                escalationThreshold,
                HumanTaskStatus.ASSIGNED).list();
    }

    /**
     * Count active tasks for user
     */
    public Uni<Long> countActiveTasksForUser(String userId, String tenantId) {
        return count(
                "assigneeIdentifier = ?1 and tenantId = ?2 and status in (?3)",
                userId,
                tenantId,
                List.of(HumanTaskStatus.ASSIGNED, HumanTaskStatus.IN_PROGRESS));
    }

    /**
     * Save assignment history
     */
    private Uni<Void> saveAssignmentHistory(HumanTask task) {
        List<TaskAssignment> history = task.getAssignmentHistory();
        if (history.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        List<TaskAssignmentEntity> entities = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            TaskAssignment assignment = history.get(i);

            TaskAssignmentEntity entity = new TaskAssignmentEntity();
            entity.taskId = task.getId().value();
            entity.assigneeType = assignment.getAssigneeType();
            entity.assigneeIdentifier = assignment.getAssigneeIdentifier();
            entity.assignedBy = assignment.getAssignedBy();
            entity.assignedAt = assignment.getAssignedAt();
            entity.delegationReason = assignment.getDelegationReason();
            entity.sequenceNumber = i + 1;

            entities.add(entity);
        }

        return TaskAssignmentEntity.persist(entities)
                .replaceWithVoid();
    }

    /**
     * Save audit trail
     */
    private Uni<Void> saveAuditTrail(HumanTask task) {
        List<TaskAuditEntry> auditTrail = task.getAuditTrail();
        if (auditTrail.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        List<TaskAuditEntity> entities = auditTrail.stream()
                .map(entry -> {
                    TaskAuditEntity entity = new TaskAuditEntity();
                    entity.entryId = entry.entryId();
                    entity.taskId = task.getId().value();
                    entity.action = entry.action();
                    entity.details = entry.details();
                    entity.performedBy = entry.performedBy();
                    entity.timestamp = entry.timestamp();
                    return entity;
                })
                .toList();

        return TaskAuditEntity.persist(entities)
                .replaceWithVoid();
    }

    /**
     * Get audit trail for task
     */
    public Uni<List<TaskAuditEntity>> getAuditTrail(String taskId) {
        return TaskAuditEntity
                .find("taskId = ?1", Sort.by("timestamp"), taskId)
                .list();
    }

    /**
     * Get assignment history for task
     */
    public Uni<List<TaskAssignmentEntity>> getAssignmentHistory(String taskId) {
        return TaskAssignmentEntity
                .find("taskId = ?1", Sort.by("sequenceNumber"), taskId)
                .list();
    }
}