package tech.kayys.wayang.hitl.repository;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "human_task_audit", indexes = {
    @Index(name = "idx_audit_task", columnList = "task_id, timestamp"),
    @Index(name = "idx_audit_user", columnList = "performed_by, timestamp")
})
public class TaskAuditEntity extends PanacheEntity {

    @Column(name = "entry_id", nullable = false)
    public String entryId;

    @Column(name = "task_id", nullable = false)
    public String taskId;

    @Column(name = "action", nullable = false)
    public String action;

    @Column(name = "details", columnDefinition = "TEXT")
    public String details;

    @Column(name = "performed_by")
    public String performedBy;

    @Column(name = "timestamp", nullable = false)
    public Instant timestamp;

    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata;
}