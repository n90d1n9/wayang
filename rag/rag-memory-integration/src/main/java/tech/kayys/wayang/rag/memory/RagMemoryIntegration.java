package tech.kayys.wayang.rag.memory;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadata;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Retriever;
import tech.kayys.wayang.memory.vector.VectorMemoryAdapter;
import tech.kayys.wayang.memory.model.Memory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Integrates RAG retrieval with Memory systems.
 * Combines document retrieval from vector stores with memory retrieval.
 */
public class RagMemoryIntegration {

    private final Retriever ragRetriever;
    private final RagMemoryRetriever memoryRetriever;
    private final double memoryWeight;

    public RagMemoryIntegration(
            Retriever ragRetriever,
            VectorMemoryAdapter vectorMemory,
            double memoryWeight) {
        this(ragRetriever, new VectorMemoryRetriever(vectorMemory), memoryWeight);
    }

    public RagMemoryIntegration(
            Retriever ragRetriever,
            RagMemoryRetriever memoryRetriever,
            double memoryWeight) {
        this.ragRetriever = Objects.requireNonNull(ragRetriever, "ragRetriever");
        this.memoryRetriever = Objects.requireNonNull(memoryRetriever, "memoryRetriever");
        this.memoryWeight = normalizeWeight(memoryWeight);
    }

    /**
     * Retrieve from both RAG documents and memory.
     */
    public Uni<RagResult> retrieveWithMemory(String query, int ragTopK, int memoryTopK) {
        return retrieveWithMemory(query, ragTopK, memoryTopK, Map.of());
    }

    public Uni<RagResult> retrieveWithMemory(
            String query,
            int ragTopK,
            int memoryTopK,
            Map<String, Object> filters) {
        int safeRagTopK = Math.max(0, ragTopK);
        int safeMemoryTopK = Math.max(0, memoryTopK);
        Map<String, Object> safeFilters = RagMetadata.copy(filters);
        RagQuery ragQuery = new RagQuery(query == null ? "" : query, safeRagTopK, 0.0, safeFilters);

        Uni<List<RagScoredChunk>> ragChunks = retrieveRagChunks(ragQuery);
        Uni<List<RagScoredChunk>> memoryChunks = retrieveMemoryChunks(ragQuery.text(), safeMemoryTopK, safeFilters);

        return Uni.combine().all().unis(ragChunks, memoryChunks).asTuple()
                .map(results -> toResult(ragQuery, safeRagTopK, safeMemoryTopK, results.getItem1(), results.getItem2()));
    }

    /**
     * Retrieve with weighted balance between memory and documents.
     */
    public Uni<RagResult> retrieveWeighted(String query, int totalTopK) {
        int safeTotalTopK = Math.max(0, totalTopK);
        int memoryTopK = (int) Math.round(safeTotalTopK * memoryWeight);
        int ragTopK = safeTotalTopK - memoryTopK;

        return retrieveWithMemory(query, ragTopK, memoryTopK);
    }

    public Uni<RagResult> retrieveWeighted(String query, int totalTopK, Map<String, Object> filters) {
        int safeTotalTopK = Math.max(0, totalTopK);
        int memoryTopK = (int) Math.round(safeTotalTopK * memoryWeight);
        int ragTopK = safeTotalTopK - memoryTopK;

        return retrieveWithMemory(query, ragTopK, memoryTopK, filters);
    }

    private Uni<List<RagScoredChunk>> retrieveRagChunks(RagQuery query) {
        if (query.topK() <= 0) {
            return Uni.createFrom().item(List.of());
        }
        return Uni.createFrom().item(() -> {
            List<RagScoredChunk> chunks = ragRetriever.retrieve(query);
            return chunks == null ? List.of() : List.copyOf(chunks);
        });
    }

    private Uni<List<RagScoredChunk>> retrieveMemoryChunks(
            String query,
            int topK,
            Map<String, Object> filters) {
        if (topK <= 0) {
            return Uni.createFrom().item(List.of());
        }
        return memoryRetriever.retrieve(query, topK, filters)
                .map(memories -> memories == null
                        ? List.of()
                        : memories.stream()
                                .filter(Objects::nonNull)
                                .map(this::memoryToScoredChunk)
                                .toList());
    }

    private RagResult toResult(
            RagQuery query,
            int ragTopK,
            int memoryTopK,
            List<RagScoredChunk> ragChunks,
            List<RagScoredChunk> memoryChunks) {
        List<RagScoredChunk> combined = Stream.concat(ragChunks.stream(), memoryChunks.stream()).toList();
        return new RagResult(query, combined, "", Map.of(
                "ragTopK", ragTopK,
                "memoryTopK", memoryTopK,
                "ragChunkCount", ragChunks.size(),
                "memoryChunkCount", memoryChunks.size(),
                "memoryWeight", memoryWeight));
    }

    private RagScoredChunk memoryToScoredChunk(Memory memory) {
        String namespace = memory.getNamespace() == null || memory.getNamespace().isBlank()
                ? "default"
                : memory.getNamespace();
        RagChunk chunk = new RagChunk(
                memory.getId(),
                "memory:" + namespace,
                0,
                memory.getContent(),
                memoryMetadata(memory, namespace));
        return new RagScoredChunk(chunk, memory.getImportance());
    }

    private Map<String, Object> memoryMetadata(Memory memory, String namespace) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (memory.getMetadata() != null) {
            metadata.putAll(memory.getMetadata());
        }
        metadata.put("sourceType", "memory");
        metadata.put("memoryId", memory.getId());
        metadata.put("memoryNamespace", namespace);
        if (memory.getType() != null) {
            metadata.put("memoryType", memory.getType().name());
        }
        if (memory.getTimestamp() != null) {
            metadata.put("memoryTimestamp", memory.getTimestamp().toString());
        }
        return RagMetadata.copy(metadata);
    }

    private static double normalizeWeight(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
