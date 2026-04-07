package tech.kayys.wayang.rag.embedding;

/**
 * Metrics callback interface for embedding operations.
 * Decouples the embedding module from rag-runtime's RagObservabilityMetrics.
 */
public interface EmbeddingMetrics {

    void recordEmbeddingSuccess(String model, int tokenCount, long durationMs);

    void recordEmbeddingFailure(String model);

    void recordIngestion(String namespace, int documentCount, int chunkCount, long durationMs);

    void recordSearchSuccess(String namespace, long durationMs, int hitCount);

    void recordSearchFailure(String namespace);

    /** No-op implementation for when metrics are not available. */
    EmbeddingMetrics NOOP = new EmbeddingMetrics() {
        @Override
        public void recordEmbeddingSuccess(String model, int tokenCount, long durationMs) {
        }

        @Override
        public void recordEmbeddingFailure(String model) {
        }

        @Override
        public void recordIngestion(String namespace, int documentCount, int chunkCount, long durationMs) {
        }

        @Override
        public void recordSearchSuccess(String namespace, long durationMs, int hitCount) {
        }

        @Override
        public void recordSearchFailure(String namespace) {
        }
    };
}
