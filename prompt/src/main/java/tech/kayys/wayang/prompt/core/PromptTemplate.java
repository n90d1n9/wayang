package tech.kayys.wayang.prompt.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * PromptTemplate — Versioned, tenant-scoped prompt definition.
 * ============================================================================
 *
 * Design rationale
 * ----------------
 * Every LLM invocation inside Wayang ultimately resolves to a PromptTemplate.
 * The template itself is immutable once published; mutations create a new
 * PromptVersion. This mirrors the platform's "Engine is authoritative" rule:
 * the Prompt Engine never mutates a published artifact in place.
 *
 * Relationship to the broader platform
 * -------------------------------------
 * - An AgentNode references a PromptTemplate by {@code templateId}.
 * - The Workflow Engine resolves variables at runtime using the current
 * NodeContext (inputs, RAG results, memory, environment).
 * - ErrorPayload and AuditPayload flow through the same provenance chain;
 * every render attempt is auditable.
 *
 * Lifecycle states
 * -----------------
 * DRAFT → PUBLISHED → DEPRECATED
 * ↑
 * (new version created, old stays PUBLISHED until explicitly deprecated)
 */
public final class PromptTemplate {

    /**
     * Unique identifier, follows platform Identifier pattern:
     * ^[a-z0-9_.-]+(/[a-z0-9_.-]+)?$
     */
    private final String templateId;

    /** Human-readable name shown in the low-code visual builder. */
    private final String name;

    /** Freeform description rendered in the designer sidebar. */
    private final String description;

    /** Owning tenant — enforced at every API boundary for multi-tenancy. */
    private final String tenantId;

    /** Semantic version of the *active* version pointer (read-only convenience). */
    private final String activeVersion;

    /** Current lifecycle state of this template root. */
    private final TemplateStatus status;

    /** Tags for search / categorisation inside the visual builder. */
    private final List<String> tags;

    /** Ordered list of all versions ever created for this template. */
    private final List<PromptVersion> versions;

    /** Immutable ordered variable declarations shared across all versions. */
    private final List<PromptVariableDefinition> variableDefinitions;

    /** Provenance: who created this template and when. */
    private final String createdBy;
    private final Instant createdAt;

    /** Provenance: who last modified (e.g. deprecated) and when. */
    private final String updatedBy;
    private final Instant updatedAt;

    /** Arbitrary key-value metadata (integrations, UI hints, etc.). */
    private final Map<String, String> metadata;

    // Precompiled regex pattern for efficiency
    private static final Pattern TEMPLATE_ID_PATTERN = Pattern.compile("^[a-z0-9_.\\-]+(/[a-z0-9_.\\-]+)?$");

    // -----------------------------------------------------------------------
    // Canonical constructor — enforces every invariant at construction time.
    // -----------------------------------------------------------------------
    @JsonCreator
    public PromptTemplate(
            @JsonProperty("templateId") String templateId,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("activeVersion") String activeVersion,
            @JsonProperty("status") TemplateStatus status,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("versions") List<PromptVersion> versions,
            @JsonProperty("variableDefinitions") List<PromptVariableDefinition> variableDefinitions,
            @JsonProperty("createdBy") String createdBy,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedBy") String updatedBy,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("metadata") Map<String, String> metadata) {

        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(status, "status must not be null");

        if (templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        if (!TEMPLATE_ID_PATTERN.matcher(templateId).matches()) {
            throw new IllegalArgumentException(
                    "templateId must match platform Identifier pattern: " + templateId);
        }

        // Validate variable definitions for duplicates
        validateVariableDefinitions(variableDefinitions);

        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.tenantId = tenantId;
        this.activeVersion = activeVersion;
        this.status = status;
        this.tags = Collections.unmodifiableList(tags != null ? List.copyOf(tags) : List.of());
        this.versions = Collections.unmodifiableList(versions != null ? List.copyOf(versions) : List.of());
        this.variableDefinitions = Collections.unmodifiableList(
                variableDefinitions != null ? List.copyOf(variableDefinitions) : List.of());
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.metadata = Collections.unmodifiableMap(metadata != null ? Map.copyOf(metadata) : Map.of());
    }

    /**
     * Validates variable definitions to ensure no duplicates.
     */
    private void validateVariableDefinitions(List<PromptVariableDefinition> variableDefinitions) {
        if (variableDefinitions != null) {
            Set<String> seenNames = new HashSet<>();
            for (PromptVariableDefinition varDef : variableDefinitions) {
                if (varDef != null) {
                    if (!seenNames.add(varDef.getName())) {
                        throw new IllegalArgumentException(
                                "Duplicate variable definition found: " + varDef.getName());
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Accessors (all return defensive copies where mutable)
    // -----------------------------------------------------------------------
    public String getTemplateId() {
        return templateId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActiveVersion() {
        return activeVersion;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<PromptVersion> getVersions() {
        return versions;
    }

    public List<PromptVariableDefinition> getVariableDefinitions() {
        return variableDefinitions;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Resolves the currently-active PromptVersion.
     * Returns empty if no version has been published yet (DRAFT-only template).
     */
    public java.util.Optional<PromptVersion> resolveActiveVersion() {
        if (activeVersion == null) {
            return java.util.Optional.empty();
        }
        return versions.stream()
                .filter(v -> v.getVersion().equals(activeVersion))
                .findFirst();
    }

    // -----------------------------------------------------------------------
    // Lifecycle helpers — return NEW instances (immutability preserved)
    // -----------------------------------------------------------------------

    /**
     * Appends a new version and, if it is the first published version, sets it as
     * active.
     */
    public PromptTemplate withNewVersion(PromptVersion newVersion) {
        List<PromptVersion> updated = new java.util.ArrayList<>(this.versions);
        updated.add(newVersion);

        String nextActive = this.activeVersion;
        if (nextActive == null && newVersion.getStatus() == PromptVersion.VersionStatus.PUBLISHED) {
            nextActive = newVersion.getVersion();
        }

        return new PromptTemplate(
                templateId, name, description, tenantId, nextActive, status,
                tags, updated, variableDefinitions,
                createdBy, createdAt, updatedBy, Instant.now(), metadata);
    }

    /**
     * Moves this template to DEPRECATED state.
     */
    public PromptTemplate deprecated(String actor) {
        return new PromptTemplate(
                templateId, name, description, tenantId, activeVersion,
                TemplateStatus.DEPRECATED, tags, versions, variableDefinitions,
                createdBy, createdAt, actor, Instant.now(), metadata);
    }

    // -----------------------------------------------------------------------
    // Object contract
    // -----------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PromptTemplate that))
            return false;
        return Objects.equals(templateId, that.templateId)
                && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, tenantId);
    }

    @Override
    public String toString() {
        return "PromptTemplate{id='%s', tenant='%s', status=%s, activeVersion='%s', versions=%d}"
                .formatted(templateId, tenantId, status, activeVersion, versions.size());
    }

    // -----------------------------------------------------------------------
    // Validation methods (expected by registry)
    // -----------------------------------------------------------------------

    /**
     * Checks for common template issues and returns warnings.
     * Validates that all declared required variables have corresponding
     * placeholders in the active version body, and vice versa.
     */
    public List<ValidationWarning> getValidationWarnings() {
        java.util.List<ValidationWarning> warnings = new java.util.ArrayList<>();
        java.util.Optional<PromptVersion> activeVer = resolveActiveVersion();

        if (activeVer.isEmpty() && status == TemplateStatus.PUBLISHED) {
            warnings.add(new ValidationWarning(
                    ValidationWarningType.MISSING_ACTIVE_VERSION,
                    null,
                    "Template is PUBLISHED but has no active version"));
            return warnings;
        }

        if (activeVer.isPresent()) {
            Set<String> placeholders = extractPlaceholders(activeVer.get().getTemplateBody());
            Set<String> declaredNames = new java.util.HashSet<>();
            for (PromptVariableDefinition varDef : variableDefinitions) {
                declaredNames.add(varDef.getName());
            }

            // Placeholders in body with no declaration
            for (String placeholder : placeholders) {
                if (!declaredNames.contains(placeholder)) {
                    warnings.add(new ValidationWarning(
                            ValidationWarningType.UNDECLARED_VARIABLE,
                            placeholder,
                            "Placeholder '{{" + placeholder + "}}' has no variable definition"));
                }
            }

            // Required declarations missing from body
            for (PromptVariableDefinition varDef : variableDefinitions) {
                if (varDef.isRequired() && !placeholders.contains(varDef.getName())) {
                    warnings.add(new ValidationWarning(
                            ValidationWarningType.UNUSED_VARIABLE,
                            varDef.getName(),
                            "Required variable '" + varDef.getName() + "' is not referenced in the template body"));
                }
            }
        }

        return warnings;
    }

    /**
     * Returns true if the active version has a system prompt configured.
     */
    public boolean hasCondition() {
        return resolveActiveVersion()
                .map(v -> v.getSystemPrompt() != null && !v.getSystemPrompt().isBlank())
                .orElse(false);
    }

    /**
     * Returns the active version's system prompt, or null if not set.
     */
    public String getCondition() {
        return resolveActiveVersion()
                .map(PromptVersion::getSystemPrompt)
                .orElse(null);
    }

    /**
     * Returns the prompt role — defaults to USER. When a system prompt is
     * present in the active version, returns SYSTEM.
     */
    public PromptRole getRole() {
        return resolveActiveVersion()
                .map(v -> v.getSystemPrompt() != null && !v.getSystemPrompt().isBlank()
                        ? PromptRole.SYSTEM
                        : PromptRole.USER)
                .orElse(PromptRole.USER);
    }

    /**
     * Extracts all variable names from the active version's template body
     * by finding all {@code {{variableName}}} patterns.
     */
    public java.util.Set<String> getExtractedVariableNames() {
        return getPlaceholders();
    }

    // -----------------------------------------------------------------------
    // Renderer-helper methods (extract from activeVersion)
    // -----------------------------------------------------------------------

    /**
     * Extracts all placeholder names from the active version's body.
     */
    public java.util.Set<String> getPlaceholders() {
        java.util.Optional<PromptVersion> activeVer = resolveActiveVersion();
        if (activeVer.isEmpty()) {
            return Set.of();
        }
        return extractPlaceholders(activeVer.get().getTemplateBody());
    }

    /**
     * Converts variable definitions to VariableDescriptor objects for
     * compatibility.
     */
    public java.util.List<VariableDescriptor> getVariables() {
        return variableDefinitions.stream()
                .map(def -> new VariableDescriptor(
                        def.getName(),
                        def.getDisplayName(),
                        def.getDescription(),
                        def.getType(),
                        convertVariableSource(def.getSource()),
                        def.isRequired(),
                        def.getDefaultValue(),
                        def.getMaxLength(),
                        def.isSensitive()))
                .toList();
    }

    /**
     * Converts from PromptVariableDefinition.VariableSource to
     * VariableDescriptor.VariableSource
     */
    private static VariableDescriptor.VariableSource convertVariableSource(
            PromptVariableDefinition.VariableSource source) {
        return switch (source) {
            case INPUT -> VariableDescriptor.VariableSource.INPUT;
            case CONTEXT -> VariableDescriptor.VariableSource.CONTEXT;
            case RAG -> VariableDescriptor.VariableSource.RAG;
            case MEMORY -> VariableDescriptor.VariableSource.MEMORY;
            case ENVIRONMENT -> VariableDescriptor.VariableSource.ENVIRONMENT;
            case SECRET -> VariableDescriptor.VariableSource.SECRET;
        };
    }

    /**
     * Returns the active version's template body.
     */
    public String getBody() {
        java.util.Optional<PromptVersion> activeVer = resolveActiveVersion();
        return activeVer.map(PromptVersion::getTemplateBody).orElse("");
    }

    /**
     * Extracts placeholder names from template text.
     */
    private static java.util.Set<String> extractPlaceholders(String templateBody) {
        if (templateBody == null || templateBody.isEmpty()) {
            return Set.of();
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");
        java.util.Set<String> placeholders = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher matcher = pattern.matcher(templateBody);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    // -----------------------------------------------------------------------
    // Lifecycle enum
    // -----------------------------------------------------------------------
    public enum TemplateStatus {
        /** Initial state — editable, not executable. */
        DRAFT,
        /** At least one version is published; template is executable. */
        PUBLISHED,
        /** No longer executable; retained for audit. */
        DEPRECATED;

        /**
         * Returns true if transitioning from this status to {@code target}
         * is a legal move in the status FSM.
         */
        public boolean isValidTransition(TemplateStatus target) {
            return switch (this) {
                case DRAFT -> target == PUBLISHED;
                case PUBLISHED -> target == DEPRECATED;
                case DEPRECATED -> false;
            };
        }
    }
}