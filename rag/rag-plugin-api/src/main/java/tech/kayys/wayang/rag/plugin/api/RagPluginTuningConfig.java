package tech.kayys.wayang.rag.plugin.api;

/**
 * Interface for RAG plugin tuning configuration.
 */
public interface RagPluginTuningConfig {

    static RagPluginTuningConfig defaults() {
        return DefaultRagPluginTuningConfig.INSTANCE;
    }

    boolean normalizeQueryLowercase();

    int normalizeQueryMaxLength();

    double lexicalRerankOriginalWeight();

    double lexicalRerankLexicalWeight();

    boolean lexicalRerankAnnotateMetadata();

    String safetyFilterBlockedTerms();

    String safetyFilterMask();
}

enum DefaultRagPluginTuningConfig implements RagPluginTuningConfig {
    INSTANCE;

    @Override
    public boolean normalizeQueryLowercase() {
        return false;
    }

    @Override
    public int normalizeQueryMaxLength() {
        return 4096;
    }

    @Override
    public double lexicalRerankOriginalWeight() {
        return 0.7;
    }

    @Override
    public double lexicalRerankLexicalWeight() {
        return 0.3;
    }

    @Override
    public boolean lexicalRerankAnnotateMetadata() {
        return true;
    }

    @Override
    public String safetyFilterBlockedTerms() {
        return "";
    }

    @Override
    public String safetyFilterMask() {
        return "[REDACTED]";
    }
}
