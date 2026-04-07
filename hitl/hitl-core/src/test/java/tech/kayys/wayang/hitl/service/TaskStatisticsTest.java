package tech.kayys.wayang.hitl.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskStatisticsTest {

    @Test
    void testTaskStatisticsRecordCreation() {
        // Given
        long activeTasks = 5L;
        long completedToday = 10L;
        long overdueTasks = 2L;

        // When
        TaskStatistics statistics = new TaskStatistics(activeTasks, completedToday, overdueTasks);

        // Then
        assertEquals(activeTasks, statistics.activeTasks());
        assertEquals(completedToday, statistics.completedToday());
        assertEquals(overdueTasks, statistics.overdueTasks());
    }

    @Test
    void testTaskStatisticsWithZeroValues() {
        // Given
        long activeTasks = 0L;
        long completedToday = 0L;
        long overdueTasks = 0L;

        // When
        TaskStatistics statistics = new TaskStatistics(activeTasks, completedToday, overdueTasks);

        // Then
        assertEquals(activeTasks, statistics.activeTasks());
        assertEquals(completedToday, statistics.completedToday());
        assertEquals(overdueTasks, statistics.overdueTasks());
    }

    @Test
    void testTaskStatisticsWithLargeValues() {
        // Given
        long activeTasks = 999999L;
        long completedToday = 888888L;
        long overdueTasks = 777777L;

        // When
        TaskStatistics statistics = new TaskStatistics(activeTasks, completedToday, overdueTasks);

        // Then
        assertEquals(activeTasks, statistics.activeTasks());
        assertEquals(completedToday, statistics.completedToday());
        assertEquals(overdueTasks, statistics.overdueTasks());
    }

    @Test
    void testTaskStatisticsEquality() {
        // Given
        TaskStatistics stats1 = new TaskStatistics(5L, 10L, 2L);
        TaskStatistics stats2 = new TaskStatistics(5L, 10L, 2L);
        TaskStatistics stats3 = new TaskStatistics(3L, 8L, 1L);

        // Then
        assertEquals(stats1, stats2);
        assertNotEquals(stats1, stats3);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void testTaskStatisticsToString() {
        // Given
        TaskStatistics statistics = new TaskStatistics(5L, 10L, 2L);

        // When
        String toStringResult = statistics.toString();

        // Then
        assertTrue(toStringResult.contains("TaskStatistics"));
        assertTrue(toStringResult.contains("activeTasks=5"));
        assertTrue(toStringResult.contains("completedToday=10"));
        assertTrue(toStringResult.contains("overdueTasks=2"));
    }
}