package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class CompleteTaskRequestTest {

    @Test
    void shouldCreateCompleteTaskRequestSuccessfully() {
        // Given
        String outcome = "COMPLETED";
        String comments = "Task completed successfully";
        Map<String, Object> data = new HashMap<>();
        data.put("result", "completed");

        // When
        CompleteTaskRequest request = new CompleteTaskRequest(
            outcome,
            comments,
            data
        );

        // Then
        assertEquals(outcome, request.outcome());
        assertEquals(comments, request.comments());
        assertEquals(data, request.data());
    }
}