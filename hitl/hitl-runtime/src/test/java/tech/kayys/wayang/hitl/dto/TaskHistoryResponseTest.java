package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class TaskHistoryResponseTest {

    @Test
    void shouldCreateTaskHistoryResponseSuccessfully() {
        // Given
        String taskId = "TASK-123";
        List<AuditEntryDto> auditTrail = List.of();
        List<AssignmentHistoryDto> assignmentHistory = List.of();

        // When
        TaskHistoryResponse response = new TaskHistoryResponse(
            taskId,
            auditTrail,
            assignmentHistory
        );

        // Then
        assertEquals(taskId, response.taskId());
        assertEquals(auditTrail, response.auditTrail());
        assertEquals(assignmentHistory, response.assignmentHistory());
    }
}