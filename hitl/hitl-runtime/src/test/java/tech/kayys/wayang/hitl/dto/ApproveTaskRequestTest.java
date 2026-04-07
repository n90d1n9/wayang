package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class ApproveTaskRequestTest {

    @Test
    void shouldCreateApproveTaskRequestSuccessfully() {
        // Given
        String comments = "Approved by reviewer";
        Map<String, Object> data = new HashMap<>();
        data.put("result", "approved");

        // When
        ApproveTaskRequest request = new ApproveTaskRequest(
            comments,
            data
        );

        // Then
        assertEquals(comments, request.comments());
        assertEquals(data, request.data());
    }
}