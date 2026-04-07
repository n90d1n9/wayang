package tech.kayys.wayang.hitl.repository;

import tech.kayys.wayang.hitl.domain.HumanTaskStatus;

import java.time.Instant;
import java.util.List;

/**
 * TaskQueryFilter - Filter for querying tasks
 */
public class TaskQueryFilter {
    private String tenantId;
    private String assigneeIdentifier;
    private List<HumanTaskStatus> statuses;
    private String taskType;
    private Integer minPriority;
    private Integer maxPriority;
    private Instant dueBefore;
    private Instant dueAfter;
    private Instant createdBefore;
    private Instant createdAfter;
    private Boolean overdue;
    private Boolean escalated;

    // Pagination
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private boolean sortAscending = false;

    // Getters and setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getAssigneeIdentifier() { return assigneeIdentifier; }
    public void setAssigneeIdentifier(String assigneeIdentifier) {
        this.assigneeIdentifier = assigneeIdentifier;
    }

    public List<HumanTaskStatus> getStatuses() { return statuses; }
    public void setStatuses(List<HumanTaskStatus> statuses) { this.statuses = statuses; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public Integer getMinPriority() { return minPriority; }
    public void setMinPriority(Integer minPriority) { this.minPriority = minPriority; }

    public Integer getMaxPriority() { return maxPriority; }
    public void setMaxPriority(Integer maxPriority) { this.maxPriority = maxPriority; }

    public Instant getDueBefore() { return dueBefore; }
    public void setDueBefore(Instant dueBefore) { this.dueBefore = dueBefore; }

    public Instant getDueAfter() { return dueAfter; }
    public void setDueAfter(Instant dueAfter) { this.dueAfter = dueAfter; }

    public Instant getCreatedBefore() { return createdBefore; }
    public void setCreatedBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Instant getCreatedAfter() { return createdAfter; }
    public void setCreatedAfter(Instant createdAfter) { this.createdAfter = createdAfter; }

    public Boolean getOverdue() { return overdue; }
    public void setOverdue(Boolean overdue) { this.overdue = overdue; }

    public Boolean getEscalated() { return escalated; }
    public void setEscalated(Boolean escalated) { this.escalated = escalated; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public boolean isSortAscending() { return sortAscending; }
    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }
}