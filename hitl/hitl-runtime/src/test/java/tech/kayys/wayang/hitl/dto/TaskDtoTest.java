package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TaskDtoTest {

    @Test
    void shouldCreateTaskDtoSuccessfully() {
        // Given
        String taskId = "TASK-123";
        String workflowRunId = "RUN-456";
        String nodeId = "NODE-789";
        String taskType = "approval";
        String title = "Test Task";
        String description = "Test Description";
        int priority = 3;
        String status = "ASSIGNED";
        String assigneeType = "USER";
        String assigneeIdentifier = "user1";
        String assignedBy = "admin";
        Instant createdAt = Instant.now();
        Instant claimedAt = Instant.now();
        Instant completedAt = Instant.now();
        Instant dueDate = Instant.now();
        String outcome = "PENDING";
        String completedBy = "user1";
        String comments = "Test comments";
        Map<String, Object> formData = new HashMap<>();
        formData.put("field1", "value1");
        boolean escalated = false;
        String escalatedTo = null;

        // When
        TaskDto taskDto = new TaskDto(
            taskId,
            workflowRunId,
            nodeId,
            taskType,
            title,
            description,
            priority,
            status,
            assigneeType,
            assigneeIdentifier,
            assignedBy,
            createdAt,
            claimedAt,
            completedAt,
            dueDate,
            outcome,
            completedBy,
            comments,
            formData,
            escalated,
            escalatedTo
        );

        // Then
        assertEquals(taskId, taskDto.taskId());
        assertEquals(workflowRunId, taskDto.workflowRunId());
        assertEquals(nodeId, taskDto.nodeId());
        assertEquals(taskType, taskDto.taskType());
        assertEquals(title, taskDto.title());
        assertEquals(description, taskDto.description());
        assertEquals(priority, taskDto.priority());
        assertEquals(status, taskDto.status());
        assertEquals(assigneeType, taskDto.assigneeType());
        assertEquals(assigneeIdentifier, taskDto.assigneeIdentifier());
        assertEquals(assignedBy, taskDto.assignedBy());
        assertEquals(createdAt, taskDto.createdAt());
        assertEquals(claimedAt, taskDto.claimedAt());
        assertEquals(completedAt, taskDto.completedAt());
        assertEquals(dueDate, taskDto.dueDate());
        assertEquals(outcome, taskDto.outcome());
        assertEquals(completedBy, taskDto.completedBy());
        assertEquals(comments, taskDto.comments());
        assertEquals(formData, taskDto.formData());
        assertEquals(escalated, taskDto.escalated());
        assertEquals(escalatedTo, taskDto.escalatedTo());
    }
}