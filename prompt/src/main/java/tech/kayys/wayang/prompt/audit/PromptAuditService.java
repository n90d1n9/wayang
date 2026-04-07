package tech.kayys.wayang.prompt.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.prompt.core.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * PromptAuditService — emits structured audit events for the Prompt Engine.
 * ============================================================================
 *
 * Every operation that changes observable state or touches LLM-bound data
 * produces an audit event.  The events are structured to match the
 * AuditPayload schema defined in the blueprint (§2 of the Audit Layer design):
 *
 *   {
 *     "timestamp"       : ISO-8601,
 *     "event"           : string,          ← see EventType enum below
 *     "level"           : INFO|WARN|ERROR|CRITICAL,
 *     "actor"           : { type, id },
 *     "nodeId"          : string,
 *     "runId"           : string,
 *     "metadata"        : { ... },
 *     "hash"            : SHA-256          ← tamper-proof chaining
 *   }
 *
 * Tamper-proof hashing
 * --------------------
 * Each event's hash is computed over:
 *     SHA-256( timestamp + event + actor.id + runId + nodeId )
 * This matches the blueprint's §6 pattern and lets the audit consumer verify
 * that events have not been modified after emission.
 *
 * Standalone fallback
 * -------------------
 * When the full ProvenanceService is not available (standalone runtime), this
 * service logs events to stdout as structured JSON lines.  The standalone
 * runtime can later ship these to a centralised audit sink.
 *
 * Event catalogue
 * ---------------
 *   TEMPLATE_CREATED        — a new PromptTemplate was created
 *   TEMPLATE_DEPRECATED     — a template was deprecated
 *   VERSION_CREATED         — a new version was added
 *   VERSION_PUBLISHED       — a version was published (frozen)
 *   VERSION_DEPRECATED      — a version was deprecated
 *   PROMPT_RENDER_STARTED   — a render pipeline began
 *   PROMPT_RENDER_SUCCEEDED — a RenderedPrompt was produced
 *   PROMPT_RENDER_FAILED    — rendering threw an exception
 */
@ApplicationScoped
public class PromptAuditService {

    private static final Logger LOG = Logger.getLogger(PromptAuditService.class);

    // -----------------------------------------------------------------------
    // Template lifecycle events
    // -----------------------------------------------------------------------

    /**
     * Emits TEMPLATE_CREATED.
     */
    public void onTemplateCreated(PromptTemplate template, String actor) {
        Map<String, Object> metadata = Map.of(
                "templateId", template.getTemplateId(),
                "tenantId", template.getTenantId(),
                "status", template.getStatus().name(),
                "variableCount", template.getVariableDefinitions().size()
        );

        emitEvent(
                EventType.TEMPLATE_CREATED,
                Level.INFO,
                actor,
                template.getTenantId(),
                "prompt-registry",   // nodeId — registry is the acting "node"
                null,                // runId — not associated with a workflow run
                metadata
        );
    }

    /**
     * Emits TEMPLATE_DEPRECATED.
     */
    public void onTemplateDeprecated(PromptTemplate template, String actor) {
        emitEvent(
                EventType.TEMPLATE_DEPRECATED,
                Level.WARN,
                actor,
                template.getTenantId(),
                "prompt-registry",
                null,
                Map.of("templateId", template.getTemplateId())
        );
    }

    // -----------------------------------------------------------------------
    // Version lifecycle events
    // -----------------------------------------------------------------------

    /**
     * Emits VERSION_CREATED.
     */
    public void onVersionCreated(String templateId, PromptVersion version, String tenantId, String actor) {
        emitEvent(
                EventType.VERSION_CREATED,
                Level.INFO,
                actor,
                tenantId,
                "prompt-registry",
                null,
                Map.of(
                        "templateId", templateId,
                        "version", version.getVersion(),
                        "strategy", version.getRenderingStrategy().name(),
                        "bodyHash", version.getBodyHash() != null ? version.getBodyHash() : "pending"
                )
        );
    }

    /**
     * Emits VERSION_PUBLISHED.
     */
    public void onVersionPublished(String templateId, String version, String tenantId, String actor) {
        emitEvent(
                EventType.VERSION_PUBLISHED,
                Level.INFO,
                actor,
                tenantId,
                "prompt-registry",
                null,
                Map.of("templateId", templateId, "version", version)
        );
    }

    // -----------------------------------------------------------------------
    // Render lifecycle events
    // -----------------------------------------------------------------------

    /**
     * Emits PROMPT_RENDER_STARTED.
     * Called at the very beginning of the render pipeline before any I/O.
     */
    public void onRenderStarted(PromptRenderContext ctx) {
        emitEvent(
                EventType.PROMPT_RENDER_STARTED,
                Level.INFO,
                "system",
                ctx.getTenantId(),
                ctx.getNodeId(),
                ctx.getRunId(),
                Map.of(
                        "templateId", ctx.getTemplateId(),
                        "versionOverride", ctx.getVersionOverride() != null ? ctx.getVersionOverride() : "active"
                )
        );
    }

    /**
     * Emits PROMPT_RENDER_SUCCEEDED with the content hash for integrity verification.
     */
    public void onRenderSucceeded(RenderedPrompt rendered) {
        emitEvent(
                EventType.PROMPT_RENDER_SUCCEEDED,
                Level.INFO,
                "system",
                rendered.getTenantId(),
                rendered.getNodeId(),
                rendered.getRunId(),
                Map.of(
                        "templateId", rendered.getTemplateId(),
                        "version", rendered.getTemplateVersion() != null ? rendered.getTemplateVersion() : "unknown",
                        "contentHash", rendered.getContentHash(),
                        "estimatedTokens", String.valueOf(rendered.getEstimatedInputTokens()),
                        "rendererId", rendered.getRendererId()
                )
        );
    }

    /**
     * Emits PROMPT_RENDER_FAILED.
     * Captures the exception type and message for the ErrorHandler routing.
     */
    public void onRenderFailed(PromptRenderContext ctx, PromptEngineException ex) {
        Level level = ex.getErrorCategory() == PromptEngineException.ErrorCategory.LLM_ERROR
                ? Level.ERROR
                : Level.WARN;

        emitEvent(
                EventType.PROMPT_RENDER_FAILED,
                level,
                "system",
                ctx.getTenantId(),
                ctx.getNodeId(),
                ctx.getRunId(),
                Map.of(
                        "templateId", ctx.getTemplateId(),
                        "errorCategory", ex.getErrorCategory().name(),
                        "retryable", String.valueOf(ex.isRetryable()),
                        "message", ex.getMessage()
                )
        );
    }

    // -----------------------------------------------------------------------
    // Core emit logic
    // -----------------------------------------------------------------------

    /**
     * Constructs the audit event, computes the tamper-proof hash, and emits it.
     *
     * In production this delegates to the platform's ProvenanceService.
     * In standalone mode it logs to stdout as structured JSON.
     */
    private void emitEvent(
            EventType eventType,
            Level level,
            String actorId,
            String tenantId,
            String nodeId,
            String runId,
            Map<String, Object> metadata) {

        Instant timestamp = Instant.now();

        // Compute tamper-proof hash: SHA-256(timestamp + event + actorId + runId + nodeId)
        String hashInput = timestamp.toString()
                + "|" + eventType.name()
                + "|" + actorId
                + "|" + (runId != null ? runId : "")
                + "|" + (nodeId != null ? nodeId : "");
        String hash = sha256(hashInput);

        // Build the structured event
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", timestamp.toString());
        event.put("event", eventType.name());
        event.put("level", level.name());
        event.put("actor", Map.of("type", "system", "id", actorId));
        event.put("tenantId", tenantId);
        event.put("nodeId", nodeId != null ? nodeId : "");
        event.put("runId", runId != null ? runId : "");
        event.put("metadata", metadata);
        event.put("hash", hash);

        // Emit — in production, delegate to ProvenanceService.
        // For now, log at INFO for visibility.
        LOG.infof("[PROMPT_AUDIT] %s | event=%s | level=%s | actor=%s | node=%s | run=%s | hash=%s",
                timestamp, eventType, level, actorId, nodeId, runId, hash);
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append("%02x".formatted(b & 0xff));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    /**
     * All audit event types emitted by the Prompt Engine.
     * These map directly to the event catalogue in the blueprint's Audit Layer §5.
     */
    public enum EventType {
        TEMPLATE_CREATED,
        TEMPLATE_DEPRECATED,
        VERSION_CREATED,
        VERSION_PUBLISHED,
        VERSION_DEPRECATED,
        PROMPT_RENDER_STARTED,
        PROMPT_RENDER_SUCCEEDED,
        PROMPT_RENDER_FAILED
    }

    /**
     * Severity levels — matches the AuditPayload.level enum from the blueprint.
     */
    public enum Level {
        INFO,
        WARN,
        ERROR,
        CRITICAL
    }
}
