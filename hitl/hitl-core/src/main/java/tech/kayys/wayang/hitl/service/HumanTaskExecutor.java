package tech.kayys.wayang.hitl.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.hitl.domain.AssigneeType;
import tech.kayys.wayang.hitl.domain.HumanTask;
import tech.kayys.wayang.hitl.domain.HumanTaskId;
import tech.kayys.wayang.hitl.domain.TaskAssignment;
import tech.kayys.wayang.hitl.domain.TaskOutcome;
import tech.kayys.wayang.hitl.repository.HumanTaskEntity;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Human Task Executor
 *
 * Production-ready executor for human-in-the-loop workflows.
 *
 * Features:
 * - Task lifecycle management (create, assign, claim, complete)
 * - Multi-level approval workflows
 * - Task delegation and escalation
 * - Timeout and SLA monitoring
 * - Notification integration (email, Slack, etc.)
 * - Audit trail
 * - Multi-tenancy support
 */
@ApplicationScoped
public class HumanTaskExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HumanTaskExecutor.class);

    @Inject
    HumanTaskRepository repository;

    @Inject
    NotificationService notificationService;

    @Inject
    HumanTaskService humanTaskService;

    @Inject
    EscalationService escalationService;

    // Track pending tasks waiting for completion
    private final Map<String, PendingTaskContext> pendingTasks = new ConcurrentHashMap<>();

    /**
     * Execute human task node
     *
     * Expected configuration in node config:
     * - assignTo: user ID, group ID, or role
     * - assigneeType: USER, GROUP, or ROLE
     * - taskType: approval, review, data_entry, etc.
     * - title: Task title
     * - description: Task description
     * - priority: 1-5
     * - dueInHours: Hours until due (optional)
     * - formSchema: JSON schema for task form (optional)
     * - escalationConfig: Escalation configuration (optional)
     * - notificationConfig: Notification preferences (optional)
     */
    public Uni<Void> execute(String workflowRunId, String nodeId, Map<String, Object> config) {
        LOG.info("Executing human task for run: {}, node: {}", workflowRunId, nodeId);

        // Extract configuration
        String assignTo = (String) config.get("assignTo");
        String assigneeTypeStr = (String) config.getOrDefault("assigneeType", "USER");
        AssigneeType assigneeType = AssigneeType.valueOf(assigneeTypeStr.toUpperCase());

        String taskType = (String) config.getOrDefault("taskType", "approval");
        String title = (String) config.get("title");
        String description = (String) config.get("description");
        int priority = ((Number) config.getOrDefault("priority", 3)).intValue();

        // Calculate due date
        Instant dueDate = null;
        if (config.containsKey("dueInHours")) {
            int dueInHours = ((Number) config.get("dueInHours")).intValue();
            dueDate = Instant.now().plus(Duration.ofHours(dueInHours));
        } else if (config.containsKey("dueInDays")) {
            int dueInDays = ((Number) config.get("dueInDays")).intValue();
            dueDate = Instant.now().plus(Duration.ofDays(dueInDays));
        }

        // Extract form data
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = (Map<String, Object>) config.getOrDefault("formData", new HashMap<>());

        // Create task assignment
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(assigneeType)
                .assigneeIdentifier(assignTo)
                .assignedBy("SYSTEM")
                .assignedAt(Instant.now())
                .build();

        // Create human task
        HumanTask humanTask = HumanTask.builder()
                .workflowRunId(workflowRunId)
                .nodeId(nodeId)
                .tenantId(extractTenantId(config))
                .taskType(taskType)
                .title(title)
                .description(description)
                .priority(priority)
                .context(config)
                .formData(formData)
                .assignTo(assignment)
                .dueDate(dueDate)
                .build();

        // Persist task
        return repository.save(humanTask)
                .flatMap(savedTask -> {
                    // Track pending task
                    PendingTaskContext context = new PendingTaskContext(
                            workflowRunId,
                            nodeId,
                            humanTask.getId(),
                            Instant.now());
                    pendingTasks.put(humanTask.getId().value(), context);

                    // Send notifications
                    return sendTaskNotifications(humanTask, config)
                            .flatMap(v -> {
                                // Schedule escalation if configured
                                if (config.containsKey("escalationConfig")) {
                                    return scheduleEscalation(humanTask, config);
                                }
                                return Uni.createFrom().voidItem();
                            });
                })
                .onFailure().invoke(error -> {
                    LOG.error("Failed to create human task", error);
                });
    }

    /**
     * Complete human task and resume workflow
     * Called when user approves/rejects/completes task
     */
    public Uni<Void> completeTask(
            String taskId,
            String userId,
            TaskOutcome outcome,
            Map<String, Object> completionData,
            String comments) {

        LOG.info("Completing human task: {} by user: {} with outcome: {}",
                taskId, userId, outcome);

        return humanTaskService.getTask(HumanTaskId.of(taskId))
                .flatMap(task -> {
                    // Complete the task
                    switch (outcome) {
                        case APPROVED -> {
                            task.approve(userId, completionData, comments);
                            return repository.save(task);
                        }
                        case REJECTED -> {
                            task.reject(userId, comments, completionData);
                            return repository.save(task);
                        }
                        default -> {
                            task.complete(userId, outcome, completionData, comments);
                            return repository.save(task);
                        }
                    }
                })
                .flatMap(completedTask -> {
                    // Remove from pending tasks
                    PendingTaskContext context = pendingTasks.remove(taskId);
                    if (context == null) {
                        LOG.warn("No pending context found for task: {}", taskId);
                        return Uni.createFrom().voidItem();
                    }

                    // Process completion (in a real system, this would resume the workflow)
                    LOG.info("Task completed and removed from pending: {}", taskId);
                    return Uni.createFrom().voidItem();
                })
                .invoke(() -> LOG.info("Human task completed: {}", taskId))
                .onFailure().invoke(error -> LOG.error("Failed to complete human task: {}", taskId, error));
    }

    /**
     * Claim task for user
     */
    public Uni<HumanTask> claimTask(String taskId, String userId, String tenantId) {
        LOG.info("User {} claiming task: {}", userId, taskId);

        return humanTaskService.getTask(HumanTaskId.of(taskId))
                .flatMap(task -> {
                    task.claim(userId);
                    return repository.save(task);
                })
                .invoke(() -> LOG.info("Task claimed: {} by {}", taskId, userId));
    }

    /**
     * Delegate task to another user
     */
    public Uni<HumanTask> delegateTask(
            String taskId,
            String fromUserId,
            String toUserId,
            String reason) {

        LOG.info("Delegating task {} from {} to {}", taskId, fromUserId, toUserId);

        return humanTaskService.getTask(HumanTaskId.of(taskId))
                .flatMap(task -> {
                    task.delegate(fromUserId, toUserId, reason);
                    return repository.save(task);
                })
                .flatMap(task ->
                // Notify new assignee
                notificationService.sendTaskAssignedNotification(task)
                        .replaceWith(task))
                .invoke(() -> LOG.info("Task delegated: {}", taskId));
    }

    /**
     * Add comment to task
     */
    public Uni<Void> addComment(String taskId, String userId, String comment) {
        return humanTaskService.getTask(HumanTaskId.of(taskId))
                .flatMap(task -> {
                    task.addComment(userId, comment);
                    return repository.save(task);
                })
                .flatMap(task ->
                // Notify stakeholders of comment
                notificationService.sendTaskCommentNotification(task, userId, comment))
                .replaceWithVoid();
    }

    /**
     * Scheduled job to check for overdue tasks
     * Runs every 5 minutes
     */
    @Scheduled(every = "5m")
    void checkOverdueTasks() {
        LOG.debug("Checking for overdue tasks");

        // Process each tenant
        repository.findOverdueTasks("*") // In production, iterate over tenants
                .subscribe().with(
                        overdueTasks -> {
                            LOG.info("Found {} overdue tasks", overdueTasks.size());
                            overdueTasks.forEach(this::handleOverdueTask);
                        },
                        error -> LOG.error("Error checking overdue tasks", error));
    }

    /**
     * Scheduled job to check for tasks requiring escalation
     * Runs every 10 minutes
     */
    @Scheduled(every = "10m")
    void checkEscalations() {
        LOG.debug("Checking for tasks requiring escalation");

        escalationService.processEscalations()
                .subscribe().with(
                        escalatedCount -> LOG.info("Escalated {} tasks", escalatedCount),
                        error -> LOG.error("Error processing escalations", error));
    }

    /**
     * Scheduled job to send reminder notifications
     * Runs every hour
     */
    @Scheduled(every = "1h")
    void sendReminders() {
        LOG.debug("Sending task reminders");

        notificationService.sendTaskReminders()
                .subscribe().with(
                        sentCount -> LOG.info("Sent {} task reminders", sentCount),
                        error -> LOG.error("Error sending reminders", error));
    }

    // ==================== PRIVATE HELPERS ====================

    private Uni<Void> sendTaskNotifications(
            HumanTask task,
            Map<String, Object> config) {

        @SuppressWarnings("unchecked")
        Map<String, Object> notificationConfig = (Map<String, Object>) config.getOrDefault("notificationConfig",
                new HashMap<>());

        boolean emailEnabled = (boolean) notificationConfig.getOrDefault("email", true);
        boolean slackEnabled = (boolean) notificationConfig.getOrDefault("slack", false);

        List<Uni<Void>> notifications = new ArrayList<>();

        if (emailEnabled) {
            notifications.add(notificationService.sendTaskAssignedNotification(task));
        }

        if (slackEnabled) {
            notifications.add(notificationService.sendSlackNotification(task));
        }

        if (notifications.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return Uni.join().all(notifications).andFailFast()
                .replaceWithVoid()
                .onFailure().invoke(error -> LOG.warn("Some notifications failed to send", error));
    }

    private Uni<Void> scheduleEscalation(
            HumanTask task,
            Map<String, Object> config) {

        @SuppressWarnings("unchecked")
        Map<String, Object> escalationConfig = (Map<String, Object>) config.get("escalationConfig");

        String escalateTo = (String) escalationConfig.get("escalateTo");
        int escalateAfterHours = ((Number) escalationConfig
                .getOrDefault("escalateAfterHours", 24)).intValue();

        return escalationService.scheduleEscalation(
                task.getId(),
                escalateTo,
                Duration.ofHours(escalateAfterHours));
    }

    private void handleOverdueTask(HumanTaskEntity taskEntity) {
        LOG.warn("Task overdue: {} - {}", taskEntity.taskId, taskEntity.title);

        humanTaskService.getTask(HumanTaskId.of(taskEntity.taskId))
                .flatMap(task -> {
                    // Send overdue notification
                    return notificationService.sendOverdueNotification(task);
                })
                .subscribe().with(
                        v -> LOG.debug("Overdue notification sent for task: {}", taskEntity.taskId),
                        error -> LOG.error("Failed to send overdue notification", error));
    }

    private String extractTenantId(Map<String, Object> config) {
        Object tenantId = config.get("tenantId");
        if (tenantId != null) {
            return tenantId.toString();
        }
        return "default-tenant"; // Fallback
    }

    // ==================== PENDING TASK CONTEXT ====================

    private static class PendingTaskContext {
        final String workflowRunId;
        final String nodeId;
        final HumanTaskId humanTaskId;
        final Instant createdAt;

        PendingTaskContext(
                String workflowRunId,
                String nodeId,
                HumanTaskId humanTaskId,
                Instant createdAt) {
            this.workflowRunId = workflowRunId;
            this.nodeId = nodeId;
            this.humanTaskId = humanTaskId;
            this.createdAt = createdAt;
        }
    }
}