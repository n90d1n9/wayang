package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddCommentRequestTest {

    @Test
    void shouldCreateAddCommentRequestSuccessfully() {
        // Given
        String comment = "This is a test comment";

        // When
        AddCommentRequest request = new AddCommentRequest(
            comment
        );

        // Then
        assertEquals(comment, request.comment());
    }
}