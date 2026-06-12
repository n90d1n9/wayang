package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeRagChunkingOptionsTest {

    @Test
    void defaultsNullConfigToRuntimeChunkingDefaults() {
        ChunkingOptions options = NativeRagChunkingOptions.from(null);

        assertEquals(ChunkingConfig.defaults().chunkSize(), options.chunkSize());
        assertEquals(ChunkingConfig.defaults().chunkOverlap(), options.chunkOverlap());
    }

    @Test
    void clampsInvalidChunkingConfig() {
        ChunkingOptions options = NativeRagChunkingOptions.from(new ChunkingConfig(0, 50));

        assertEquals(1, options.chunkSize());
        assertEquals(0, options.chunkOverlap());
    }

    @Test
    void capsOverlapBelowChunkSize() {
        ChunkingOptions options = NativeRagChunkingOptions.from(new ChunkingConfig(10, 10));

        assertEquals(10, options.chunkSize());
        assertEquals(9, options.chunkOverlap());
    }
}
