package tech.kayys.wayang.hitl.repository;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import tech.kayys.wayang.hitl.domain.AssigneeType;

import java.time.Instant;

@Entity
@Table(name = "human_task_assignments", indexes = {
    @Index(name = "idx_hta_task", columnList = "task_id"),
    @Index(name = "idx_hta_assignee", columnList = "assignee_identifier")
})
public class TaskAssignmentEntity extends PanacheEntity {

    @Column(name = "task_id", nullable = false)
    public String taskId;

    @Column(name = "assignee_type", nullable = false)
    public AssigneeType assigneeType;

    @Column(name = "assignee_identifier", nullable = false)
    public String assigneeIdentifier;

    @Column(name = "assigned_by")
    public String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    public Instant assignedAt;

    @Column(name = "delegation_reason")
    public String delegationReason;

    @Column(name = "sequence_number")
    public int sequenceNumber;
}