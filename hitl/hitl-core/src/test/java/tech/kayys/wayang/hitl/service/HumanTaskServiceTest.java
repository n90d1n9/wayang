package tech.kayys.wayang.hitl.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.hitl.repository.HumanTaskRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanTaskServiceTest {

    @Mock
    private HumanTaskRepository repository;

    private HumanTaskService humanTaskService;

    @BeforeEach
    void setUp() {
        humanTaskService = new HumanTaskService();
        // Use reflection to inject the mock repository
        try {
            java.lang.reflect.Field repoField = HumanTaskService.class.getDeclaredField("repository");
            repoField.setAccessible(true);
            repoField.set(humanTaskService, repository);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mock repository", e);
        }
    }

    @Test
    void testGetUserTaskStatistics() {
        // Given
        String userId = "user123";
        String tenantId = "tenant1";
        long activeTaskCount = 5L;

        when(repository.countActiveTasksForUser(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(activeTaskCount));

        // When
        Uni<TaskStatistics> statisticsUni = humanTaskService.getUserTaskStatistics(userId, tenantId);
        
        // Then
        TaskStatistics statistics = statisticsUni.await().indefinitely();
        assertEquals(activeTaskCount, statistics.activeTasks());
        assertEquals(0L, statistics.completedToday()); // Expected default value
        assertEquals(0L, statistics.overdueTasks());   // Expected default value
    }

    @Test
    void testGetUserTaskStatisticsWithZeroTasks() {
        // Given
        String userId = "user456";
        String tenantId = "tenant2";

        when(repository.countActiveTasksForUser(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(0L));

        // When
        Uni<TaskStatistics> statisticsUni = humanTaskService.getUserTaskStatistics(userId, tenantId);
        
        // Then
        TaskStatistics statistics = statisticsUni.await().indefinitely();
        assertEquals(0L, statistics.activeTasks());
        assertEquals(0L, statistics.completedToday());
        assertEquals(0L, statistics.overdueTasks());
    }
}