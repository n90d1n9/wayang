package tech.kayys.wayang.prompt.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.prompt.core.PromptTemplate.TemplateStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * PromptRegistry — ApplicationScoped service managing template lifecycle.
 * ============================================================================
 *
 * Responsibilities:
 * • CRUD operations on {@link PromptTemplate}s, scoped by tenant.
 * • Status-transition enforcement via the {@link TemplateStatus} FSM.
 * • Publish-gate validation (blocks publishing if warnings remain).
 * • Two resolution modes exposed to the engine:
 * – {@link #resolveLatest(String, String)} – latest PUBLISHED version.
 * – {@link #resolvePinned(String, String, String)} – exact version.
 * • In-memory LRU cache for hot-path reads; invalidated on any write.
 *
 * Multi-tenancy:
 * • All queries are scoped by {@code tenantId}.
 * • Platform built-ins ({@code tenantId == null}) are visible to every
 * tenant but are read-only (mutations are rejected).
 *
 * Persistence:
 * Delegated entirely to {@link PromptTemplateRepository}, which is the
 * contract for the Hibernate Reactive + Panache implementation.
 *
 * Thread safety:
 * The cache is a {@link ConcurrentHashMap}. All repository calls return
 * {@link Uni<T>} — the service never blocks the event loop.
 */
@ApplicationScoped
public class PromptRegistry {

    private static final Logger LOG = Logger.getLogger(PromptRegistry.class);

    @Inject
    PromptTemplateRepository repository;

    // ------------------------------------------------------------------
    // In-memory cache (key = "tenantId:id:version")
    // ------------------------------------------------------------------
    private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    /**
     * Persists a new template. The status is forced to {@link TemplateStatus#DRAFT}
     * regardless of what the caller supplied — new templates always start as
     * drafts.
     *
     * @param template the template to save
     * @return the persisted template (with status forced to DRAFT)
     */
    public Uni<PromptTemplate> create(PromptTemplate template) {
        // Force DRAFT status
        PromptTemplate draft = new PromptTemplate(
                template.getTemplateId(),
                template.getName(),
                template.getDescription(),
                template.getTenantId(),
                template.getActiveVersion(),
                TemplateStatus.DRAFT,
                template.getTags(),
                template.getVersions(),
                template.getVariableDefinitions(),
                template.getCreatedBy(),
                template.getCreatedAt(),
                template.getUpdatedBy(),
                template.getUpdatedAt(),
                template.getMetadata());

        LOG.infof("Creating template: %s (tenant=%s)",
                draft.getTemplateId(), draft.getTenantId());

        return repository.save(draft)
                .onItem().invoke(saved -> cache.put(cacheKey(saved), saved));
    }

    /**
     * Returns the latest PUBLISHED version of a template visible to the
     * given tenant. Built-in templates (tenantId=null) are included.
     *
     * @throws TemplateNotFoundException when no published version exists
     */
    public Uni<PromptTemplate> resolveLatest(String templateId, String tenantId) {
        LOG.debugf("Resolving latest: %s (tenant=%s)", templateId, tenantId);

        return repository.findLatestPublished(templateId, tenantId)
                .onItem().ifNull().failWith(() -> new TemplateNotFoundException(templateId, null, tenantId))
                .onItem().invoke(t -> cache.put(cacheKey(t), t));
    }

    /**
     * Returns the exact version of a template visible to the given tenant.
     *
     * @throws TemplateNotFoundException when the version does not exist
     */
    public Uni<PromptTemplate> resolvePinned(String templateId, String version, String tenantId) {
        // Check cache first
        String key = cacheKey(tenantId, templateId, version);
        PromptTemplate cached = cache.get(key);
        if (cached != null) {
            LOG.debugf("Cache hit: %s", key);
            return Uni.createFrom().item(cached);
        }

        LOG.debugf("Resolving pinned: %s v%s (tenant=%s)", templateId, version, tenantId);

        return repository.findByIdAndVersion(templateId, version, tenantId)
                .onItem().ifNull().failWith(() -> new TemplateNotFoundException(templateId, version, tenantId))
                .onItem().invoke(t -> cache.put(cacheKey(t), t));
    }

    // ------------------------------------------------------------------
    // Status transitions
    // ------------------------------------------------------------------

    /**
     * Transitions a template from DRAFT → PUBLISHED.
     */
    public Uni<PromptTemplate> submitForReview(String templateId, String version, String tenantId) {
        return transitionStatus(templateId, version, tenantId, TemplateStatus.PUBLISHED);
    }

    /**
     * Transitions a template from DRAFT → PUBLISHED.
     *
     * Publish-gate: if the template still has validation warnings the
     * transition is rejected with an {@link IllegalStateException} whose
     * message lists the warnings.
     */
    public Uni<PromptTemplate> publish(String templateId, String version, String tenantId) {
        return resolvePinned(templateId, version, tenantId)
                .onItem().transformToUni(template -> {
                    // Publish-gate check
                    List<ValidationWarning> warnings = template.getValidationWarnings();
                    if (!warnings.isEmpty()) {
                        String details = warnings.stream()
                                .map(w -> "  [%s] %s: %s".formatted(w.getType(), w.getVariableName(), w.getMessage()))
                                .reduce("", (a, b) -> a + "\n" + b);
                        throw new IllegalStateException(
                                "Cannot publish template '%s' v%s — validation warnings remain:%s"
                                        .formatted(templateId, version, details));
                    }
                    return transitionStatus(templateId, version, tenantId, TemplateStatus.PUBLISHED);
                });
    }

    /**
     * Transitions a template to DEPRECATED (allowed from DRAFT or PUBLISHED).
     */
    public Uni<PromptTemplate> deprecate(String templateId, String version, String tenantId) {
        return transitionStatus(templateId, version, tenantId, TemplateStatus.DEPRECATED);
    }

    /**
     * Deletes a template. Only DRAFT or DEPRECATED templates may be deleted.
     */
    public Uni<Void> delete(String templateId, String version, String tenantId) {
        return resolvePinned(templateId, version, tenantId)
                .onItem().transformToUni(template -> {
                    if (template.getStatus() != TemplateStatus.DRAFT &&
                            template.getStatus() != TemplateStatus.DEPRECATED) {
                        throw new IllegalStateException(
                                "Only DRAFT or DEPRECATED templates may be deleted (current: %s)"
                                        .formatted(template.getStatus()));
                    }
                    cache.remove(cacheKey(template));
                    return repository.delete(templateId, version, tenantId);
                });
    }

    // ------------------------------------------------------------------
    // Listing / search
    // ------------------------------------------------------------------

    /** Lists all versions of a template visible to the tenant. */
    public Uni<List<PromptTemplate>> listVersions(String templateId, String tenantId) {
        return repository.findAllVersions(templateId, tenantId);
    }

    /** Searches templates by keyword (matched against id, body, metadata). */
    public Uni<List<PromptTemplate>> search(String keyword, String tenantId) {
        return repository.search(keyword, tenantId);
    }

    // ------------------------------------------------------------------
    // Cache management
    // ------------------------------------------------------------------

    /** Invalidates the entire in-memory cache. Call after bulk operations. */
    public void clearCache() {
        cache.clear();
        LOG.info("PromptRegistry cache cleared");
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private Uni<PromptTemplate> transitionStatus(
            String templateId, String version, String tenantId, TemplateStatus target) {
        return resolvePinned(templateId, version, tenantId)
                .onItem().transformToUni(template -> {
                    if (!template.getStatus().isValidTransition(target)) {
                        throw new IllegalStateException(
                                "Invalid status transition: %s → %s for template '%s' v%s"
                                        .formatted(template.getStatus(), target, templateId, version));
                    }
                    PromptTemplate updated = new PromptTemplate(
                            template.getTemplateId(),
                            template.getName(),
                            template.getDescription(),
                            template.getTenantId(),
                            template.getActiveVersion(),
                            target,
                            template.getTags(),
                            template.getVersions(),
                            template.getVariableDefinitions(),
                            template.getCreatedBy(),
                            template.getCreatedAt(),
                            template.getUpdatedBy(),
                            template.getUpdatedAt(),
                            template.getMetadata());

                    cache.put(cacheKey(updated), updated);
                    return repository.save(updated);
                });
    }

    private static String cacheKey(PromptTemplate t) {
        return cacheKey(t.getTenantId(), t.getTemplateId(), t.getActiveVersion());
    }

    private static String cacheKey(String tenantId, String id, String version) {
        return "%s:%s:%s".formatted(tenantId != null ? tenantId : "__builtin__", id, version);
    }

    // ============================================================
    // PromptTemplateRepository (contract for Panache impl)
    // ============================================================
    /**
     * Repository contract. The concrete implementation uses Hibernate
     * Reactive + Panache and maps to a {@code prompt_templates} table.
     *
     * All methods return {@link Uni<T>} — never block.
     */
    public interface PromptTemplateRepository {
        /** Upserts a template (insert or update by id+version). */
        Uni<PromptTemplate> save(PromptTemplate template);

        /** Finds the latest PUBLISHED version visible to the tenant. */
        Uni<PromptTemplate> findLatestPublished(String templateId, String tenantId);

        /** Finds an exact version visible to the tenant. */
        Uni<PromptTemplate> findByIdAndVersion(String templateId, String version, String tenantId);

        /** Lists all versions of a template. */
        Uni<List<PromptTemplate>> findAllVersions(String templateId, String tenantId);

        /** Keyword search across templates. */
        Uni<List<PromptTemplate>> search(String keyword, String tenantId);

        /** Deletes a specific version. */
        Uni<Void> delete(String templateId, String version, String tenantId);
    }

    // ============================================================
    // TemplateNotFoundException
    // ============================================================
    /**
     * Thrown when a template resolution (latest or pinned) finds no match.
     * Carries the identity fields for structured error mapping.
     */
    public static class TemplateNotFoundException extends RuntimeException {
        private final String templateId;
        private final String version; // null when resolving latest
        private final String tenantId;

        public TemplateNotFoundException(String templateId, String version, String tenantId) {
            super("Template not found: id='%s', version='%s', tenant='%s'"
                    .formatted(templateId, version != null ? version : "latest", tenantId));
            this.templateId = templateId;
            this.version = version;
            this.tenantId = tenantId;
        }

        public String getTemplateId() {
            return templateId;
        }

        public String getVersion() {
            return version;
        }

        public String getTenantId() {
            return tenantId;
        }
    }
}
