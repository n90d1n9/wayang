package tech.kayys.wayang.hitl.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.hitl.domain.*;
import tech.kayys.wayang.hitl.repository.HumanTaskEntity;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Application service for human task operations
 */
@ApplicationScoped
public class HumanTaskService {

    private static final Logger LOG = LoggerFactory.getLogger(HumanTaskService.class);

    @Inject
    HumanTaskRepository repository;

    /**
     * Get human task by ID
     */
    public Uni<HumanTask> getTask(HumanTaskId taskId) {
        return repository.findByTaskId(taskId.value(), "*")
                .flatMap(entity -> {
                    if (entity == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Task not found: " + taskId.value()));
                    }
                    return Uni.createFrom().item(toTask(entity));
                });
    }

    /**
     * Get tasks assigned to user
     */
    public Uni<List<HumanTask>> getTasksForUser(
            String userId,
            String tenantId,
            List<HumanTaskStatus> statuses) {
        LOG.debug("Getting tasks for user: {}", userId);
        return repository.findAssignedToUser(userId, tenantId, statuses)
                .map(entities -> entities.stream()
                        .map(this::toTask)
                        .toList());
    }

    /**
     * Get tasks for workflow run
     */
    public Uni<List<HumanTask>> getTasksForWorkflowRun(
            String workflowRunId,
            String tenantId) {

        return repository.findByWorkflowRun(workflowRunId, tenantId)
                .map(entities -> entities.stream()
                        .map(this::toTask)
                        .toList());
    }

    /**
     * Get task statistics for user
     */
    public Uni<TaskStatistics> getUserTaskStatistics(String userId, String tenantId) {
        return repository.countActiveTasksForUser(userId, tenantId)
                .map(activeCount -> new TaskStatistics(
                        activeCount,
                        0L, // completed today
                        0L // overdue
                ));
    }

    private HumanTask toTask(HumanTaskEntity entity) {
        // Reconstruct HumanTask from entity
        // In production, use proper mapper or factory

        TaskAssignment assignment = null;
        if (entity.assigneeIdentifier != null) {
            assignment = TaskAssignment.builder()
                    .assigneeType(entity.assigneeType)
                    .assigneeIdentifier(entity.assigneeIdentifier)
                    .assignedBy(entity.assignedBy)
                    .assignedAt(entity.assignedAt)
                    .build();
        }

        return HumanTask.builder()
                .workflowRunId(entity.workflowRunId)
                .nodeId(entity.nodeId)
                .tenantId(entity.tenantId)
                .taskType(entity.taskType)
                .title(entity.title)
                .description(entity.description)
                .priority(entity.priority)
                .context(parseJson(entity.contextData))
                .formData(parseJson(entity.formData))
                .assignTo(assignment)
                .dueDate(entity.dueDate)
                .build();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        return io.vertx.core.json.JsonObject.mapFrom(json).getMap();
    }
}