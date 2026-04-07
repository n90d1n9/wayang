package tech.kayys.wayang.prompt.store;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import tech.kayys.wayang.prompt.core.PromptTemplate;

/**
 * ============================================================================
 * PromptTemplateEntity — JPA / Hibernate entity for prompt_templates table.
 * ============================================================================
 *
 * The entity stores the full PromptTemplate as a JSONB column
 * ({@code definition})
 * for maximum schema flexibility, while also exposing the most frequently
 * queried fields (templateId, tenantId, status, activeVersion) as indexed
 * scalar columns for efficient filtering.
 *
 * This mirrors the pattern used by WorkflowEntity in the platform codebase:
 * scalar index columns + JSONB payload.
 */
@Entity
@Table(name = "prompt_templates", schema = "wayang", indexes = {
        @Index(name = "idx_pt_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pt_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_pt_tenant_id", columnList = "tenant_id, template_id", unique = true)
})
public class PromptTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false, length = 255)
    private String templateId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "active_version", length = 32)
    private String activeVersion;

    /**
     * Full PromptTemplate serialised as JSONB.
     * This is the source of truth; the scalar columns are denormalisations
     * for query performance.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition", columnDefinition = "jsonb", nullable = false)
    private PromptTemplate definition;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // -----------------------------------------------------------------------
    // Conversion helpers
    // -----------------------------------------------------------------------

    public static PromptTemplateEntity fromDomain(PromptTemplate template) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.templateId = template.getTemplateId();
        entity.tenantId = template.getTenantId();
        entity.name = template.getName();
        entity.status = template.getStatus().name();
        entity.activeVersion = template.getActiveVersion();
        entity.definition = template;
        entity.createdAt = template.getCreatedAt();
        entity.updatedAt = template.getUpdatedAt();
        return entity;
    }

    public PromptTemplate toDomain() {
        return definition;
    }

    // -----------------------------------------------------------------------
    // Getters / setters (required by JPA)
    // -----------------------------------------------------------------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String v) {
        this.templateId = v;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String v) {
        this.tenantId = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        this.name = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public String getActiveVersion() {
        return activeVersion;
    }

    public void setActiveVersion(String v) {
        this.activeVersion = v;
    }

    public PromptTemplate getDefinition() {
        return definition;
    }

    public void setDefinition(PromptTemplate v) {
        this.definition = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant v) {
        this.createdAt = v;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        this.updatedAt = v;
    }
}
