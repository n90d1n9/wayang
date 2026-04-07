package tech.kayys.wayang.rag.plugin.api;

/**
 * Interface for RAG plugin tuning configuration.
 */
public interface RagPluginTuningConfig {

    boolean normalizeQueryLowercase();

    int normalizeQueryMaxLength();

    double lexicalRerankOriginalWeight();

    double lexicalRerankLexicalWeight();

    boolean lexicalRerankAnnotateMetadata();

    String safetyFilterBlockedTerms();

    String safetyFilterMask();
}
