package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import java.util.Map;

record NativeRagQueryContext(
        RagPluginExecutionContext pluginContext,
        RagQuery ragQuery,
        GenerationConfig generationConfig) {

    static NativeRagQueryContext create(
            String tenantId,
            String query,
            RetrievalConfig retrievalConfig,
            GenerationConfig generationConfig,
            Map<String, Object> filters,
            boolean retrievalOnly) {

        RetrievalConfig effectiveRetrievalConfig = RagRuntimeConfigs.retrievalOrDefault(retrievalConfig);
        GenerationConfig effectiveGenerationConfig = RagRuntimeConfigs.generationOrDefault(generationConfig);

        return fromPluginContext(new RagPluginExecutionContext(
                tenantId,
                query,
                effectiveRetrievalConfig.topK(),
                effectiveRetrievalConfig.minSimilarity(),
                RagRuntimeMetadata.copy(filters),
                effectiveGenerationConfig,
                retrievalOnly));
    }

    NativeRagQueryContext withPluginContext(RagPluginExecutionContext updatedContext) {
        return fromPluginContext(updatedContext == null ? pluginContext : updatedContext);
    }

    private static NativeRagQueryContext fromPluginContext(RagPluginExecutionContext context) {
        GenerationConfig effectiveGenerationConfig = RagRuntimeConfigs.generationOrDefault(context.generationConfig());
        Map<String, Object> effectiveFilters = RagRuntimeMetadata.copy(context.filters());
        RagQuery ragQuery = new RagQuery(
                context.query(),
                context.topK(),
                context.minSimilarity(),
                effectiveFilters);
        return new NativeRagQueryContext(context, ragQuery, effectiveGenerationConfig);
    }
}
