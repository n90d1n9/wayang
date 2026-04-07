package tech.kayys.wayang.prompt.core;

import java.util.*;

/**
 * ============================================================================
 * PromptRequest — the single inbound DTO that AgentNodes hand to
 * {@link PromptEngine#buildAndRender()}.
 * ============================================================================
 *
 * Encapsulates everything the engine needs in one immutable object:
 * • Tenant context – for registry scoping.
 * • Run / Node identity – for provenance correlation.
 * • Template references – "latest published" or "pinned to version".
 * • Explicit values – node-supplied variable overrides.
 * • Context values – workflow-engine-supplied context (RAG, memory, state).
 *
 * Constructed via the fluent {@link Builder}.
 */
public final class PromptRequest {

    private final String tenantId;
    private final String runId;
    private final String nodeId;
    private final List<TemplateRef> templateRefs;
    private final Map<String, Object> explicitValues;
    private final Map<String, Object> contextValues;

    private PromptRequest(Builder builder) {
        this.tenantId = builder.tenantId;
        this.runId = builder.runId;
        this.nodeId = builder.nodeId;
        this.templateRefs = Collections.unmodifiableList(new ArrayList<>(builder.templateRefs));
        this.explicitValues = Collections.unmodifiableMap(new LinkedHashMap<>(builder.explicitValues));
        this.contextValues = Collections.unmodifiableMap(new LinkedHashMap<>(builder.contextValues));
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRunId() {
        return runId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<TemplateRef> getTemplateRefs() {
        return templateRefs;
    }

    public Map<String, Object> getExplicitValues() {
        return explicitValues;
    }

    public Map<String, Object> getContextValues() {
        return contextValues;
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------
    public static class Builder {
        private String tenantId;
        private String runId;
        private String nodeId;
        private List<TemplateRef> templateRefs = new ArrayList<>();
        private Map<String, Object> explicitValues = new LinkedHashMap<>();
        private Map<String, Object> contextValues = new LinkedHashMap<>();

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder templateRefs(List<TemplateRef> refs) {
            this.templateRefs = new ArrayList<>(refs);
            return this;
        }

        public Builder addTemplateRef(TemplateRef ref) {
            this.templateRefs.add(ref);
            return this;
        }

        public Builder explicitValues(Map<String, Object> v) {
            this.explicitValues.putAll(v);
            return this;
        }

        public Builder contextValues(Map<String, Object> v) {
            this.contextValues.putAll(v);
            return this;
        }

        public PromptRequest build() {
            Objects.requireNonNull(tenantId, "tenantId is required");
            if (templateRefs.isEmpty()) {
                throw new IllegalStateException("At least one templateRef is required");
            }
            return new PromptRequest(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}