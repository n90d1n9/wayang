package tech.kayys.wayang.memory.graph;

import tech.kayys.wayang.graph.*;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.MemoryType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for integrating GraphStore with Memory systems.
 * Stores memories as nodes with relationships for structured knowledge.
 */
public class GraphMemoryAdapter {

    private final GraphStore graphStore;

    public GraphMemoryAdapter(GraphStore graphStore) {
        this.graphStore = graphStore;
    }

    /**
     * Store memory as a graph node.
     */
    public String storeMemory(Memory memory) {
        Node.Builder builder = Node.builder()
                .label("Memory")
                .property("content", memory.getContent())
                .property("type", memory.getType().toString())
                .property("timestamp", memory.getTimestamp().toString())
                .property("importance", memory.getImportance());

        memory.getMetadata().forEach(builder::metadata);

        return graphStore.addNode(builder.build());
    }

    /**
     * Store memories with relationships.
     */
    public void storeMemoriesWithRelationships(List<Memory> memories, List<MemoryRelationship> relationships) {
        // Store nodes
        for (Memory memory : memories) {
            storeMemory(memory);
        }

        // Store relationships
        List<Relationship> graphRels = relationships.stream()
                .map(rel -> {
                    Relationship.Builder builder = Relationship.builder()
                            .startNodeId(rel.fromMemoryId)
                            .endNodeId(rel.toMemoryId)
                            .type(rel.relationshipType);
                    rel.properties.forEach(builder::property);
                    return builder.build();
                })
                .collect(Collectors.toList());

        graphStore.addRelationships(graphRels);
    }

    /**
     * Find memories by traversing relationships.
     */
    public List<Memory> findRelatedMemories(String memoryId, String relationshipType, int maxDepth) {
        Node startNode = graphStore.getNode(memoryId).orElse(null);
        if (startNode == null) return List.of();

        List<Relationship> relationships = graphStore.getRelationships(
                memoryId,
                GraphStore.Direction.OUTGOING
        );

        return relationships.stream()
                .filter(r -> relationshipType == null || r.getType().equals(relationshipType))
                .map(r -> graphStore.getNode(r.getEndNodeId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::nodeToMemory)
                .collect(Collectors.toList());
    }

    /**
     * Find paths between memories.
     */
    public List<List<Memory>> findMemoryPaths(String fromMemoryId, String toMemoryId, int maxDepth) {
        List<List<Node>> paths = graphStore.findPaths(fromMemoryId, toMemoryId, maxDepth);

        return paths.stream()
                .map(path -> path.stream()
                        .map(this::nodeToMemory)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * Find memories by concept/label.
     */
    public List<Memory> findMemoriesByConcept(String concept) {
        List<Node> conceptNodes = graphStore.findNodesByLabel(concept);

        return conceptNodes.stream()
                .map(this::nodeToMemory)
                .collect(Collectors.toList());
    }

    /**
     * Get memory statistics from graph.
     */
    public GraphStatistics getStatistics() {
        return graphStore.getStatistics();
    }

    /**
     * Convert Node to Memory.
     */
    private Memory nodeToMemory(Node node) {
        Memory.Builder builder = Memory.builder()
                .id(node.getId())
                .content(node.getProperty("content").toString())
                .metadata(new HashMap<>(node.getMetadata()));

        Object type = node.getProperty("type");
        if (type != null) {
            builder.type(MemoryType.valueOf(type.toString()));
        }

        Object timestamp = node.getProperty("timestamp");
        if (timestamp != null) {
            builder.timestamp(java.time.Instant.parse(timestamp.toString()));
        }

        Object importance = node.getProperty("importance");
        if (importance != null) {
            builder.importance(Double.parseDouble(importance.toString()));
        }

        return builder.build();
    }

    /**
     * Represents a relationship between memories.
     */
    public static class MemoryRelationship {
        public String fromMemoryId;
        public String toMemoryId;
        public String relationshipType;
        public Map<String, Object> properties = new HashMap<>();

        public MemoryRelationship(String fromMemoryId, String toMemoryId, String relationshipType) {
            this.fromMemoryId = fromMemoryId;
            this.toMemoryId = toMemoryId;
            this.relationshipType = relationshipType;
        }

        public MemoryRelationship withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
    }
}
