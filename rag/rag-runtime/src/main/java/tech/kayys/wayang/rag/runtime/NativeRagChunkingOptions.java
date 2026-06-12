package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;

final class NativeRagChunkingOptions {

    private NativeRagChunkingOptions() {
    }

    static ChunkingOptions from(ChunkingConfig chunkingConfig) {
        ChunkingConfig effectiveConfig = chunkingConfig == null
                ? ChunkingConfig.defaults()
                : chunkingConfig;
        int chunkSize = Math.max(1, effectiveConfig.chunkSize());
        int chunkOverlap = Math.max(0, Math.min(effectiveConfig.chunkOverlap(), chunkSize - 1));
        return new ChunkingOptions(chunkSize, chunkOverlap);
    }
}
