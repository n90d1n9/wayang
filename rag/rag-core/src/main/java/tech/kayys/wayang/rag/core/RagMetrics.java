package tech.kayys.wayang.rag.core;

/**
 * Metrics for RAG operation performance and quality.
 */
public class RagMetrics {
    private final long totalDurationMs;
    private final int documentsRetrieved;
    private final int tokensGenerated;
    private final float averageSimilarityScore;
    private final int rerankedResults;
    private final int hallucinationScore;
    private final boolean groundingVerified;

    public RagMetrics(long totalDurationMs, int documentsRetrieved, int tokensGenerated,
            float averageSimilarityScore, int rerankedResults, int hallucinationScore,
            boolean groundingVerified) {
        this.totalDurationMs = totalDurationMs;
        this.documentsRetrieved = documentsRetrieved;
        this.tokensGenerated = tokensGenerated;
        this.averageSimilarityScore = averageSimilarityScore;
        this.rerankedResults = rerankedResults;
        this.hallucinationScore = hallucinationScore;
        this.groundingVerified = groundingVerified;
    }

    public long totalDurationMs() {
        return totalDurationMs;
    }

    public int documentsRetrieved() {
        return documentsRetrieved;
    }

    public int tokensGenerated() {
        return tokensGenerated;
    }

    public float averageSimilarityScore() {
        return averageSimilarityScore;
    }

    public int rerankedResults() {
        return rerankedResults;
    }

    public int hallucinationScore() {
        return hallucinationScore;
    }

    public boolean groundingVerified() {
        return groundingVerified;
    }
}
