package tech.kayys.wayang.rag.core;

public record ScoredDocument(RagChunk segment, double score)
        implements Comparable<ScoredDocument> {
    @Override
    public int compareTo(ScoredDocument other) {
        return Double.compare(other.score, this.score);
    }
}