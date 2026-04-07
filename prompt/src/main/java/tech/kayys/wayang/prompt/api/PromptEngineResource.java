package tech.kayys.wayang.prompt.api;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.logging.Logger;
import tech.kayys.wayang.error.ErrorCode;
import tech.kayys.wayang.error.ErrorResponse;
import tech.kayys.wayang.error.WayangException;
import tech.kayys.wayang.prompt.audit.PromptAuditService;
import tech.kayys.wayang.prompt.core.*;
import tech.kayys.wayang.prompt.registry.PromptTemplateRegistry;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * PromptEngineResource — REST API for the Prompt Engine.
 * ============================================================================
 *
 * Endpoint summary
 * ----------------
 * POST /api/v1/prompts → create template
 * GET /api/v1/prompts/{templateId} → get template
 * GET /api/v1/prompts → list templates (tenant)
 * POST /api/v1/prompts/{templateId}/deprecate → deprecate template
 * POST /api/v1/prompts/{templateId}/versions → add version
 * POST /api/v1/prompts/{templateId}/versions/{version}/publish → publish
 * version
 * POST /api/v1/prompts/{templateId}/render → render prompt (hot path)
 *
 * Authentication & tenant extraction
 * -----------------------------------
 * In production, the tenantId and actor (createdBy) are extracted from the
 * incoming JWT via a @Produces SecurityContext or a custom CDI bean.
 * For clarity, this resource accepts them as explicit query parameters in
 * development; the production wrapper replaces them with JWT claims.
 *
 * Error responses
 * ---------------
 * All errors are returned as structured JSON bodies compatible with the
 * platform's ErrorPayload schema:
 * {
 * "type" : "ValidationError | LLMError | UnknownError",
 * "message" : "...",
 * "retryable" : boolean,
 * "originNode" : "prompt-api"
 * }
 */
@Path("/api/v1/prompts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromptEngineResource {

        private static final Logger LOG = Logger.getLogger(PromptEngineResource.class);

        @Inject
        PromptTemplateRegistry registry;

        @Inject
        PromptAuditService auditService;

        @Inject
        TemplateRenderer renderer;

        // -----------------------------------------------------------------------
        // Template CRUD
        // -----------------------------------------------------------------------

        /**
         * Creates a new PromptTemplate in DRAFT state.
         *
         * Request body example:
         * {
         * "templateId": "prompts/customer-greeting",
         * "name": "Customer Greeting",
         * "description": "Greeting prompt for onboarding flow",
         * "tags": ["onboarding", "greeting"],
         * "variableDefinitions": [
         * { "name": "customerName", "type": "STRING", "source": "INPUT", "required":
         * true },
         * { "name": "productInfo", "type": "JSON", "source": "RAG", "required": false,
         * "maxLength": 2000 }
         * ]
         * }
         */
        @POST
        public Uni<Response> createTemplate(
                        CreateTemplateRequest request,
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId,
                        @QueryParam("actor") @DefaultValue("system") String actor) {

                LOG.infof("POST /prompts — creating template '%s' for tenant '%s'", request.templateId(), tenantId);

                return registry.createTemplate(
                                request.templateId(),
                                request.name(),
                                request.description(),
                                tenantId,
                                request.tags(),
                                request.variableDefinitions(),
                                actor)
                                .onItem().invoke(template -> auditService.onTemplateCreated(template, actor))
                                .map(template -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(template)
                                                .build())
                                .onFailure().recoverWithItem(th -> errorResponse(th, tenantId));
        }

        /**
         * Retrieves a template by ID.
         */
        @GET
        @Path("/{templateId}")
        public Uni<Response> getTemplate(
                        @PathParam("templateId") String templateId,
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId) {

                return registry.findById(templateId, tenantId)
                                .map(template -> Response.ok(template).build())
                                .onFailure().recoverWithItem(th -> errorResponse(th, tenantId));
        }

        /**
         * Lists all templates for the tenant.
         */
        @GET
        public Uni<Response> listTemplates(
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId,
                        @QueryParam("page") @DefaultValue("0") int page,
                        @QueryParam("size") @DefaultValue("20") int size) {

                return registry.listByTenant(tenantId, page, size)
                                .map(templates -> Response.ok(templates).build())
                                .onFailure().recoverWithItem(th -> errorResponse(th, tenantId));
        }

        /**
         * Deprecates a template. The template remains queryable for audit but
         * can no longer be rendered.
         */
        @POST
        @Path("/{templateId}/deprecate")
        public Uni<Response> deprecateTemplate(
                        @PathParam("templateId") String templateId,
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId,
                        @QueryParam("actor") @DefaultValue("system") String actor) {

                LOG.infof("POST /prompts/%s/deprecate — actor='%s'", templateId, actor);

                return registry.deprecateTemplate(templateId, tenantId, actor)
                                .onItem().invoke(t -> auditService.onTemplateDeprecated(t, actor))
                                .map(template -> Response.ok(template).build())
                                .onFailure().recoverWithItem(th -> errorResponse(th, tenantId));
        }

        // -----------------------------------------------------------------------
        // Version management
        // -----------------------------------------------------------------------

        /**
         * Adds a new DRAFT version to an existing template.
         *
         * Request body example:
         * {
         * "version": "1.0.0",
         * "templateBody": "Hello {{customerName}}, welcome to {{productInfo}}!",
         * "systemPrompt": "You are a friendly onboarding assistant.",
         * "renderingStrategy": "SIMPLE",
         * "maxOutputTokens": 512,
         * "maxContextTokens": 4096
         * }
         */
        @POST
        @Path("/{templateId}/versions")
        public Uni<Response> addVersion(
                        @PathParam("templateId") String templateId,
                        AddVersionRequest request,
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId,
                        @QueryParam("actor") @DefaultValue("system") String actor) {

                LOG.infof("POST /prompts/%s/versions — version='%s', actor='%s'",
                                templateId, request.version(), actor);

                PromptVersion draftVersion = new PromptVersion(
                                request.version(),
                                request.templateBody(),
                                request.systemPrompt(),
                                request.renderingStrategy(),
                                request.maxOutputTokens(),
                                request.maxContextTokens(),
                                PromptVersion.VersionStatus.DRAFT,
                                null, // bodyHash computed by registry
                                actor,
                                java.time.Instant.now(),
                                request.metadata() != null ? request.metadata() : Map.of());

                return registry.addVersion(templateId, tenantId, draftVersion, actor)
                                .onItem()
                                .invoke(t -> auditService.onVersionCreated(templateId, draftVersion, tenantId, actor))
                                .map(template -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(template)
                                                .build())
                                .onFailure().recoverWithItem(th -> errorResponse(th, tenantId));
        }

        /**
         * Publishes a DRAFT version, freezing it and making it the active version.
         */
        @POST
        @Path("/{templateId}/versions/{version}/publish")
        public Uni<Response> publishVersion(
                        @PathParam("templateId") String templateId,
                        @PathParam("version") String version,
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId,
                        @QueryParam("actor") @DefaultValue("system") String actor) {

                LOG.infof("POST /prompts/%s/versions/%s/publish — actor='%s'", templateId, version, actor);

                return registry.publishVersion(templateId, tenantId, version, actor)
                                .onItem()
                                .invoke(t -> auditService.onVersionPublished(templateId, version, tenantId, actor))
                                .map(template -> Response.ok(template).build())
                                .onFailure().recoverWithItem(th -> errorResponse(th, tenantId));
        }

        // -----------------------------------------------------------------------
        // Render (hot path)
        // -----------------------------------------------------------------------

        /**
         * Renders a prompt template with the supplied variable values.
         * This is the endpoint called by AgentNode at runtime.
         *
         * Request body example:
         * {
         * "runId": "run-abc-123",
         * "nodeId": "node-greeting-01",
         * "versionOverride": null, ← null means "use active version"
         * "inputs": { "customerName": "Alice" },
         * "ragResults": { "productInfo": "Wayang is an AI workflow platform..." },
         * "context": {},
         * "memoryEntries": {},
         * "environment": {},
         * "secrets": {}
         * }
         */
        @POST
        @Path("/{templateId}/render")
        public Uni<Response> renderPrompt(
                        @PathParam("templateId") String templateId,
                        RenderRequest request,
                        @QueryParam("tenantId") @DefaultValue("default-tenant") String tenantId) {

                LOG.debugf("POST /prompts/%s/render — run='%s', node='%s'",
                                templateId, request.runId(), request.nodeId());

                // Build the render context
                PromptRenderContext ctx = new PromptRenderContext.Builder()
                                .runId(request.runId())
                                .nodeId(request.nodeId())
                                .tenantId(tenantId)
                                .templateId(templateId)
                                .versionOverride(request.versionOverride())
                                .inputs(request.inputs())
                                .context(request.context())
                                .ragResults(request.ragResults())
                                .memoryEntries(request.memoryEntries())
                                .environment(request.environment())
                                .secrets(request.secrets())
                                .build();

                // Emit render-started audit event
                auditService.onRenderStarted(ctx);

                // Resolve version → render → audit → respond
                return registry.resolveVersion(templateId, tenantId, request.versionOverride())
                                .onItem().transformToUni(version -> {
                                        // Resolve variables using the template's variable definitions
                                        // In a full implementation, the definitions come from the registry.
                                        // Here we pass them through the context for the resolver.
                                        return renderer.renderWithVariables(version, List.of(), ctx);
                                })
                                .onItem().invoke(auditService::onRenderSucceeded)
                                .map(rendered -> Response.ok(rendered).build())
                                .onFailure().recoverWithItem(th -> {
                                        if (th instanceof PromptEngineException pex) {
                                                auditService.onRenderFailed(ctx, pex);
                                        }
                                        return errorResponse(th, tenantId);
                                });
        }

        // -----------------------------------------------------------------------
        // Error response helper
        // -----------------------------------------------------------------------

        /**
         * Converts any Throwable into a structured ErrorPayload-compatible JSON
         * response.
         * This ensures the platform's Error Channel can route errors from the Prompt
         * Engine in the same way it routes errors from any other node.
         */
        private Response errorResponse(Throwable th, String tenantId) {
                if (th instanceof PromptEngineException pex) {
                        ErrorCode errorCode = switch (pex.getErrorCategory()) {
                                case VALIDATION_ERROR -> ErrorCode.VALIDATION_FAILED;
                                case LLM_ERROR -> ErrorCode.INFERENCE_REQUEST_FAILED;
                                case UNKNOWN_ERROR -> ErrorCode.CORE_NOT_FOUND;
                        };

                        WayangException wrapped = new WayangException(
                                        errorCode,
                                        pex.getMessage(),
                                        pex);

                        return Response.status(errorCode.getHttpStatus())
                                        .entity(ErrorResponse.from(wrapped))
                                        .build();
                }

                // Fallback for unexpected exceptions
                LOG.errorf("Unexpected error in PromptEngine: %s", th.getMessage());
                WayangException wrapped = new WayangException(
                                ErrorCode.INTERNAL_ERROR,
                                th.getMessage(),
                                th);
                return Response.serverError()
                                .entity(ErrorResponse.from(wrapped))
                                .build();
        }
}

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request body for POST /prompts (create template).
 */
record CreateTemplateRequest(
                String templateId,
                String name,
                String description,
                List<String> tags,
                List<PromptVariableDefinition> variableDefinitions) {
}

/**
 * Request body for POST /prompts/{id}/versions (add version).
 */
record AddVersionRequest(
                String version,
                String templateBody,
                String systemPrompt,
                PromptVersion.RenderingStrategy renderingStrategy,
                Integer maxOutputTokens,
                Integer maxContextTokens,
                Map<String, String> metadata) {
}

/**
 * Request body for POST /prompts/{id}/render.
 * Maps directly to PromptRenderContext fields.
 */
record RenderRequest(
                String runId,
                String nodeId,
                String versionOverride,
                Map<String, Object> inputs,
                Map<String, Object> context,
                Map<String, Object> ragResults,
                Map<String, Object> memoryEntries,
                Map<String, String> environment,
                Map<String, String> secrets) {
}
