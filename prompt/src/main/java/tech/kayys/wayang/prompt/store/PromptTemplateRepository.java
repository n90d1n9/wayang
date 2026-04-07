package tech.kayys.wayang.prompt.store;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.prompt.core.PromptTemplate;

import java.util.List;

/**
 * ============================================================================
 * PromptTemplateRepository — SPI for PromptTemplate persistence.
 * ============================================================================
 *
 * Why an interface?
 * -----------------
 * The standalone / portable runtime cannot assume PostgreSQL.  It needs an
 * in-memory or SQLite-backed implementation.  By defining persistence as a
 * plain SPI, the registry layer is completely decoupled from the storage
 * technology.  The Quarkus CDI container picks the right bean at startup
 * based on the active profile.
 *
 * Tenant isolation contract
 * ----------- ------------
 * Every method that reads data MUST filter by tenantId.  The repository
 * implementation is the last line of defence before data leaves the system;
 * it must never return rows belonging to another tenant.
 *
 * Reactive contract
 * -----------------
 * All methods return Uni<T> to stay compatible with Quarkus Mutiny / Hibernate
 * Reactive.  Implementations that are inherently synchronous (in-memory, SQLite)
 * wrap their results in Uni.createFrom().item().
 */
public interface PromptTemplateRepository {

    /**
     * Persists or updates a PromptTemplate.  Upsert semantics:
     * if a template with the same (templateId, tenantId) exists, it is replaced.
     */
    Uni<PromptTemplate> save(PromptTemplate template);

    /**
     * Finds a template by its unique (templateId, tenantId) composite key.
     * Returns null (not an exception) if not found — the service layer decides
     * whether to throw.
     */
    Uni<PromptTemplate> findById(String templateId, String tenantId);

    /**
     * Lists all templates for a tenant, ordered by createdAt DESC, paginated.
     */
    Uni<List<PromptTemplate>> findByTenant(String tenantId, int page, int pageSize);

    /**
     * Deletes a template.  In practice this is rarely called; DEPRECATED is
     * the preferred lifecycle end-state for audit compliance.
     */
    Uni<Void> delete(String templateId, String tenantId);
}