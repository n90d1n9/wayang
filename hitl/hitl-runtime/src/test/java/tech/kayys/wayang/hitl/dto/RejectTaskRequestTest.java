package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class RejectTaskRequestTest {

    @Test
    void shouldCreateRejectTaskRequestSuccessfully() {
        // Given
        String reason = "Does not meet criteria";
        Map<String, Object> data = new HashMap<>();
        data.put("result", "rejected");

        // When
        RejectTaskRequest request = new RejectTaskRequest(
            reason,
            data
        );

        // Then
        assertEquals(reason, request.reason());
        assertEquals(data, request.data());
    }
}