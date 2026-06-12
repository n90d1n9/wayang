package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RagWorkflowInput;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

final class RagWorkflowRequestMapper {

    static final String DEFAULT_COLLECTION = RagRuntimeDefaults.DEFAULT_COLLECTION;

    private RagWorkflowRequestMapper() {
    }

    static RagQueryRequest toQueryRequest(RagWorkflowInput input) {
        RetrievalConfig retrievalConfig = retrievalConfig(input);
        GenerationConfig generationConfig = generationConfig(input);

        return new RagQueryRequest(
                input == null ? "" : RagRuntimeText.trimToEmpty(input.tenantId()),
                input == null ? "" : RagRuntimeText.trimToEmpty(input.query()),
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                retrievalConfig,
                generationConfig,
                RagRuntimeDefaults.defaultCollections(),
                RagWorkflowFilters.copy(retrievalConfig.metadataFilters()));
    }

    private static RetrievalConfig retrievalConfig(RagWorkflowInput input) {
        return RagRuntimeConfigs.retrievalOrDefault(input == null ? null : input.retrievalConfig());
    }

    private static GenerationConfig generationConfig(RagWorkflowInput input) {
        return RagRuntimeConfigs.generationOrDefault(input == null ? null : input.generationConfig());
    }
}
