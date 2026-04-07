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
import java.util.List;
import java.util.Map;

class HumanTaskServiceTest {

    @Mock
    private HumanTaskRepository repository;

    private HumanTaskService humanTaskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        humanTaskService = new HumanTaskService();
        // Use reflection to inject the mock repository
        try {
            java.lang.reflect.Field repoField = HumanTaskService.class.getDeclaredField("repository");
            repoField.setAccessible(true);
            repoField.set(humanTaskService, repository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock repository", e);
        }
    }

    @Test
    void shouldGetTaskSuccessfully() {
        // Given
        HumanTaskId taskId = HumanTaskId.of("TASK-123");
        HumanTaskEntity entity = new HumanTaskEntity();
        entity.taskId = "TASK-123";
        entity.workflowRunId = "RUN-456";
        entity.nodeId = "NODE-789";
        entity.tenantId = "TENANT-001";
        entity.taskType = "approval";
        entity.title = "Test Task";
        entity.description = "Test Description";
        entity.priority = 3;
        entity.status = HumanTaskStatus.ASSIGNED;
        entity.assigneeIdentifier = "user1";
        entity.assigneeType = AssigneeType.USER;
        entity.assignedBy = "admin";
        entity.assignedAt = Instant.now();
        entity.createdAt = Instant.now();
        
        when(repository.findByTaskId(eq("TASK-123"), eq("*"))).thenReturn(Uni.createFrom().item(entity));

        // When
        Uni<HumanTask> result = humanTaskService.getTask(taskId);

        // Then
        HumanTask task = result.await().indefinitely();
        assertNotNull(task);
        assertEquals("RUN-456", task.getWorkflowRunId());
        assertNotNull(task.getCurrentAssignment());
        assertEquals("user1", task.getCurrentAssignment().getAssigneeIdentifier());
    }

    @Test
    void shouldReturnFailureWhenTaskNotFound() {
        // Given
        HumanTaskId taskId = HumanTaskId.of("TASK-NOT-FOUND");
        
        when(repository.findByTaskId(eq("TASK-NOT-FOUND"), eq("*"))).thenReturn(Uni.createFrom().nullItem());

        // When
        Uni<HumanTask> result = humanTaskService.getTask(taskId);

        // Then
        assertThrows(RuntimeException.class, () -> {
            result.await().indefinitely();
        });
    }

    @Test
    void shouldGetTasksForUserSuccessfully() {
        // Given
        String userId = "user1";
        String tenantId = "tenant-001";
        List<HumanTaskStatus> statuses = List.of(HumanTaskStatus.ASSIGNED);
        
        HumanTaskEntity entity = new HumanTaskEntity();
        entity.taskId = "TASK-123";
        entity.workflowRunId = "RUN-456";
        entity.nodeId = "NODE-789";
        entity.tenantId = "TENANT-001";
        entity.taskType = "approval";
        entity.title = "Test Task";
        entity.description = "Test Description";
        entity.priority = 3;
        entity.status = HumanTaskStatus.ASSIGNED;
        entity.assigneeIdentifier = "user1";
        entity.assigneeType = AssigneeType.USER;
        entity.assignedBy = "admin";
        entity.assignedAt = Instant.now();
        entity.createdAt = Instant.now();
        
        when(repository.findAssignedToUser(eq(userId), eq(tenantId), eq(statuses))).thenReturn(Uni.createFrom().item(List.of(entity)));

        // When
        Uni<List<HumanTask>> result = humanTaskService.getTasksForUser(userId, tenantId, statuses);

        // Then
        List<HumanTask> tasks = result.await().indefinitely();
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("RUN-456", tasks.get(0).getWorkflowRunId());
        assertEquals("Test Task", tasks.get(0).getTitle());
    }

    @Test
    void shouldGetTasksForWorkflowRunSuccessfully() {
        // Given
        String workflowRunId = "RUN-456";
        String tenantId = "tenant-001";
        
        HumanTaskEntity entity = new HumanTaskEntity();
        entity.taskId = "TASK-123";
        entity.workflowRunId = "RUN-456";
        entity.nodeId = "NODE-789";
        entity.tenantId = "TENANT-001";
        entity.taskType = "approval";
        entity.title = "Test Task";
        entity.description = "Test Description";
        entity.priority = 3;
        entity.status = HumanTaskStatus.ASSIGNED;
        entity.assigneeIdentifier = "user1";
        entity.assigneeType = AssigneeType.USER;
        entity.assignedBy = "admin";
        entity.assignedAt = Instant.now();
        entity.createdAt = Instant.now();
        
        when(repository.findByWorkflowRun(eq(workflowRunId), eq(tenantId))).thenReturn(Uni.createFrom().item(List.of(entity)));

        // When
        Uni<List<HumanTask>> result = humanTaskService.getTasksForWorkflowRun(workflowRunId, tenantId);

        // Then
        List<HumanTask> tasks = result.await().indefinitely();
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("RUN-456", tasks.get(0).getWorkflowRunId());
    }

    @Test
    void shouldGetUserTaskStatisticsSuccessfully() {
        // Given
        String userId = "user1";
        String tenantId = "tenant-001";
        
        when(repository.countActiveTasksForUser(eq(userId), eq(tenantId))).thenReturn(Uni.createFrom().item(5L));

        // When
        Uni<TaskStatistics> result = humanTaskService.getUserTaskStatistics(userId, tenantId);

        // Then
        TaskStatistics stats = result.await().indefinitely();
        assertNotNull(stats);
        assertEquals(5L, stats.activeTasks());
    }
}
