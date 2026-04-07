package tech.kayys.wayang.hitl.repository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import tech.kayys.wayang.hitl.domain.HumanTaskStatus;

import java.time.Instant;
import java.util.List;

class TaskQueryFilterTest {

    @Test
    void shouldSetAndGetTenantId() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        String tenantId = "tenant-001";

        // When
        filter.setTenantId(tenantId);

        // Then
        assertEquals(tenantId, filter.getTenantId());
    }

    @Test
    void shouldSetAndGetAssigneeIdentifier() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        String assigneeIdentifier = "user1";

        // When
        filter.setAssigneeIdentifier(assigneeIdentifier);

        // Then
        assertEquals(assigneeIdentifier, filter.getAssigneeIdentifier());
    }

    @Test
    void shouldSetAndGetStatuses() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        List<HumanTaskStatus> statuses = List.of(HumanTaskStatus.ASSIGNED, HumanTaskStatus.IN_PROGRESS);

        // When
        filter.setStatuses(statuses);

        // Then
        assertEquals(statuses, filter.getStatuses());
    }

    @Test
    void shouldSetAndGetTaskType() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        String taskType = "approval";

        // When
        filter.setTaskType(taskType);

        // Then
        assertEquals(taskType, filter.getTaskType());
    }

    @Test
    void shouldSetAndGetMinPriority() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        Integer minPriority = 2;

        // When
        filter.setMinPriority(minPriority);

        // Then
        assertEquals(minPriority, filter.getMinPriority());
    }

    @Test
    void shouldSetAndGetMaxPriority() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        Integer maxPriority = 4;

        // When
        filter.setMaxPriority(maxPriority);

        // Then
        assertEquals(maxPriority, filter.getMaxPriority());
    }

    @Test
    void shouldSetAndGetDueDates() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        Instant dueBefore = Instant.now();
        Instant dueAfter = Instant.now().minusSeconds(100);

        // When
        filter.setDueBefore(dueBefore);
        filter.setDueAfter(dueAfter);

        // Then
        assertEquals(dueBefore, filter.getDueBefore());
        assertEquals(dueAfter, filter.getDueAfter());
    }

    @Test
    void shouldSetAndGetCreatedDates() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        Instant createdBefore = Instant.now();
        Instant createdAfter = Instant.now().minusSeconds(100);

        // When
        filter.setCreatedBefore(createdBefore);
        filter.setCreatedAfter(createdAfter);

        // Then
        assertEquals(createdBefore, filter.getCreatedBefore());
        assertEquals(createdAfter, filter.getCreatedAfter());
    }

    @Test
    void shouldSetAndGetOverdueFlag() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        Boolean overdue = true;

        // When
        filter.setOverdue(overdue);

        // Then
        assertEquals(overdue, filter.getOverdue());
    }

    @Test
    void shouldSetAndGetEscalatedFlag() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        Boolean escalated = true;

        // When
        filter.setEscalated(escalated);

        // Then
        assertEquals(escalated, filter.getEscalated());
    }

    @Test
    void shouldSetAndGetPaginationParams() {
        // Given
        TaskQueryFilter filter = new TaskQueryFilter();
        int page = 1;
        int size = 50;
        String sortBy = "priority";
        boolean sortAscending = false;

        // When
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortAscending(sortAscending);

        // Then
        assertEquals(page, filter.getPage());
        assertEquals(size, filter.getSize());
        assertEquals(sortBy, filter.getSortBy());
        assertEquals(sortAscending, filter.isSortAscending());
    }
}