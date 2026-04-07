package tech.kayys.wayang.rag.memory;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Retriever;
import tech.kayys.wayang.memory.vector.VectorMemoryAdapter;
import tech.kayys.wayang.memory.model.Memory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Integrates RAG retrieval with Memory systems.
 * Combines document retrieval from vector stores with memory retrieval.
 */
public class RagMemoryIntegration {

    private final Retriever ragRetriever;
    private final VectorMemoryAdapter vectorMemory;
    private final double memoryWeight;

    public RagMemoryIntegration(Retriever ragRetriever, 
                                 VectorMemoryAdapter vectorMemory,
                                 double memoryWeight) {
        this.ragRetriever = ragRetriever;
        this.vectorMemory = vectorMemory;
        this.memoryWeight = memoryWeight;
    }

    /**
     * Retrieve from both RAG documents and memory.
     */
    public Uni<RagResult> retrieveWithMemory(String query, int ragTopK, int memoryTopK) {
        // Retrieve from RAG (blocking)
        RagQuery ragQuery = new RagQuery(query, ragTopK, 0.0, Map.of());
        List<RagScoredChunk> ragChunks = ragRetriever.retrieve(ragQuery);

        // Retrieve from memory (reactive)
        return vectorMemory.searchSimilarMemories(query, memoryTopK)
                .map(memories -> {
                    // Convert memories to RAG scored chunks
                    List<RagScoredChunk> memoryChunks = memories.stream()
                            .map(this::memoryToScoredChunk)
                            .collect(Collectors.toList());

                    // Combine results
                    List<RagScoredChunk> combined = Stream.concat(
                                    ragChunks.stream(),
                                    memoryChunks.stream())
                            .collect(Collectors.toList());

                    return new RagResult(ragQuery, combined, "", Map.of());
                });
    }

    /**
     * Retrieve with weighted balance between memory and documents.
     */
    public Uni<RagResult> retrieveWeighted(String query, int totalTopK) {
        int ragTopK = (int) (totalTopK * (1.0 - memoryWeight));
        int memoryTopK = totalTopK - ragTopK;

        return retrieveWithMemory(query, ragTopK, memoryTopK);
    }

    /**
     * Convert Memory to RagScoredChunk.
     */
    private RagScoredChunk memoryToScoredChunk(Memory memory) {
        RagChunk chunk = RagChunk.of(
                memory.getNamespace() != null ? memory.getNamespace() : "default",
                0,
                memory.getContent(),
                memory.getMetadata()
        );
        return new RagScoredChunk(chunk, memory.getImportance());
    }
}
