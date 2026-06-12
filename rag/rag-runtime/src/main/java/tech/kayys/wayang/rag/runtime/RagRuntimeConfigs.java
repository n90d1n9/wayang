package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RetrievalConfig;

final class RagRuntimeConfigs {

    private RagRuntimeConfigs() {
    }

    static RetrievalConfig retrievalOrDefault(RetrievalConfig config) {
        return config == null ? RetrievalConfig.defaults() : config;
    }

    static GenerationConfig generationOrDefault(GenerationConfig config) {
        return config == null ? GenerationConfig.defaults() : config;
    }
}
