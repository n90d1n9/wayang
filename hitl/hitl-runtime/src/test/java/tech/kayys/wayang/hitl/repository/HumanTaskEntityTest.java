package tech.kayys.wayang.hitl.repository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tech.kayys.wayang.hitl.domain.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class HumanTaskEntityTest {

    @Test
    void shouldConvertFromDomainSuccessfully() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder()
                .assigneeType(AssigneeType.USER)
                .assigneeIdentifier("user1")
                .assignedBy("admin")
                .build();
        
        Map<String, Object> context = new HashMap<>();
        context.put("key1", "value1");
        
        Map<String, Object> formData = new HashMap<>();
        formData.put("field1", "data1");
        
        HumanTask task = HumanTask.builder()
                .workflowRunId("workflow-123")
                .nodeId("node-456")
                .tenantId("tenant-789")
                .taskType("approval")
                .title("Test Task")
                .description("Test Description")
                .priority(3)
                .context(context)
                .formData(formData)
                .assignTo(assignment)
                .dueDate(Instant.now())
                .build();

        // When
        HumanTaskEntity entity = HumanTaskEntity.fromDomain(task);

        // Then
        assertEquals(task.getId().value(), entity.taskId);
        assertEquals(task.getWorkflowRunId(), entity.workflowRunId);
        assertEquals(task.getNodeId(), entity.nodeId);
        assertEquals(task.getTenantId(), entity.tenantId);
        assertEquals(task.getTaskType(), entity.taskType);
        assertEquals(task.getTitle(), entity.title);
        assertEquals(task.getDescription(), entity.description);
        assertEquals(task.getPriority(), entity.priority);
        assertEquals(task.getStatus(), entity.status);
        assertEquals(assignment.getAssigneeType(), entity.assigneeType);
        assertEquals(assignment.getAssigneeIdentifier(), entity.assigneeIdentifier);
        assertEquals(assignment.getAssignedBy(), entity.assignedBy);
        assertEquals(assignment.getAssignedAt(), entity.assignedAt);
        assertEquals(task.getDueDate(), entity.dueDate);
        assertNotNull(entity.contextData);
        assertTrue(entity.contextData.contains("\"key1\":\"value1\""));
        assertNotNull(entity.formData);
        assertTrue(entity.formData.contains("\"field1\":\"data1\""));
    }

    @Test
    void shouldHandleNullContextAndFormData() {
        // Given
        HumanTask task = HumanTask.builder()
                .workflowRunId("workflow-123")
                .nodeId("node-456")
                .tenantId("tenant-789")
                .taskType("approval")
                .title("Test Task")
                .build();

        // When
        HumanTaskEntity entity = HumanTaskEntity.fromDomain(task);

        // Then
        assertEquals(task.getId().value(), entity.taskId);
        assertEquals(task.getWorkflowRunId(), entity.workflowRunId);
        // Context and form data should be handled gracefully
    }
}
