package tech.kayys.wayang.prompt.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * ============================================================================
 * PromptVersion — one immutable snapshot of a PromptTemplate's content.
 * ============================================================================
 *
 * Why a separate object?
 * ----------------------
 * The platform requires full provenance on every LLM call. A PromptVersion
 * captures *exactly* what text was available at render time, the rendering
 * strategy used, and any model hints that were baked in. This lets the
 * Provenance / Audit layer reconstruct any past execution byte-for-byte.
 *
 * Version lifecycle
 * -----------------
 * DRAFT → PUBLISHED → DEPRECATED
 *
 * A DRAFT version can be freely edited via the registry API. Once published,
 * it is frozen. Deprecation is the only subsequent mutation and is append-only
 * in the audit trail.
 *
 * Rendering strategy
 * ------------------
 * {@link RenderingStrategy} determines how variable placeholders are resolved:
 * - SIMPLE : {{variable}} — basic string interpolation, zero external deps.
 * - JINJA2 : Jinja2-compatible syntax via Pebble (JVM port).
 * - FREEMARKER: Full FreeMarker template language.
 *
 * Choosing SIMPLE keeps the standalone/portable runtime dependency-free;
 * JINJA2 / FREEMARKER are available when the full platform module is loaded.
 */
public final class PromptVersion {

    /** Semantic version string (SemVer). */
    private final String version;

    /** Raw template body — the text that the renderer processes. */
    private final String templateBody;

    /**
     * Optional system-prompt override. When set, this replaces the default
     * system message sent to the LLM. Null means "use the agent's default".
     */
    private final String systemPrompt;

    /** Which rendering engine to apply to {@link #templateBody}. */
    private final RenderingStrategy renderingStrategy;

    /**
     * Maximum tokens the LLM is allowed to *generate* for this prompt.
     * Null means "use the model's default / agent-level cap".
     */
    private final Integer maxOutputTokens;

    /**
     * Hard cap on the total context window consumed by this prompt after
     * variable expansion (input tokens). Used to prevent runaway RAG injection.
     * Null means no explicit cap beyond the model limit.
     */
    private final Integer maxContextTokens;

    /** Lifecycle state. */
    private final VersionStatus status;

    /**
     * Hash of {@link #templateBody} at creation time — used by audit for integrity
     * checks.
     */
    private final String bodyHash;

    /** Who created / last touched this version. */
    private final String createdBy;
    private final Instant createdAt;

    /** Arbitrary key-value metadata (e.g. "changelog", "reviewedBy"). */
    private final Map<String, String> metadata;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    @JsonCreator
    public PromptVersion(
            @JsonProperty("version") String version,
            @JsonProperty("templateBody") String templateBody,
            @JsonProperty("systemPrompt") String systemPrompt,
            @JsonProperty("renderingStrategy") RenderingStrategy renderingStrategy,
            @JsonProperty("maxOutputTokens") Integer maxOutputTokens,
            @JsonProperty("maxContextTokens") Integer maxContextTokens,
            @JsonProperty("status") VersionStatus status,
            @JsonProperty("bodyHash") String bodyHash,
            @JsonProperty("createdBy") String createdBy,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("metadata") Map<String, String> metadata) {

        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(templateBody, "templateBody must not be null");
        Objects.requireNonNull(renderingStrategy, "renderingStrategy must not be null");
        Objects.requireNonNull(status, "status must not be null");

        if (!version.matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.\\-]+)?$")) {
            throw new IllegalArgumentException("version must be valid SemVer: " + version);
        }

        this.version = version;
        this.templateBody = templateBody;
        this.systemPrompt = systemPrompt;
        this.renderingStrategy = renderingStrategy;
        this.maxOutputTokens = maxOutputTokens;
        this.maxContextTokens = maxContextTokens;
        this.status = status;
        this.bodyHash = bodyHash;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.metadata = Collections.unmodifiableMap(metadata != null ? Map.copyOf(metadata) : Map.of());
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String getVersion() {
        return version;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public RenderingStrategy getRenderingStrategy() {
        return renderingStrategy;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public Integer getMaxContextTokens() {
        return maxContextTokens;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public String getBodyHash() {
        return bodyHash;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    /** Returns true when this version is frozen and cannot be edited. */
    public boolean isFrozen() {
        return status == VersionStatus.PUBLISHED || status == VersionStatus.DEPRECATED;
    }

    // -----------------------------------------------------------------------
    // Lifecycle transition — returns a NEW instance
    // -----------------------------------------------------------------------
    public PromptVersion publish() {
        if (status != VersionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Cannot publish version in state " + status + "; must be DRAFT");
        }
        return new PromptVersion(version, templateBody, systemPrompt, renderingStrategy,
                maxOutputTokens, maxContextTokens, VersionStatus.PUBLISHED,
                bodyHash, createdBy, createdAt, metadata);
    }

    public PromptVersion deprecated() {
        if (status == VersionStatus.DEPRECATED) {
            return this; // idempotent
        }
        return new PromptVersion(version, templateBody, systemPrompt, renderingStrategy,
                maxOutputTokens, maxContextTokens, VersionStatus.DEPRECATED,
                bodyHash, createdBy, createdAt, metadata);
    }

    // -----------------------------------------------------------------------
    // Object contract
    // -----------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PromptVersion that))
            return false;
        return Objects.equals(version, that.version)
                && Objects.equals(bodyHash, that.bodyHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, bodyHash);
    }

    @Override
    public String toString() {
        return "PromptVersion{version='%s', strategy=%s, status=%s, bodyLen=%d}"
                .formatted(version, renderingStrategy, status, templateBody.length());
    }

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    /**
     * Lifecycle states for a single version.
     */
    public enum VersionStatus {
        DRAFT,
        PUBLISHED,
        DEPRECATED
    }

    /**
     * Template rendering engines supported by the platform.
     *
     * SIMPLE — Zero-dependency mustache-style interpolation.
     * Suitable for standalone / portable runtimes.
     * JINJA2 — Jinja2-compatible syntax via Pebble (JVM).
     * Supports loops, conditionals, filters. Requires
     * the {@code wayang-prompt-jinja2} module.
     * FREEMARKER — Full Apache FreeMarker. Maximum expressiveness.
     * Requires the {@code wayang-prompt-freemarker} module.
     */
    public enum RenderingStrategy {
        SIMPLE,
        JINJA2,
        FREEMARKER
    }
}