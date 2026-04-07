package tech.kayys.wayang.rag.core;

public record RagScoredChunk(
        RagChunk chunk,
        double score) {
}
