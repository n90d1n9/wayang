package tech.kayys.wayang.hitl.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskStatisticsTest {

    @Test
    void shouldCreateTaskStatisticsSuccessfully() {
        // Given
        long activeTasks = 5L;
        long completedToday = 10L;
        long overdueTasks = 2L;

        // When
        TaskStatistics stats = new TaskStatistics(
            activeTasks,
            completedToday,
            overdueTasks
        );

        // Then
        assertEquals(activeTasks, stats.activeTasks());
        assertEquals(completedToday, stats.completedToday());
        assertEquals(overdueTasks, stats.overdueTasks());
    }
}