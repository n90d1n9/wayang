package tech.kayys.wayang.rag.core;

/**
 * Supported search strategies for RAG retrieval.
 */
public enum SearchStrategy {
    SEMANTIC,
    HYBRID,
    SEMANTIC_RERANK,
    MULTI_QUERY
}
