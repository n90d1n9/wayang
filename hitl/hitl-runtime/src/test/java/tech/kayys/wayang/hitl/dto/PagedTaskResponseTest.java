package tech.kayys.wayang.hitl.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class PagedTaskResponseTest {

    @Test
    void shouldCreatePagedTaskResponseSuccessfully() {
        // Given
        List<TaskDto> tasks = List.of();
        int page = 0;
        int size = 20;
        long totalElements = 100;
        int totalPages = 5;

        // When
        PagedTaskResponse response = new PagedTaskResponse(
            tasks,
            page,
            size,
            totalElements,
            totalPages
        );

        // Then
        assertEquals(tasks, response.tasks());
        assertEquals(page, response.page());
        assertEquals(size, response.size());
        assertEquals(totalElements, response.totalElements());
        assertEquals(totalPages, response.totalPages());
    }
}