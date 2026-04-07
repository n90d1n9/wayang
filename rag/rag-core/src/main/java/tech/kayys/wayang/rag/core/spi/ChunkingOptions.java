package tech.kayys.wayang.rag.core.spi;

public record ChunkingOptions(
        int chunkSize,
        int chunkOverlap) {

    public static ChunkingOptions defaults() {
        return new ChunkingOptions(800, 120);
    }
}
