package tech.kayys.wayang.prompt.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * ============================================================================
 * PromptRenderContext — runtime resolution bag supplied by the Workflow Engine.
 * ============================================================================
 *
 * The Prompt Engine is deliberately source-agnostic: it does not know how
 * values were produced. The Engine assembles a PromptRenderContext from the
 * current NodeContext and hands it in. The VariableResolver then picks the
 * right value for each variable based on
 * {@link PromptVariableDefinition#getSource()}.
 *
 * Immutability
 * ------------
 * All maps and lists exposed here are unmodifiable. The Engine constructs one
 * via the {@link Builder} and hands it to the Prompt Engine; the Prompt Engine
 * never mutates it.
 *
 * Relationship to the platform
 * ----------------------------
 * - {@code inputs} ← merged output of upstream node(s)
 * - {@code ragResults} ← output of RAG Service retrieval (if configured)
 * - {@code memoryEntries}← output of Agent Memory lookup (if configured)
 * - {@code environment} ← read-only view of runtime config / env vars
 * - {@code secrets} ← read-only view of resolved secrets (already decrypted)
 *
 * All five maps use the same key namespace as
 * {@link PromptVariableDefinition#getName()}.
 */
public final class PromptRenderContext {

    // -----------------------------------------------------------------------
    // Identity & provenance
    // -----------------------------------------------------------------------

    /** Workflow run identifier — propagated into RenderedPrompt for audit. */
    private final String runId;

    /** Node identifier within the run. */
    private final String nodeId;

    /** Owning tenant. */
    private final String tenantId;

    /** The template ID to render. */
    private final String templateId;

    /**
     * Explicit version to render. Null means "resolve to the template's
     * active version at render time".
     */
    private final String versionOverride;

    // -----------------------------------------------------------------------
    // Value sources (keyed by variable name)
    // -----------------------------------------------------------------------

    /** Direct inputs from upstream nodes or the workflow trigger. */
    private final Map<String, Object> inputs;

    /** Generic context bag (merged, lower priority than inputs). */
    private final Map<String, Object> context;

    /**
     * RAG retrieval results. Each entry is typically a {@code String}
     * (chunk text) or a {@code List<String>} (multiple chunks).
     */
    private final Map<String, Object> ragResults;

    /**
     * Agent memory entries. Keyed by memory key; value is whatever the
     * memory store returned (String or structured object).
     */
    private final Map<String, Object> memoryEntries;

    /** Environment / config values. */
    private final Map<String, String> environment;

    /**
     * Secrets — already decrypted by the Engine before this context is created.
     * The Prompt Engine treats these as opaque strings.
     */
    private final Map<String, String> secrets;

    // -----------------------------------------------------------------------
    // Private constructor — use Builder
    // -----------------------------------------------------------------------
    private PromptRenderContext(
            String runId,
            String nodeId,
            String tenantId,
            String templateId,
            String versionOverride,
            Map<String, Object> inputs,
            Map<String, Object> context,
            Map<String, Object> ragResults,
            Map<String, Object> memoryEntries,
            Map<String, String> environment,
            Map<String, String> secrets) {

        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(templateId, "templateId must not be null");

        this.runId = runId;
        this.nodeId = nodeId;
        this.tenantId = tenantId;
        this.templateId = templateId;
        this.versionOverride = versionOverride;
        this.inputs = Collections.unmodifiableMap(inputs != null ? Map.copyOf(inputs) : Map.of());
        this.context = Collections.unmodifiableMap(context != null ? Map.copyOf(context) : Map.of());
        this.ragResults = Collections.unmodifiableMap(ragResults != null ? Map.copyOf(ragResults) : Map.of());
        this.memoryEntries = Collections.unmodifiableMap(memoryEntries != null ? Map.copyOf(memoryEntries) : Map.of());
        this.environment = Collections.unmodifiableMap(environment != null ? Map.copyOf(environment) : Map.of());
        this.secrets = Collections.unmodifiableMap(secrets != null ? Map.copyOf(secrets) : Map.of());
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String getRunId() {
        return runId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getVersionOverride() {
        return versionOverride;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Map<String, Object> getRagResults() {
        return ragResults;
    }

    public Map<String, Object> getMemoryEntries() {
        return memoryEntries;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    /**
     * Convenience: looks up a value across all sources in priority order.
     *
     * Priority (highest first):
     * 1. secrets
     * 2. inputs
     * 3. context
     * 4. ragResults
     * 5. memoryEntries
     * 6. environment
     *
     * Returns null if the key is not found anywhere.
     */
    public Object resolveAny(String key) {
        if (secrets.containsKey(key))
            return secrets.get(key);
        if (inputs.containsKey(key))
            return inputs.get(key);
        if (context.containsKey(key))
            return context.get(key);
        if (ragResults.containsKey(key))
            return ragResults.get(key);
        if (memoryEntries.containsKey(key))
            return memoryEntries.get(key);
        if (environment.containsKey(key))
            return environment.get(key);
        return null;
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------
    public static class Builder {
        private String runId;
        private String nodeId;
        private String tenantId;
        private String templateId;
        private String versionOverride;
        private Map<String, Object> inputs;
        private Map<String, Object> context;
        private Map<String, Object> ragResults;
        private Map<String, Object> memoryEntries;
        private Map<String, String> environment;
        private Map<String, String> secrets;

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder versionOverride(String v) {
            this.versionOverride = v;
            return this;
        }

        public Builder inputs(Map<String, Object> m) {
            this.inputs = m;
            return this;
        }

        public Builder context(Map<String, Object> m) {
            this.context = m;
            return this;
        }

        public Builder ragResults(Map<String, Object> m) {
            this.ragResults = m;
            return this;
        }

        public Builder memoryEntries(Map<String, Object> m) {
            this.memoryEntries = m;
            return this;
        }

        public Builder environment(Map<String, String> m) {
            this.environment = m;
            return this;
        }

        public Builder secrets(Map<String, String> m) {
            this.secrets = m;
            return this;
        }

        public PromptRenderContext build() {
            return new PromptRenderContext(
                    runId, nodeId, tenantId, templateId, versionOverride,
                    inputs, context, ragResults, memoryEntries, environment, secrets);
        }
    }

    @Override
    public String toString() {
        return ("PromptRenderContext{run='%s', node='%s', tenant='%s', template='%s', "
                + "inputs=%d, rag=%d, memory=%d}").formatted(
                        runId, nodeId, tenantId, templateId,
                        inputs.size(), ragResults.size(), memoryEntries.size());
    }
}
