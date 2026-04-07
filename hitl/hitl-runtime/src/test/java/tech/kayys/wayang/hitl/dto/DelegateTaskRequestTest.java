package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelegateTaskRequestTest {

    @Test
    void shouldCreateDelegateTaskRequestSuccessfully() {
        // Given
        String toUserId = "user2";
        String reason = "Going on vacation";

        // When
        DelegateTaskRequest request = new DelegateTaskRequest(
            toUserId,
            reason
        );

        // Then
        assertEquals(toUserId, request.toUserId());
        assertEquals(reason, request.reason());
    }
}