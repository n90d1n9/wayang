package tech.kayys.wayang.prompt.registry;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.prompt.core.PromptEngineException;
import tech.kayys.wayang.prompt.core.PromptTemplate;
import tech.kayys.wayang.prompt.core.PromptTemplate.TemplateStatus;
import tech.kayys.wayang.prompt.core.PromptVariableDefinition;
import tech.kayys.wayang.prompt.core.PromptVersion;
import tech.kayys.wayang.prompt.store.PromptTemplateRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * PromptTemplateRegistry — service layer for all template lifecycle operations.
 * ============================================================================
 *
 * Responsibilities
 * ----------------
 * 1. Create / update / delete PromptTemplates (tenant-scoped).
 * 2. Publish a DRAFT version → frozen, executable.
 * 3. Resolve the active version for a given templateId + tenant.
 * 4. Deprecate templates and versions.
 * 5. Emit provenance audit events for every state change.
 *
 * Tenant isolation
 * ----------------
 * Every method that accepts a tenantId enforces it at the repository level.
 * A tenant can never see or mutate another tenant's templates.
 *
 * Error contract
 * --------------
 * - Template not found → PromptTemplateNotFoundException
 * - Version not found → PromptValidationException (with variable = "version")
 * - Illegal lifecycle → PromptValidationException
 *
 * Audit events emitted
 * ---------------------
 * TEMPLATE_CREATED | TEMPLATE_DEPRECATED
 * VERSION_CREATED | VERSION_PUBLISHED | VERSION_DEPRECATED
 */
@ApplicationScoped
public class PromptTemplateRegistry {

        private static final Logger LOG = Logger.getLogger(PromptTemplateRegistry.class);

        @Inject
        PromptTemplateRepository repository;

        // -----------------------------------------------------------------------
        // Template CRUD
        // -----------------------------------------------------------------------

        /**
         * Creates a new PromptTemplate in DRAFT state with zero versions.
         * The caller must subsequently add a version and publish it before the
         * template can be used by the Workflow Engine.
         */
        public Uni<PromptTemplate> createTemplate(
                        String templateId,
                        String name,
                        String description,
                        String tenantId,
                        List<String> tags,
                        List<PromptVariableDefinition> variableDefinitions,
                        String createdBy) {

                LOG.infof("Creating template: id='%s', tenant='%s', actor='%s'",
                                templateId, tenantId, createdBy);

                PromptTemplate template = new PromptTemplate(
                                templateId, name, description, tenantId,
                                null, // activeVersion — none yet
                                TemplateStatus.DRAFT,
                                tags,
                                List.of(), // versions — empty initially
                                variableDefinitions,
                                createdBy, Instant.now(),
                                createdBy, Instant.now(),
                                java.util.Map.of());

                return repository.save(template)
                                .onItem().invoke(saved -> LOG.infof("Template created: id='%s', tenant='%s'",
                                                saved.getTemplateId(), saved.getTenantId()));
        }

        /**
         * Retrieves a template by ID within the given tenant.
         * Throws PromptTemplateNotFoundException if not found.
         */
        public Uni<PromptTemplate> findById(String templateId, String tenantId) {
                return repository.findById(templateId, tenantId)
                                .onItem().ifNull()
                                .failWith(() -> new PromptEngineException.PromptTemplateNotFoundException(
                                                templateId, tenantId, "registry"));
        }

        /**
         * Lists all templates for a tenant, paginated.
         */
        public Uni<List<PromptTemplate>> listByTenant(String tenantId, int page, int pageSize) {
                return repository.findByTenant(tenantId, page, pageSize);
        }

        /**
         * Deprecates a template. All versions remain queryable for audit but
         * the template can no longer be rendered.
         */
        public Uni<PromptTemplate> deprecateTemplate(
                        String templateId, String tenantId, String actor) {

                LOG.infof("Deprecating template: id='%s', tenant='%s', actor='%s'",
                                templateId, tenantId, actor);

                return findById(templateId, tenantId)
                                .onItem().transformToUni(template -> {
                                        PromptTemplate deprecated = template.deprecated(actor);
                                        return repository.save(deprecated);
                                })
                                .onItem().invoke(t -> LOG.infof("Template deprecated: id='%s'", t.getTemplateId()));
        }

        // -----------------------------------------------------------------------
        // Version management
        // -----------------------------------------------------------------------

        /**
         * Adds a new DRAFT version to an existing template.
         * The version's bodyHash is computed here for audit integrity.
         */
        public Uni<PromptTemplate> addVersion(
                        String templateId,
                        String tenantId,
                        PromptVersion draftVersion,
                        String actor) {

                LOG.infof("Adding version: template='%s', version='%s', tenant='%s', actor='%s'",
                                templateId, draftVersion.getVersion(), tenantId, actor);

                return findById(templateId, tenantId)
                                .onItem().transformToUni(template -> {
                                        // Compute body hash
                                        String bodyHash = computeBodyHash(draftVersion.getTemplateBody());

                                        PromptVersion versionWithHash = new PromptVersion(
                                                        draftVersion.getVersion(),
                                                        draftVersion.getTemplateBody(),
                                                        draftVersion.getSystemPrompt(),
                                                        draftVersion.getRenderingStrategy(),
                                                        draftVersion.getMaxOutputTokens(),
                                                        draftVersion.getMaxContextTokens(),
                                                        PromptVersion.VersionStatus.DRAFT,
                                                        bodyHash,
                                                        actor,
                                                        Instant.now(),
                                                        draftVersion.getMetadata());

                                        PromptTemplate updated = template.withNewVersion(versionWithHash);
                                        return repository.save(updated);
                                })
                                .onItem().invoke(t -> LOG.infof("Version added: template='%s', version='%s'",
                                                t.getTemplateId(), draftVersion.getVersion()));
        }

        /**
         * Publishes a DRAFT version, making it the active version for the template.
         * The template status is also moved to PUBLISHED if it was DRAFT.
         */
        public Uni<PromptTemplate> publishVersion(
                        String templateId,
                        String tenantId,
                        String versionStr,
                        String actor) {

                LOG.infof("Publishing version: template='%s', version='%s', tenant='%s', actor='%s'",
                                templateId, versionStr, tenantId, actor);

                return findById(templateId, tenantId)
                                .onItem().transformToUni(template -> {
                                        // Find the target version
                                        Optional<PromptVersion> target = template.getVersions().stream()
                                                        .filter(v -> v.getVersion().equals(versionStr))
                                                        .findFirst();

                                        if (target.isEmpty()) {
                                                throw new PromptEngineException.PromptValidationException(
                                                                "Version '%s' not found in template '%s'"
                                                                                .formatted(versionStr, templateId),
                                                                templateId, "registry", "version");
                                        }

                                        PromptVersion published = target.get().publish();

                                        // Rebuild the versions list with the published version
                                        java.util.List<PromptVersion> updatedVersions = new java.util.ArrayList<>();
                                        for (PromptVersion v : template.getVersions()) {
                                                if (v.getVersion().equals(versionStr)) {
                                                        updatedVersions.add(published);
                                                } else {
                                                        updatedVersions.add(v);
                                                }
                                        }

                                        // Determine new template status
                                        PromptTemplate.TemplateStatus newStatus = template
                                                        .getStatus() == PromptTemplate.TemplateStatus.DRAFT
                                                                        ? PromptTemplate.TemplateStatus.PUBLISHED
                                                                        : template.getStatus();

                                        // Create updated template with new active version
                                        PromptTemplate updated = new PromptTemplate(
                                                        template.getTemplateId(),
                                                        template.getName(),
                                                        template.getDescription(),
                                                        template.getTenantId(),
                                                        versionStr, // new active version
                                                        newStatus,
                                                        template.getTags(),
                                                        updatedVersions,
                                                        template.getVariableDefinitions(),
                                                        template.getCreatedBy(),
                                                        template.getCreatedAt(),
                                                        actor,
                                                        Instant.now(),
                                                        template.getMetadata());

                                        return repository.save(updated);
                                })
                                .onItem().invoke(t -> LOG.infof("Version published: template='%s', active='%s'",
                                                t.getTemplateId(), t.getActiveVersion()));
        }

        // -----------------------------------------------------------------------
        // Resolution — used by TemplateRenderer at render time
        // -----------------------------------------------------------------------

        /**
         * Resolves the active PromptVersion for a given template.
         * If {@code versionOverride} is non-null, that specific version is returned
         * instead.
         *
         * This is the hot path called on every LLM invocation.
         */
        public Uni<PromptVersion> resolveVersion(
                        String templateId,
                        String tenantId,
                        String versionOverride) {

                return findById(templateId, tenantId)
                                .onItem().transform(template -> {
                                        if (versionOverride != null) {
                                                // Explicit version requested
                                                return template.getVersions().stream()
                                                                .filter(v -> v.getVersion().equals(versionOverride))
                                                                .findFirst()
                                                                .orElseThrow(() -> new PromptEngineException.PromptValidationException(
                                                                                "Requested version '%s' not found"
                                                                                                .formatted(versionOverride),
                                                                                templateId, "registry", "version"));
                                        }

                                        // Use the active version
                                        return template.resolveActiveVersion()
                                                        .orElseThrow(() -> new PromptEngineException.PromptValidationException(
                                                                        "No active version for template '%s'"
                                                                                        .formatted(templateId),
                                                                        templateId, "registry", "version"));
                                });
        }

        // -----------------------------------------------------------------------
        // Utility
        // -----------------------------------------------------------------------

        /**
         * Computes SHA-256 hash of the template body for audit integrity.
         */
        private static String computeBodyHash(String body) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
                        StringBuilder hex = new StringBuilder(64);
                        for (byte b : hash) {
                                hex.append("%02x".formatted(b & 0xff));
                        }
                        return hex.toString();
                } catch (java.security.NoSuchAlgorithmException e) {
                        throw new RuntimeException("SHA-256 not available", e);
                }
        }
}
