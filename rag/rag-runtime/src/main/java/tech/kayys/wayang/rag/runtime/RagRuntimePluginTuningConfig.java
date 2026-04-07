package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;

@ApplicationScoped
public class RagRuntimePluginTuningConfig implements RagPluginTuningConfig {

    private final RagRuntimeConfig runtimeConfig;

    public RagRuntimePluginTuningConfig(RagRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    public boolean normalizeQueryLowercase() {
        return runtimeConfig.isRagPluginNormalizeLowercase();
    }

    @Override
    public int normalizeQueryMaxLength() {
        return runtimeConfig.getRagPluginNormalizeMaxQueryLength();
    }

    @Override
    public double lexicalRerankOriginalWeight() {
        return runtimeConfig.getRagPluginRerankOriginalWeight();
    }

    @Override
    public double lexicalRerankLexicalWeight() {
        return runtimeConfig.getRagPluginRerankLexicalWeight();
    }

    @Override
    public boolean lexicalRerankAnnotateMetadata() {
        return runtimeConfig.isRagPluginRerankAnnotateMetadata();
    }

    @Override
    public String safetyFilterBlockedTerms() {
        return runtimeConfig.getRagPluginSafetyBlockedTerms();
    }

    @Override
    public String safetyFilterMask() {
        return runtimeConfig.getRagPluginSafetyMask();
    }
}
