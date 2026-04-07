package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class HumanTaskTest {

    private HumanTask.Builder builder;

    @BeforeEach
    void setUp() {
        builder = HumanTask.builder()
                .workflowRunId("workflow-123")
                .nodeId("node-456")
                .tenantId("tenant-789")
                .taskType("approval")
                .title("Test Task")
                .description("Test Description");
    }

    @Test
    void shouldCreateTaskSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();

        // When
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // Then
        assertNotNull(task.getId());
        assertEquals("workflow-123", task.getWorkflowRunId());
        assertEquals("node-456", task.getNodeId());
        assertEquals("tenant-789", task.getTenantId());
        assertEquals("approval", task.getTaskType());
        assertEquals("Test Task", task.getTitle());
        assertEquals("Test Description", task.getDescription());
        assertEquals(3, task.getPriority()); // default
        assertEquals(HumanTaskStatus.ASSIGNED, task.getStatus());
        assertEquals("user1", task.getCurrentAssignment().getAssigneeIdentifier());
    }

    @Test
    void shouldSetPriorityCorrectly() {
        // When
        HumanTask task = builder
                .priority(5)
                .build();

        // Then
        assertEquals(5, task.getPriority());
    }

    @Test
    void shouldThrowExceptionForInvalidPriority() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            builder.priority(6).build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            builder.priority(0).build();
        });
    }

    @Test
    void shouldClaimTaskSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When
        task.claim("user1");

        // Then
        assertEquals(HumanTaskStatus.IN_PROGRESS, task.getStatus());
        assertNotNull(task.getClaimedAt());
    }

    @Test
    void shouldThrowExceptionWhenClaimingNonAssignedTask() {
        // Given
        HumanTask task = builder.build();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            task.claim("user1");
        });
    }

    @Test
    void shouldThrowExceptionWhenClaimingByWrongUser() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When & Then
        assertThrows(SecurityException.class, () -> {
            task.claim("user2");
        });
    }

    @Test
    void shouldDelegateTaskSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When
        task.delegate("user1", "user2", "Going on vacation");

        // Then
        assertEquals(HumanTaskStatus.ASSIGNED, task.getStatus());
        assertEquals("user2", task.getCurrentAssignment().getAssigneeIdentifier());
        assertEquals(2, task.getAssignmentHistory().size());
    }

    @Test
    void shouldThrowExceptionWhenDelegatingByWrongUser() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When & Then
        assertThrows(SecurityException.class, () -> {
            task.delegate("user2", "user3", "Wrong user");
        });
    }

    @Test
    void shouldCompleteTaskSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();
        
        task.claim("user1");

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("result", "approved");
        task.approve("user1", data, "Approved by reviewer");

        // Then
        assertEquals(HumanTaskStatus.COMPLETED, task.getStatus());
        assertEquals(TaskOutcome.APPROVED, task.getOutcome());
        assertEquals("user1", task.getCompletedBy());
        assertEquals("Approved by reviewer", task.getComments());
        assertTrue(task.getCompletionData().containsKey("result"));
    }

    @Test
    void shouldRejectTaskSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();
        
        task.claim("user1");

        // When
        Map<String, Object> data = new HashMap<>();
        data.put("reason", "does not meet criteria");
        task.reject("user1", "Does not meet criteria", data);

        // Then
        assertEquals(HumanTaskStatus.COMPLETED, task.getStatus());
        assertEquals(TaskOutcome.REJECTED, task.getOutcome());
        assertEquals("user1", task.getCompletedBy());
        assertEquals("Does not meet criteria", task.getComments());
    }

    @Test
    void shouldThrowExceptionWhenCompletingByWrongUser() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();
        
        task.claim("user1");

        // When & Then
        assertThrows(SecurityException.class, () -> {
            task.approve("user2", new HashMap<>(), "Wrong user");
        });
    }

    @Test
    void shouldThrowExceptionWhenCompletingNonInProgressTask() {
        // Given
        HumanTask task = builder.build();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            task.approve("user1", new HashMap<>(), "Task not in progress");
        });
    }

    @Test
    void shouldCancelTaskSuccessfully() {
        // Given
        HumanTask task = builder.build();

        // When
        task.cancel("admin", "Business requirements changed");

        // Then
        assertEquals(HumanTaskStatus.CANCELLED, task.getStatus());
        assertEquals("Business requirements changed", task.getComments());
    }

    @Test
    void shouldExpireTaskSuccessfully() {
        // Given
        HumanTask task = builder.build();

        // When
        task.expire();

        // Then
        assertEquals(HumanTaskStatus.EXPIRED, task.getStatus());
    }

    @Test
    void shouldNotExpireAlreadyCompletedTask() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();
        
        task.claim("user1");
        task.approve("user1", new HashMap<>(), "Approved");

        // When
        task.expire();

        // Then
        assertEquals(HumanTaskStatus.COMPLETED, task.getStatus()); // Should remain completed
    }

    @Test
    void shouldCheckIfOverdue() {
        // Given
        HumanTask task = builder
                .dueDate(Instant.now().minusSeconds(1))
                .build();

        // When & Then
        assertTrue(task.isOverdue());
    }

    @Test
    void shouldCheckIfNotOverdue() {
        // Given
        HumanTask task = builder
                .dueDate(Instant.now().plusSeconds(1))
                .build();

        // When & Then
        assertFalse(task.isOverdue());
    }

    @Test
    void shouldCheckIfAssignedToUser() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When & Then
        assertTrue(task.isAssignedTo("user1"));
        assertFalse(task.isAssignedTo("user2"));
    }

    @Test
    void shouldReturnUncommittedEvents() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When
        int eventCount = task.getUncommittedEvents().size();

        // Then
        assertEquals(2, eventCount); // Created and Assigned events
    }

    @Test
    void shouldMarkEventsAsCommitted() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("system")
                .build();
        
        HumanTask task = builder
                .assignTo(assignment)
                .build();

        // When
        task.markEventsAsCommitted();
        int eventCount = task.getUncommittedEvents().size();

        // Then
        assertEquals(0, eventCount);
    }
}
