package tech.kayys.wayang.hitl.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import tech.kayys.wayang.hitl.domain.*;
import tech.kayys.wayang.hitl.repository.HumanTaskEntity;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HumanTaskExecutorTest {

    @Mock
    private HumanTaskRepository repository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HumanTaskService humanTaskService;

    @Mock
    private EscalationService escalationService;

    private HumanTaskExecutor executor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new HumanTaskExecutor();
        // Use reflection to inject the mock services
        try {
            java.lang.reflect.Field repoField = HumanTaskExecutor.class.getDeclaredField("repository");
            repoField.setAccessible(true);
            repoField.set(executor, repository);

            java.lang.reflect.Field notificationField = HumanTaskExecutor.class.getDeclaredField("notificationService");
            notificationField.setAccessible(true);
            notificationField.set(executor, notificationService);

            java.lang.reflect.Field taskServiceField = HumanTaskExecutor.class.getDeclaredField("humanTaskService");
            taskServiceField.setAccessible(true);
            taskServiceField.set(executor, humanTaskService);

            java.lang.reflect.Field escalationField = HumanTaskExecutor.class.getDeclaredField("escalationService");
            escalationField.setAccessible(true);
            escalationField.set(executor, escalationService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock services", e);
        }
    }

    @Test
    void shouldExecuteSuccessfully() {
        // Given
        String workflowRunId = "RUN-123";
        String nodeId = "NODE-456";
        Map<String, Object> config = new HashMap<>();
        config.put("assignTo", "user1");
        config.put("title", "Test Task");
        config.put("description", "Test Description");
        config.put("priority", 3);

        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("SYSTEM")
                .build();

        HumanTask task = HumanTask.builder()
                .workflowRunId(workflowRunId)
                .nodeId(nodeId)
                .tenantId("default-tenant")
                .taskType("approval")
                .title("Test Task")
                .description("Test Description")
                .priority(3)
                .assignTo(assignment)
                .build();

        when(repository.save(any(HumanTask.class))).thenReturn(Uni.createFrom().item(task));
        when(notificationService.sendTaskAssignedNotification(any(HumanTask.class))).thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = executor.execute(workflowRunId, nodeId, config);

        // Then
        result.await().indefinitely();
        verify(repository, times(1)).save(any(HumanTask.class));
        verify(notificationService, times(1)).sendTaskAssignedNotification(any(HumanTask.class));
    }

    @Test
    void shouldCompleteTaskSuccessfully() {
        // Given
        String taskId = "TASK-123";
        String userId = "user1";
        TaskOutcome outcome = TaskOutcome.APPROVED;
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("result", "approved");
        String comments = "Approved by reviewer";

        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier(userId)
                .assignedBy("SYSTEM")
                .build();

        HumanTask task = HumanTask.builder()
                .workflowRunId("RUN-123")
                .nodeId("NODE-456")
                .tenantId("default-tenant")
                .taskType("approval")
                .title("Test Task")
                .assignTo(assignment)
                .build();
        task.claim(userId);

        when(humanTaskService.getTask(any(HumanTaskId.class))).thenReturn(Uni.createFrom().item(task));
        when(repository.save(any(HumanTask.class))).thenReturn(Uni.createFrom().item(task));

        // When
        Uni<Void> result = executor.completeTask(taskId, userId, outcome, completionData, comments);

        // Then
        result.await().indefinitely();
        verify(humanTaskService, times(1)).getTask(any(HumanTaskId.class));
        verify(repository, times(1)).save(any(HumanTask.class));
    }

    @Test
    void shouldClaimTaskSuccessfully() {
        // Given
        String taskId = "TASK-123";
        String userId = "user1";
        String tenantId = "tenant-001";

        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier(userId)
                .assignedBy("SYSTEM")
                .build();

        HumanTask task = HumanTask.builder()
                .workflowRunId("RUN-123")
                .nodeId("NODE-456")
                .tenantId("default-tenant")
                .taskType("approval")
                .title("Test Task")
                .assignTo(assignment)
                .build();

        when(humanTaskService.getTask(any(HumanTaskId.class))).thenReturn(Uni.createFrom().item(task));
        when(repository.save(any(HumanTask.class))).thenReturn(Uni.createFrom().item(task));

        // When
        Uni<HumanTask> result = executor.claimTask(taskId, userId, tenantId);

        // Then
        HumanTask claimedTask = result.await().indefinitely();
        assertNotNull(claimedTask);
        verify(humanTaskService, times(1)).getTask(any(HumanTaskId.class));
        verify(repository, times(1)).save(any(HumanTask.class));
    }

    @Test
    void shouldDelegateTaskSuccessfully() {
        // Given
        String taskId = "TASK-123";
        String fromUserId = "user1";
        String toUserId = "user2";
        String reason = "Going on vacation";

        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier(fromUserId)
                .assignedBy("SYSTEM")
                .build();

        HumanTask task = HumanTask.builder()
                .workflowRunId("RUN-123")
                .nodeId("NODE-456")
                .tenantId("default-tenant")
                .taskType("approval")
                .title("Test Task")
                .assignTo(assignment)
                .build();

        when(humanTaskService.getTask(any(HumanTaskId.class))).thenReturn(Uni.createFrom().item(task));
        when(repository.save(any(HumanTask.class))).thenReturn(Uni.createFrom().item(task));
        when(notificationService.sendTaskAssignedNotification(any(HumanTask.class))).thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<HumanTask> result = executor.delegateTask(taskId, fromUserId, toUserId, reason);

        // Then
        HumanTask delegatedTask = result.await().indefinitely();
        assertNotNull(delegatedTask);
        verify(humanTaskService, times(1)).getTask(any(HumanTaskId.class));
        verify(repository, times(1)).save(any(HumanTask.class));
        verify(notificationService, times(1)).sendTaskAssignedNotification(any(HumanTask.class));
    }

    @Test
    void shouldAddCommentSuccessfully() {
        // Given
        String taskId = "TASK-123";
        String userId = "user1";
        String comment = "This is a test comment";

        HumanTask task = HumanTask.builder()
                .workflowRunId("RUN-123")
                .nodeId("NODE-456")
                .tenantId("default-tenant")
                .taskType("approval")
                .title("Test Task")
                .build();

        when(humanTaskService.getTask(any(HumanTaskId.class))).thenReturn(Uni.createFrom().item(task));
        when(repository.save(any(HumanTask.class))).thenReturn(Uni.createFrom().item(task));
        when(notificationService.sendTaskCommentNotification(any(HumanTask.class), any(String.class), any(String.class))).thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = executor.addComment(taskId, userId, comment);

        // Then
        result.await().indefinitely();
        verify(humanTaskService, times(1)).getTask(any(HumanTaskId.class));
        verify(repository, times(1)).save(any(HumanTask.class));
        verify(notificationService, times(1)).sendTaskCommentNotification(any(HumanTask.class), any(String.class), any(String.class));
    }
}
