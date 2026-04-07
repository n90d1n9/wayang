package tech.kayys.wayang.hitl.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.error.ErrorCode;
import tech.kayys.wayang.error.WayangException;
import tech.kayys.wayang.hitl.dto.*;
import tech.kayys.wayang.hitl.service.EscalationService;
import tech.kayys.wayang.hitl.service.HumanTaskService;
import tech.kayys.wayang.hitl.service.TaskStatistics;
import tech.kayys.wayang.hitl.domain.*;
import tech.kayys.wayang.hitl.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * HUMAN TASK REST API
 *
 * Complete REST API for human task management.
 *
 * Endpoints:
 * - GET /api/v1/tasks - List tasks
 * - GET /api/v1/tasks/{id} - Get task details
 * - GET /api/v1/tasks/my-tasks - Get tasks for current user
 * - POST /api/v1/tasks/{id}/claim - Claim task
 * - POST /api/v1/tasks/{id}/release - Release task
 * - POST /api/v1/tasks/{id}/delegate - Delegate task
 * - POST /api/v1/tasks/{id}/approve - Approve task
 * - POST /api/v1/tasks/{id}/reject - Reject task
 * - POST /api/v1/tasks/{id}/complete - Complete task
 * - POST /api/v1/tasks/{id}/comments - Add comment
 * - GET /api/v1/tasks/{id}/history - Get task history
 */
@Path("/api/v1/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Human Tasks", description = "Human task management")
public class HumanTaskResource {

    @Inject
    HumanTaskService taskService;

    @Inject
    HumanTaskRepository repository;

    @Inject
    HumanTaskQueryService queryService;

    @Context
    SecurityContext securityContext;

    // ==================== QUERY ENDPOINTS ====================

    @GET
    @Operation(summary = "List tasks with filters")
    public Uni<RestResponse<PagedTaskResponse>> listTasks(
            @QueryParam("assignee") String assignee,
            @QueryParam("status") String status,
            @QueryParam("taskType") String taskType,
            @QueryParam("priority") Integer priority,
            @QueryParam("overdue") Boolean overdue,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
            @QueryParam("sortAsc") @DefaultValue("false") boolean sortAsc) {

        TaskQueryFilter filter = new TaskQueryFilter();
        filter.setTenantId(getCurrentTenantId());
        filter.setAssigneeIdentifier(assignee);

        if (status != null) {
            filter.setStatuses(List.of(HumanTaskStatus.valueOf(status)));
        }

        filter.setTaskType(taskType);

        if (priority != null) {
            filter.setMinPriority(priority);
            filter.setMaxPriority(priority);
        }

        filter.setOverdue(overdue);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortAscending(sortAsc);

        return Uni.combine().all()
            .unis(
                queryService.query(filter),
                queryService.count(filter)
            )
            .asTuple()
            .map(tuple -> {
                List<HumanTaskEntity> tasks = tuple.getItem1();
                Long total = tuple.getItem2();

                List<TaskDto> taskDtos = tasks.stream()
                    .map(this::toDto)
                    .toList();

                PagedTaskResponse response = new PagedTaskResponse(
                    taskDtos,
                    page,
                    size,
                    total,
                    (int) Math.ceil((double) total / size)
                );

                return RestResponse.ok(response);
            });
    }

    @GET
    @Path("/my-tasks")
    @Operation(summary = "Get tasks for current user")
    public Uni<RestResponse<List<TaskDto>>> getMyTasks(
            @QueryParam("status") @DefaultValue("ASSIGNED,IN_PROGRESS") String statusList) {

        String userId = getCurrentUserId();
        String tenantId = getCurrentTenantId();

        List<HumanTaskStatus> statuses = List.of(statusList.split(",")).stream()
            .map(String::trim)
            .map(HumanTaskStatus::valueOf)
            .toList();

        return taskService.getTasksForUser(userId, tenantId, statuses)
            .map(tasks -> tasks.stream()
                .map(this::toDomainDto)
                .toList())
            .map(RestResponse::ok);
    }

    @GET
    @Path("/{taskId}")
    @Operation(summary = "Get task by ID")
    public Uni<RestResponse<TaskDto>> getTask(@PathParam("taskId") String taskId) {
        String tenantId = getCurrentTenantId();

        return repository.findByTaskId(taskId, tenantId)
            .map(entity -> {
                if (entity == null) {
                    return RestResponse.<TaskDto>notFound();
                }
                return RestResponse.ok(toDto(entity));
            });
    }

    @GET
    @Path("/workflow/{workflowRunId}")
    @Operation(summary = "Get tasks for workflow run")
    public Uni<RestResponse<List<TaskDto>>> getTasksForWorkflow(
            @PathParam("workflowRunId") String workflowRunId) {

        String tenantId = getCurrentTenantId();

        return taskService.getTasksForWorkflowRun(workflowRunId, tenantId)
            .map(tasks -> tasks.stream()
                .map(this::toDomainDto)
                .toList())
            .map(RestResponse::ok);
    }

    @GET
    @Path("/statistics")
    @Operation(summary = "Get task statistics for current user")
    public Uni<RestResponse<TaskStatistics>> getStatistics() {
        String userId = getCurrentUserId();
        String tenantId = getCurrentTenantId();

        return taskService.getUserTaskStatistics(userId, tenantId)
            .map(RestResponse::ok);
    }

    // ==================== ACTION ENDPOINTS ====================

    @POST
    @Path("/{taskId}/claim")
    @Operation(summary = "Claim task")
    public Uni<RestResponse<TaskDto>> claimTask(@PathParam("taskId") String taskId) {
        String userId = getCurrentUserId();
        String tenantId = getCurrentTenantId();

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.claim(userId);
                return repository.save(task);
            })
            .map(this::toDomainDto)
            .map(RestResponse::ok)
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    @POST
    @Path("/{taskId}/release")
    @Operation(summary = "Release task back to pool")
    public Uni<RestResponse<TaskDto>> releaseTask(@PathParam("taskId") String taskId) {
        String userId = getCurrentUserId();

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.release(userId);
                return repository.save(task);
            })
            .map(this::toDomainDto)
            .map(RestResponse::ok)
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    @POST
    @Path("/{taskId}/delegate")
    @Operation(summary = "Delegate task to another user")
    public Uni<RestResponse<TaskDto>> delegateTask(
            @PathParam("taskId") String taskId,
            @Valid DelegateTaskRequest request) {

        String userId = getCurrentUserId();

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.delegate(userId, request.toUserId(), request.reason());
                return repository.save(task);
            })
            .flatMap(task -> {
                // Notify new assignee
                return Uni.createFrom().item(task);
            })
            .map(this::toDomainDto)
            .map(RestResponse::ok)
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    @POST
    @Path("/{taskId}/approve")
    @Operation(summary = "Approve task")
    public Uni<RestResponse<Void>> approveTask(
            @PathParam("taskId") String taskId,
            @Valid ApproveTaskRequest request) {

        String userId = getCurrentUserId();

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.approve(userId, request.data() != null ? request.data() : Map.of(), request.comments());
                return repository.save(task);
            })
            .map(v -> RestResponse.<Void>ok())
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    @POST
    @Path("/{taskId}/reject")
    @Operation(summary = "Reject task")
    public Uni<RestResponse<Void>> rejectTask(
            @PathParam("taskId") String taskId,
            @Valid RejectTaskRequest request) {

        String userId = getCurrentUserId();

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.reject(userId, request.reason(), request.data() != null ? request.data() : Map.of());
                return repository.save(task);
            })
            .map(v -> RestResponse.<Void>ok())
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    @POST
    @Path("/{taskId}/complete")
    @Operation(summary = "Complete task with custom outcome")
    public Uni<RestResponse<Void>> completeTask(
            @PathParam("taskId") String taskId,
            @Valid CompleteTaskRequest request) {

        String userId = getCurrentUserId();

        TaskOutcome outcome = request.outcome() != null ?
            TaskOutcome.valueOf(request.outcome()) :
            TaskOutcome.COMPLETED;

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.complete(userId, outcome, request.data() != null ? request.data() : Map.of(), request.comments());
                return repository.save(task);
            })
            .map(v -> RestResponse.<Void>ok())
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    @POST
    @Path("/{taskId}/comments")
    @Operation(summary = "Add comment to task")
    public Uni<RestResponse<Void>> addComment(
            @PathParam("taskId") String taskId,
            @Valid AddCommentRequest request) {

        String userId = getCurrentUserId();

        return taskService.getTask(HumanTaskId.of(taskId))
            .flatMap(task -> {
                task.addComment(userId, request.comment());
                return repository.save(task);
            })
            .map(v -> RestResponse.<Void>ok())
            .onFailure().transform(error ->
                new WayangException(ErrorCode.VALIDATION_FAILED, error.getMessage(), error));
    }

    // ==================== HELPER METHODS ====================

    private String getCurrentUserId() {
        // Extract from security context
        // In production, get from JWT or session
        return securityContext.getUserPrincipal().getName();
    }

    private String getCurrentTenantId() {
        // Extract from security context or headers
        // In production, use proper tenant resolution
        return "default-tenant";
    }

    private TaskDto toDto(HumanTaskEntity entity) {
        return new TaskDto(
            entity.taskId,
            entity.workflowRunId,
            entity.nodeId,
            entity.taskType,
            entity.title,
            entity.description,
            entity.priority,
            entity.status.name(),
            entity.assigneeType != null ? entity.assigneeType.name() : null,
            entity.assigneeIdentifier,
            entity.assignedBy,
            entity.createdAt,
            entity.claimedAt,
            entity.completedAt,
            entity.dueDate,
            entity.outcome != null ? entity.outcome.name() : null,
            entity.completedBy,
            entity.comments,
            parseJson(entity.formData),
            entity.escalated,
            entity.escalatedTo
        );
    }

    private TaskDto toDomainDto(HumanTask task) {
        return new TaskDto(
            task.getId().value(),
            task.getWorkflowRunId(),
            task.getNodeId(),
            task.getTaskType(),
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getStatus().name(),
            task.getCurrentAssignment() != null ?
                task.getCurrentAssignment().getAssigneeType().name() : null,
            task.getCurrentAssignment() != null ?
                task.getCurrentAssignment().getAssigneeIdentifier() : null,
            task.getCurrentAssignment() != null ?
                task.getCurrentAssignment().getAssignedBy() : null,
            task.getCreatedAt(),
            task.getClaimedAt(),
            task.getCompletedAt(),
            task.getDueDate(),
            task.getOutcome() != null ? task.getOutcome().name() : null,
            task.getCompletedBy(),
            task.getComments(),
            task.getFormData(),
            task.getEscalationState() != null,
            task.getEscalationState() != null ?
                task.getEscalationState().escalatedTo() : null
        );
    }

    private AuditEntryDto toAuditDto(TaskAuditEntity entity) {
        return new AuditEntryDto(
            entity.entryId,
            entity.action,
            entity.details,
            entity.performedBy,
            entity.timestamp
        );
    }

    private AssignmentHistoryDto toAssignmentDto(TaskAssignmentEntity entity) {
        return new AssignmentHistoryDto(
            entity.assigneeType.name(),
            entity.assigneeIdentifier,
            entity.assignedBy,
            entity.assignedAt,
            entity.delegationReason
        );
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return io.vertx.core.json.JsonObject.mapFrom(json).getMap();
    }
}
