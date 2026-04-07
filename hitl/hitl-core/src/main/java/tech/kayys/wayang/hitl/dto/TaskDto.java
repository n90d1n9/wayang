package tech.kayys.wayang.hitl.dto;

import java.time.Instant;
import java.util.Map;

public record TaskDto(
                String taskId,
                String workflowRunId,
                String nodeId,
                String taskType,
                String title,
                String description,
                int priority,
                String status,
                String assigneeType,
                String assigneeIdentifier,
                String assignedBy,
                Instant createdAt,
                Instant claimedAt,
                Instant completedAt,
                Instant dueDate,
                String outcome,
                String completedBy,
                String comments,
                Map<String, Object> formData,
                boolean escalated,
                String escalatedTo) {
}