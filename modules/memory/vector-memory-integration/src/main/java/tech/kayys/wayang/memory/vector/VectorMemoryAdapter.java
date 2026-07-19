package tech.kayys.wayang.memory.vector;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.vector.VectorEntry;
import tech.kayys.wayang.vector.VectorQuery;
import tech.kayys.wayang.vector.VectorStore;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.MemoryType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter for integrating VectorStore with Memory systems.
 * Converts between Memory objects and VectorEntry objects.
 */
public class VectorMemoryAdapter {

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    @Inject
    EmbeddingService embeddingService;

    public VectorMemoryAdapter(VectorStore vectorStore, double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Store memories as vector entries.
     */
    public Uni<Void> storeMemories(List<Memory> memories) {
        List<VectorEntry> entries = memories.stream()
                .map(this::memoryToVectorEntry)
                .collect(Collectors.toList());
        return vectorStore.store(entries);
    }

    /**
     * Search for similar memories.
     */
    public Uni<List<Memory>> searchSimilarMemories(String query, int topK) {
        return embeddingService.embedOne(query)
                .onItem().transformToUni(vector -> {
                    VectorQuery vectorQuery = new VectorQuery(floatArrayToList(vector), topK, (float) similarityThreshold);
                    return vectorStore.search(vectorQuery);
                })
                .onItem().transform(entries -> entries.stream()
                        .map(this::vectorEntryToMemory)
                        .collect(Collectors.toList()));
    }

    /**
     * Search with filters.
     */
    public Uni<List<Memory>> searchMemories(String query, int topK, Map<String, Object> filters) {
        return embeddingService.embedOne(query)
                .onItem().transformToUni(vector -> {
                    VectorQuery vectorQuery = new VectorQuery(floatArrayToList(vector), topK, (float) similarityThreshold);
                    return vectorStore.search(vectorQuery, filters);
                })
                .onItem().transform(entries -> entries.stream()
                        .map(this::vectorEntryToMemory)
                        .collect(Collectors.toList()));
    }

    /**
     * Delete memories by IDs.
     */
    public Uni<Void> deleteMemories(List<String> memoryIds) {
        return vectorStore.delete(memoryIds);
    }

    /**
     * Convert Memory to VectorEntry.
     */
    private VectorEntry memoryToVectorEntry(Memory memory) {
        List<Float> vector = floatArrayToList(memory.getEmbedding());
        return new VectorEntry(
                memory.getId(),
                vector,
                memory.getContent(),
                Map.ofEntries(
                        Map.entry("type", memory.getType().toString()),
                        Map.entry("timestamp", memory.getTimestamp().toString()),
                        Map.entry("namespace", memory.getNamespace() != null ? memory.getNamespace() : ""),
                        Map.entry("importance", memory.getImportance())
                ));
    }

    /**
     * Convert VectorEntry to Memory.
     */
    private Memory vectorEntryToMemory(VectorEntry entry) {
        Memory.Builder builder = Memory.builder()
                .id(entry.id())
                .content(entry.content())
                .metadata(entry.metadata());

        if (entry.vector() != null) {
            builder.embedding(listToFloatArray(entry.vector()));
        }

        if (entry.metadata() != null) {
            Object type = entry.metadata().get("type");
            if (type != null) {
                builder.type(MemoryType.valueOf(type.toString()));
            }
            Object timestamp = entry.metadata().get("timestamp");
            if (timestamp != null) {
                builder.timestamp(Instant.parse(timestamp.toString()));
            }
            Object namespace = entry.metadata().get("namespace");
            if (namespace != null) {
                builder.namespace(namespace.toString());
            }
            Object importance = entry.metadata().get("importance");
            if (importance != null) {
                builder.importance(Double.parseDouble(importance.toString()));
            }
        }

        return builder.build();
    }

    private List<Float> floatArrayToList(float[] array) {
        if (array == null) return null;
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    private float[] listToFloatArray(List<Float> list) {
        if (list == null) return null;
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
