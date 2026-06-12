package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import java.util.LinkedHashMap;
import java.util.Map;

final class RagResponseMetadata {

    static final String RAG_MODE = "ragMode";
    static final String SEARCH_STRATEGY = "searchStrategy";
    static final String GENERATION_CONFIG = "generationConfig";
    static final String RETRIEVAL_CONFIG = "retrievalConfig";

    static final String PROVIDER = "provider";
    static final String MODEL = "model";
    static final String TEMPERATURE = "temperature";
    static final String MAX_TOKENS = "maxTokens";
    static final String SYSTEM_PROMPT = "systemPrompt";
    static final String ADDITIONAL_PARAMS = "additionalParams";

    static final String TOP_K = "topK";
    static final String MIN_SIMILARITY = "minSimilarity";
    static final String MAX_CHUNK_SIZE = "maxChunkSize";
    static final String CHUNK_OVERLAP = "chunkOverlap";
    static final String ENABLE_RERANKING = "enableReranking";
    static final String ENABLE_HYBRID_SEARCH = "enableHybridSearch";
    static final String HYBRID_ALPHA = "hybridAlpha";

    private RagResponseMetadata() {
    }

    static Map<String, Object> from(RagQueryWorkflowContext context) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RAG_MODE, context.mode().name());
        metadata.put(SEARCH_STRATEGY, context.strategy().name());
        metadata.put(GENERATION_CONFIG, generationConfig(context.generationConfig()));
        metadata.put(RETRIEVAL_CONFIG, retrievalConfig(context.retrievalConfig()));
        return unmodifiable(metadata);
    }

    static Map<String, Object> retrievalConfig(RetrievalConfig config) {
        RetrievalConfig effectiveConfig = RagRuntimeConfigs.retrievalOrDefault(config);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(TOP_K, effectiveConfig.topK());
        metadata.put(MIN_SIMILARITY, effectiveConfig.minSimilarity());
        metadata.put(MAX_CHUNK_SIZE, effectiveConfig.maxChunkSize());
        metadata.put(CHUNK_OVERLAP, effectiveConfig.chunkOverlap());
        metadata.put(ENABLE_RERANKING, effectiveConfig.enableReranking());
        metadata.put(ENABLE_HYBRID_SEARCH, effectiveConfig.enableHybridSearch());
        metadata.put(HYBRID_ALPHA, effectiveConfig.hybridAlpha());
        return unmodifiable(metadata);
    }

    static Map<String, Object> generationConfig(GenerationConfig config) {
        GenerationConfig effectiveConfig = RagRuntimeConfigs.generationOrDefault(config);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(PROVIDER, effectiveConfig.provider());
        metadata.put(MODEL, effectiveConfig.model());
        metadata.put(TEMPERATURE, effectiveConfig.temperature());
        metadata.put(MAX_TOKENS, effectiveConfig.maxTokens());
        metadata.put(SYSTEM_PROMPT, effectiveConfig.systemPrompt());
        metadata.put(
                NativeGenerationMode.PARAM_NATIVE_GENERATION_MODE,
                NativeGenerationMode.from(effectiveConfig).name());
        metadata.put(ADDITIONAL_PARAMS, effectiveConfig.additionalParams());
        return unmodifiable(metadata);
    }

    private static Map<String, Object> unmodifiable(Map<String, Object> metadata) {
        return RagRuntimeMetadata.copy(metadata);
    }
}
