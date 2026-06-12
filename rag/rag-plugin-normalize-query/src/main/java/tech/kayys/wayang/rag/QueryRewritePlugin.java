package tech.kayys.wayang.rag;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.plugin.api.RagPluginSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QueryRewritePlugin implements RagPipelinePlugin {

    @Inject
    RagPluginTuningConfig tuningConfig = RagPluginTuningConfig.defaults();

    public QueryRewritePlugin() {
    }

    public QueryRewritePlugin(RagPluginTuningConfig tuningConfig) {
        this.tuningConfig = tuningConfig == null ? RagPluginTuningConfig.defaults() : tuningConfig;
    }

    @Override
    public String id() {
        return "normalize-query";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public RagPluginExecutionContext beforeQuery(RagPluginExecutionContext context) {
        return context.withQuery(RagPluginSupport.normalizeQuery(
                context.query(),
                tuningConfig.normalizeQueryLowercase(),
                tuningConfig.normalizeQueryMaxLength()));
    }
}
