package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

class AuditEntryDtoTest {

    @Test
    void shouldCreateAuditEntryDtoSuccessfully() {
        // Given
        String entryId = "ENTRY-123";
        String action = "CREATED";
        String details = "Task created";
        String performedBy = "system";
        Instant timestamp = Instant.now();

        // When
        AuditEntryDto dto = new AuditEntryDto(
            entryId,
            action,
            details,
            performedBy,
            timestamp
        );

        // Then
        assertEquals(entryId, dto.entryId());
        assertEquals(action, dto.action());
        assertEquals(details, dto.details());
        assertEquals(performedBy, dto.performedBy());
        assertEquals(timestamp, dto.timestamp());
    }
}