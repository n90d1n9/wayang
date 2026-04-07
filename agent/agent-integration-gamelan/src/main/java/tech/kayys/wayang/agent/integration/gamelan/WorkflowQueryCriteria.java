package tech.kayys.wayang.agent.integration.gamelan;

import java.util.*;

/**
 * Criteria for querying workflow runs.
 *
 * Provides a fluent API for building complex queries to find specific
 * workflow runs by workflow ID, status, labels, time range, etc.
 */
public class WorkflowQueryCriteria {

    private String workflowId;
    private String status;  // CREATED, RUNNING, SUSPENDED, COMPLETED, FAILED, CANCELLED
    private Map<String, String> labels;
    private Long createdAfterMs;
    private Long createdBeforeMs;
    private Integer limit;
    private Integer offset;
    private String orderBy;  // createdAt, duration, status, etc.
    private boolean ascending;

    /**
     * Creates empty query criteria (matches all runs).
     */
    public WorkflowQueryCriteria() {
        this.labels = new HashMap<>();
        this.limit = 100;
        this.offset = 0;
        this.orderBy = "createdAt";
        this.ascending = false;
    }

    // Fluent setters

    /**
     * Filters by workflow ID.
     */
    public WorkflowQueryCriteria withWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    /**
     * Filters by execution status.
     */
    public WorkflowQueryCriteria withStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Adds a label filter (key-value pair).
     */
    public WorkflowQueryCriteria withLabel(String key, String value) {
        if (this.labels == null) {
            this.labels = new HashMap<>();
        }
        this.labels.put(key, value);
        return this;
    }

    /**
     * Adds multiple label filters.
     */
    public WorkflowQueryCriteria withLabels(Map<String, String> labels) {
        if (this.labels == null) {
            this.labels = new HashMap<>();
        }
        this.labels.putAll(labels);
        return this;
    }

    /**
     * Filters for runs created after the given time (milliseconds since epoch).
     */
    public WorkflowQueryCriteria createdAfter(Long timestampMs) {
        this.createdAfterMs = timestampMs;
        return this;
    }

    /**
     * Filters for runs created before the given time (milliseconds since epoch).
     */
    public WorkflowQueryCriteria createdBefore(Long timestampMs) {
        this.createdBeforeMs = timestampMs;
        return this;
    }

    /**
     * Sets maximum results limit (default: 100).
     */
    public WorkflowQueryCriteria limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets result offset for pagination.
     */
    public WorkflowQueryCriteria offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Orders results by field (createdAt, duration, status, etc.).
     */
    public WorkflowQueryCriteria orderBy(String field) {
        this.orderBy = field;
        return this;
    }

    /**
     * Sets sort order to ascending.
     */
    public WorkflowQueryCriteria ascending() {
        this.ascending = true;
        return this;
    }

    /**
     * Sets sort order to descending (default).
     */
    public WorkflowQueryCriteria descending() {
        this.ascending = false;
        return this;
    }

    // Getters

    public String getWorkflowId() {
        return workflowId;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, String> getLabels() {
        return labels != null ? labels : Map.of();
    }

    public Long getCreatedAfterMs() {
        return createdAfterMs;
    }

    public Long getCreatedBeforeMs() {
        return createdBeforeMs;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public boolean isAscending() {
        return ascending;
    }

    @Override
    public String toString() {
        return "WorkflowQueryCriteria{" +
                "workflowId='" + workflowId + '\'' +
                ", status='" + status + '\'' +
                ", labels=" + labels +
                ", createdAfterMs=" + createdAfterMs +
                ", createdBeforeMs=" + createdBeforeMs +
                ", limit=" + limit +
                ", offset=" + offset +
                ", orderBy='" + orderBy + '\'' +
                ", ascending=" + ascending +
                '}';
    }
}
