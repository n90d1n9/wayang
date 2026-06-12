package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.SourceDocument;

import java.util.List;
import java.util.Map;

final class RagSourceDocuments {

    private RagSourceDocuments() {
    }

    static List<SourceDocument> fromChunks(List<RagScoredChunk> chunks) {
        return RagScoredChunks.valid(chunks).stream()
                .map(RagSourceDocuments::fromChunk)
                .toList();
    }

    static SourceDocument fromChunk(RagScoredChunk scoredChunk) {
        RagChunk chunk = scoredChunk.chunk();
        Map<String, String> metadata = RagRuntimeMetadata.stringifyValues(chunk.metadata());
        String source = metadata.getOrDefault(RagMetadataKeys.SOURCE, chunk.documentId());
        return new SourceDocument(
                chunk.id(),
                source,
                chunk.text(),
                source,
                metadata,
                (float) scoredChunk.score(),
                -1,
                "");
    }
}
