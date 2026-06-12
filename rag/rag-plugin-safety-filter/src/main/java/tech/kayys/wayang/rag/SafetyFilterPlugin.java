package tech.kayys.wayang.rag;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.plugin.api.RagPluginSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SafetyFilterPlugin implements RagPipelinePlugin {

    @Inject
    RagPluginTuningConfig tuningConfig = RagPluginTuningConfig.defaults();

    public SafetyFilterPlugin() {
    }

    public SafetyFilterPlugin(RagPluginTuningConfig tuningConfig) {
        this.tuningConfig = tuningConfig == null ? RagPluginTuningConfig.defaults() : tuningConfig;
    }

    @Override
    public String id() {
        return "safety-filter";
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public RagPluginExecutionContext beforeQuery(RagPluginExecutionContext context) {
        Set<String> blocked = blockedTerms();
        if (blocked.isEmpty()) {
            return context;
        }
        return context.withQuery(RagPluginSupport.redactTermsIgnoreCase(context.query(), blocked, maskValue()));
    }

    @Override
    public List<RagScoredChunk> afterRetrieve(RagPluginExecutionContext context, List<RagScoredChunk> chunks) {
        Set<String> blocked = blockedTerms();
        if (blocked.isEmpty() || chunks == null || chunks.isEmpty()) {
            return chunks == null ? List.of() : chunks;
        }
        return chunks.stream()
                .filter(chunk -> chunk != null
                        && chunk.chunk() != null
                        && !RagPluginSupport.containsAnyTermIgnoreCase(chunk.chunk().text(), blocked))
                .collect(Collectors.toList());
    }

    @Override
    public RagResult afterResult(RagPluginExecutionContext context, RagResult result) {
        Set<String> blocked = blockedTerms();
        if (blocked.isEmpty() || result == null) {
            return result;
        }
        String answer = result.answer();
        String redacted = RagPluginSupport.redactTermsIgnoreCase(answer, blocked, maskValue());
        if (answer == null || answer.equals(redacted)) {
            return result;
        }
        return new RagResult(
                result.query(),
                result.chunks(),
                redacted,
                RagPluginSupport.metadataWith(result.metadata(), "plugin.safety_filter.answer_redacted", true));
    }

    private Set<String> blockedTerms() {
        return RagPluginSupport.lowercaseCsvTerms(tuningConfig.safetyFilterBlockedTerms());
    }

    private String maskValue() {
        return tuningConfig.safetyFilterMask();
    }
}
