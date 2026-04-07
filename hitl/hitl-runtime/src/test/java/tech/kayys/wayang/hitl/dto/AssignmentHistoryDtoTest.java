package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

class AssignmentHistoryDtoTest {

    @Test
    void shouldCreateAssignmentHistoryDtoSuccessfully() {
        // Given
        String assigneeType = "USER";
        String assigneeIdentifier = "user1";
        String assignedBy = "admin";
        Instant assignedAt = Instant.now();
        String delegationReason = "Temporary assignment";

        // When
        AssignmentHistoryDto dto = new AssignmentHistoryDto(
            assigneeType,
            assigneeIdentifier,
            assignedBy,
            assignedAt,
            delegationReason
        );

        // Then
        assertEquals(assigneeType, dto.assigneeType());
        assertEquals(assigneeIdentifier, dto.assigneeIdentifier());
        assertEquals(assignedBy, dto.assignedBy());
        assertEquals(assignedAt, dto.assignedAt());
        assertEquals(delegationReason, dto.delegationReason());
    }
}