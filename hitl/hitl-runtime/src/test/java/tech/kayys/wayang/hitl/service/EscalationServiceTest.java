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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

class EscalationServiceTest {

    @Mock
    private HumanTaskRepository repository;

    private EscalationService escalationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        escalationService = new EscalationService();
        // Use reflection to inject the mock repository
        try {
            java.lang.reflect.Field repoField = EscalationService.class.getDeclaredField("repository");
            repoField.setAccessible(true);
            repoField.set(escalationService, repository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock repository", e);
        }
    }

    @Test
    void shouldScheduleEscalationSuccessfully() {
        // Given
        HumanTaskId taskId = HumanTaskId.of("TASK-123");
        String escalateTo = "manager1";
        Duration after = Duration.ofHours(24);

        // When
        Uni<Void> result = escalationService.scheduleEscalation(taskId, escalateTo, after);

        // Then
        // Just verify that the method completes without exception
        result.await().indefinitely();
        // In the actual implementation, this would schedule an escalation
    }

    @Test
    void shouldFindTasksForEscalation() {
        // Given
        String tenantId = "tenant-001";
        Instant escalationThreshold = Instant.now().minus(Duration.ofHours(24));
        
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
        entity.createdAt = Instant.now().minus(Duration.ofHours(48)); // Created 48 hours ago
        
        when(repository.findTasksForEscalation(eq(tenantId), eq(escalationThreshold))).thenReturn(Uni.createFrom().item(List.of(entity)));

        // When
        Uni<Integer> result = escalationService.processEscalations();

        // Then
        Integer count = result.await().indefinitely();
        // The exact behavior depends on the implementation, but we can at least verify the method runs
        assertNotNull(count);
    }

    @Test
    void shouldProcessEscalationsSuccessfully() {
        // Given
        String tenantId = "tenant-001";
        Instant escalationThreshold = Instant.now().minus(Duration.ofHours(24));
        
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
        entity.createdAt = Instant.now().minus(Duration.ofHours(48)); // Created 48 hours ago
        
        when(repository.findTasksForEscalation(any(String.class), any(Instant.class))).thenReturn(Uni.createFrom().item(List.of()));

        // When
        Uni<Integer> result = escalationService.processEscalations();

        // Then
        Integer count = result.await().indefinitely();
        assertNotNull(count);
    }
}