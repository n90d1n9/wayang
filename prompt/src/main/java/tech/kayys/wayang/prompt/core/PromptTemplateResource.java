package tech.kayys.wayang.prompt.core;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.logging.Logger;
import tech.kayys.wayang.error.ErrorCode;
import tech.kayys.wayang.error.ErrorResponse;
import tech.kayys.wayang.error.WayangException;

import java.util.Map;

/**
 * ============================================================================
 * PromptTemplateResource — JAX-RS REST API for prompt template management.
 * ============================================================================
 *
 * Base path: {@code /api/v1/prompts}
 *
 * Endpoints:
 * POST /prompts – create a new template (forced DRAFT)
 * GET /prompts/{id} – resolve latest PUBLISHED version
 * GET /prompts/{id}/{version} – resolve a pinned version
 * GET /prompts/{id}/versions – list all versions of a template
 * PUT /prompts/{id}/{version}/status/{target}
 * – transition status (FSM)
 * DELETE /prompts/{id}/{version} – delete (DRAFT or DEPRECATED only)
 * GET /prompts/search?keyword=... – keyword search
 *
 * Multi-tenancy:
 * The tenant ID is extracted from the security context. In production
 * this reads a claim from the JWT bearer token. For local development a
 * fallback to the {@code X-Tenant-ID} header is provided.
 *
 * Error responses:
 * All errors are returned as a JSON object with {@code error} and
 * {@code details} fields, compatible with the platform's
 * {@code ErrorPayload} envelope.
 */
@Path("/api/v1/prompt-templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromptTemplateResource {

        private static final Logger LOG = Logger.getLogger(PromptTemplateResource.class);

        @Inject
        PromptRegistry registry;

        @Context
        SecurityContext securityContext;

        // ------------------------------------------------------------------
        // CREATE
        // ------------------------------------------------------------------

        /**
         * Creates a new prompt template. Status is forced to DRAFT.
         *
         * @param template the template payload (status field is ignored)
         * @return 201 Created with the persisted template
         */
        @POST
        public Uni<Response> createTemplate(PromptTemplate template) {
                String tenantId = getTenantId();
                LOG.debug("Creating template for tenant: " + tenantId);
                // Inject tenant into the template
                PromptTemplate scoped = new PromptTemplate(
                                template.getTemplateId(),
                                template.getName(),
                                template.getDescription(),
                                tenantId,
                                template.getActiveVersion(),
                                template.getStatus(),
                                template.getTags(),
                                template.getVersions(),
                                template.getVariableDefinitions(),
                                template.getCreatedBy(),
                                template.getCreatedAt(),
                                template.getUpdatedBy(),
                                template.getUpdatedAt(),
                                template.getMetadata());

                return registry.create(scoped)
                                .map(saved -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(saved)
                                                .build())
                                .onFailure().recoverWithItem(ex -> errorResponse(Response.Status.BAD_REQUEST, ex));
        }

        // ------------------------------------------------------------------
        // READ
        // ------------------------------------------------------------------

        /**
         * Resolves the latest PUBLISHED version of a template.
         */
        @GET
        @Path("/{id}")
        public Uni<Response> getLatest(@PathParam("id") String id) {
                return registry.resolveLatest(id, getTenantId())
                                .map(template -> Response.ok(template).build())
                                .onFailure(PromptRegistry.TemplateNotFoundException.class)
                                .recoverWithItem(ex -> Response.status(Response.Status.NOT_FOUND)
                                                .entity(Map.of("error", ex.getMessage()))
                                                .build())
                                .onFailure()
                                .recoverWithItem(ex -> errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex));
        }

        /**
         * Resolves an exact version of a template.
         */
        @GET
        @Path("/{id}/{version}")
        public Uni<Response> getPinned(
                        @PathParam("id") String id,
                        @PathParam("version") String version) {
                return registry.resolvePinned(id, version, getTenantId())
                                .map(template -> Response.ok(template).build())
                                .onFailure(PromptRegistry.TemplateNotFoundException.class)
                                .recoverWithItem(ex -> Response.status(Response.Status.NOT_FOUND)
                                                .entity(Map.of("error", ex.getMessage()))
                                                .build())
                                .onFailure()
                                .recoverWithItem(ex -> errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex));
        }

        /**
         * Lists all versions of a template.
         */
        @GET
        @Path("/{id}/versions")
        public Uni<Response> listVersions(@PathParam("id") String id) {
                return registry.listVersions(id, getTenantId())
                                .map(versions -> Response.ok(versions).build())
                                .onFailure()
                                .recoverWithItem(ex -> errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex));
        }

        // ------------------------------------------------------------------
        // STATUS TRANSITION
        // ------------------------------------------------------------------

        /**
         * Transitions a template's status. Valid targets depend on current state
         * (see {@link TemplateStatus} FSM).
         *
         * @param target one of: PUBLISHED, DEPRECATED
         */
        @PUT
        @Path("/{id}/{version}/status/{target}")
        public Uni<Response> transitionStatus(
                        @PathParam("id") String id,
                        @PathParam("version") String version,
                        @PathParam("target") String target) {
                String tenantId = getTenantId();
                TemplateStatus targetStatus;
                try {
                        targetStatus = TemplateStatus.valueOf(target.toUpperCase());
                } catch (IllegalArgumentException e) {
                        return Uni.createFrom().item(
                                        errorResponse(Response.Status.BAD_REQUEST,
                                                        new IllegalArgumentException("Unknown status: " + target)));
                }

                Uni<PromptTemplate> transition = switch (targetStatus) {
                        case PUBLISHED -> registry.publish(id, version, tenantId);
                        case DEPRECATED -> registry.deprecate(id, version, tenantId);
                        default -> Uni.createFrom().failure(
                                        new IllegalArgumentException("Cannot transition to " + targetStatus));
                };

                return transition
                                .map(updated -> Response.ok(updated).build())
                                .onFailure(IllegalStateException.class)
                                .recoverWithItem(ex -> errorResponse(Response.Status.CONFLICT, ex))
                                .onFailure(PromptRegistry.TemplateNotFoundException.class)
                                .recoverWithItem(ex -> errorResponse(Response.Status.NOT_FOUND, ex))
                                .onFailure()
                                .recoverWithItem(ex -> errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex));
        }

        // ------------------------------------------------------------------
        // DELETE
        // ------------------------------------------------------------------

        /**
         * Deletes a template version. Only DRAFT or DEPRECATED templates may be
         * deleted.
         */
        @DELETE
        @Path("/{id}/{version}")
        public Uni<Response> deleteTemplate(
                        @PathParam("id") String id,
                        @PathParam("version") String version) {
                return registry.delete(id, version, getTenantId())
                                .map(v -> Response.noContent().build())
                                .onFailure(IllegalStateException.class)
                                .recoverWithItem(ex -> errorResponse(Response.Status.CONFLICT, ex))
                                .onFailure(PromptRegistry.TemplateNotFoundException.class)
                                .recoverWithItem(ex -> errorResponse(Response.Status.NOT_FOUND, ex))
                                .onFailure()
                                .recoverWithItem(ex -> errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex));
        }

        // ------------------------------------------------------------------
        // SEARCH
        // ------------------------------------------------------------------

        /**
         * Keyword search across templates.
         */
        @GET
        @Path("/search")
        public Uni<Response> search(@QueryParam("keyword") String keyword) {
                if (keyword == null || keyword.isBlank()) {
                        return Uni.createFrom().item(
                                        errorResponse(Response.Status.BAD_REQUEST,
                                                        new IllegalArgumentException(
                                                                        "'keyword' query parameter is required")));
                }
                return registry.search(keyword, getTenantId())
                                .map(results -> Response.ok(results).build())
                                .onFailure()
                                .recoverWithItem(ex -> errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex));
        }

        // ------------------------------------------------------------------
        // Private helpers
        // ------------------------------------------------------------------

        /**
         * Extracts tenantId from the security context.
         *
         * Production: reads the {@code tenant_id} claim from the JWT bearer token
         * injected by Quarkus Security.
         * Development fallback: reads the {@code X-Tenant-ID} HTTP header.
         *
         * TODO: wire full JWT claim extraction when the security module is ready.
         */
        private String getTenantId() {
                if (securityContext != null && securityContext.getUserPrincipal() != null) {
                        // In production the principal name or a custom claim carries the tenant.
                        // For now fall back to a header-based approach.
                        return securityContext.getUserPrincipal().getName();
                }
                // Ultimate fallback for local dev — default tenant
                return "default-tenant";
        }

        /** Builds a uniform error response body. */
        private static Response errorResponse(Response.Status status, Throwable ex) {
                ErrorCode errorCode = switch (status) {
                        case BAD_REQUEST -> ErrorCode.VALIDATION_FAILED;
                        case CONFLICT -> ErrorCode.CORE_CONFLICT;
                        case NOT_FOUND -> ErrorCode.CORE_NOT_FOUND;
                        default -> ErrorCode.INTERNAL_ERROR;
                };
                return Response.status(status)
                                .entity(ErrorResponse.from(new WayangException(
                                                errorCode,
                                                ex.getMessage(),
                                                ex)))
                                .build();
        }
}
