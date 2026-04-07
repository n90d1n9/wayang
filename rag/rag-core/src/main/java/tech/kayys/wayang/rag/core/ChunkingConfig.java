package tech.kayys.wayang.rag.core;

/**
 * Configuration for document chunking operations.
 */
public record ChunkingConfig(int chunkSize, int chunkOverlap) {
    public static ChunkingConfig defaults() {
        return new ChunkingConfig(512, 50);
    }
}
