package tech.kayys.wayang.rag.core.spi;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagDocument;

import java.util.List;

public interface Chunker {
    List<RagChunk> chunk(RagDocument document, ChunkingOptions options);
}
