package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagDocument;
import tech.kayys.wayang.rag.core.spi.Chunker;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SlidingWindowChunker implements Chunker {

    @Override
    public List<RagChunk> chunk(RagDocument document, ChunkingOptions options) {
        if (document.content() == null || document.content().isBlank()) {
            return List.of();
        }

        int chunkSize = Math.max(1, options.chunkSize());
        int overlap = Math.max(0, Math.min(options.chunkOverlap(), chunkSize - 1));
        int step = chunkSize - overlap;

        List<RagChunk> chunks = new ArrayList<>();
        String text = document.content();
        int chunkIndex = 0;

        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + chunkSize);
            String fragment = text.substring(start, end).trim();
            if (!fragment.isEmpty()) {
                chunks.add(RagChunk.of(
                        document.id(),
                        chunkIndex++,
                        fragment,
                        Map.of("source", document.metadata().getOrDefault("source", document.id()), "start", start,
                                "end", end)));
            }
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }
}
