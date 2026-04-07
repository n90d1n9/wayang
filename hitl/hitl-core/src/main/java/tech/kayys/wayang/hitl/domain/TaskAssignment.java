package tech.kayys.wayang.hitl.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * TaskAssignment - Who is assigned to the task
 */
public class TaskAssignment {
    private final AssigneeType assigneeType;
    private final String assigneeIdentifier;
    private final String assignedBy;
    private final Instant assignedAt;
    private final String delegationReason;

    private TaskAssignment(Builder builder) {
        this.assigneeType = builder.assigneeType;
        this.assigneeIdentifier = builder.assigneeIdentifier;
        this.assignedBy = builder.assignedBy;
        this.assignedAt = builder.assignedAt;
        this.delegationReason = builder.delegationReason;
    }

    public boolean canClaim(String userId) {
        return switch (assigneeType) {
            case USER -> assigneeIdentifier.equals(userId);
            case GROUP, ROLE -> true; // Members of group/role can claim
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AssigneeType assigneeType;
        private String assigneeIdentifier;
        private String assignedBy;
        private Instant assignedAt;
        private String delegationReason;

        public Builder assigneeType(AssigneeType type) {
            this.assigneeType = type;
            return this;
        }

        public Builder assigneeIdentifier(String identifier) {
            this.assigneeIdentifier = identifier;
            return this;
        }

        public Builder assignedBy(String assignedBy) {
            this.assignedBy = assignedBy;
            return this;
        }

        public Builder assignedAt(Instant assignedAt) {
            this.assignedAt = assignedAt;
            return this;
        }

        public Builder delegationReason(String reason) {
            this.delegationReason = reason;
            return this;
        }

        public TaskAssignment build() {
            Objects.requireNonNull(assigneeType);
            Objects.requireNonNull(assigneeIdentifier);
            if (assignedAt == null) {
                assignedAt = Instant.now();
            }
            return new TaskAssignment(this);
        }
    }

    public AssigneeType getAssigneeType() {
        return assigneeType;
    }

    public String getAssigneeIdentifier() {
        return assigneeIdentifier;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public String getDelegationReason() {
        return delegationReason;
    }
}