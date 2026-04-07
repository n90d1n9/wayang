package tech.kayys.wayang.rag;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Locale;

@ApplicationScoped
public class QueryRewritePlugin implements RagPipelinePlugin {

    @Inject
    RagPluginTuningConfig tuningConfig;

    public QueryRewritePlugin() {
    }

    public QueryRewritePlugin(RagPluginTuningConfig tuningConfig) {
        this.tuningConfig = tuningConfig;
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
        String query = context.query();
        if (query == null) {
            query = "";
        }
        String normalized = query.trim().replaceAll("\\s+", " ");
        if (tuningConfig.normalizeQueryLowercase()) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }

        int maxLength = tuningConfig.normalizeQueryMaxLength();
        if (maxLength > 0 && normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
        }
        return context.withQuery(normalized);
    }
}
