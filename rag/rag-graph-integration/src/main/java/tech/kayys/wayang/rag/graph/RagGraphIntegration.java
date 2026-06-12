package tech.kayys.wayang.rag.graph;

import tech.kayys.wayang.graph.*;
import tech.kayys.wayang.rag.core.*;
import tech.kayys.wayang.rag.core.spi.Retriever;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Integrates RAG with GraphStore for relationship-based retrieval.
 * Enables multi-hop reasoning and structured knowledge retrieval.
 */
public class RagGraphIntegration {

    private final GraphStore graphStore;
    private final Retriever vectorRetriever;

    public RagGraphIntegration(GraphStore graphStore, Retriever vectorRetriever) {
        this.graphStore = graphStore;
        this.vectorRetriever = vectorRetriever;
    }

    /**
     * Retrieve using hybrid vector + graph approach.
     */
    public RagResult retrieveHybrid(String query, int vectorTopK, int graphHops) {
        // 1. Vector retrieval for semantic similarity
        RagQuery vectorQuery = new RagQuery(query, vectorTopK, 0.0, Map.of());
        List<RagScoredChunk> vectorChunks = vectorRetriever.retrieve(vectorQuery);

        // 2. Graph retrieval for relationship expansion
        List<RagScoredChunk> graphChunks = retrieveFromGraph(query, graphHops).stream()
                .map(chunk -> new RagScoredChunk(chunk, 0.5))
                .collect(Collectors.toList());

        // 3. Combine results
        List<RagScoredChunk> combined = new ArrayList<>();
        combined.addAll(vectorChunks);
        combined.addAll(graphChunks);

        return new RagResult(
                vectorQuery,
                combined,
                "",
                Map.of(
                        "vectorCount", vectorChunks.size(),
                        "graphCount", graphChunks.size()));
    }

    /**
     * Retrieve from graph using entity extraction and traversal.
     */
    private List<RagChunk> retrieveFromGraph(String query, int maxHops) {
        // Extract entities from query (simplified - use NLP in production)
        List<String> entities = extractEntities(query);

        List<RagChunk> chunks = new ArrayList<>();

        for (String entity : entities) {
            // Find entity node
            List<Node> entityNodes = graphStore.findNodesByProperty("Entity", "name", entity);

            for (Node entityNode : entityNodes) {
                // Get connected nodes
                List<Relationship> relationships = graphStore.getRelationships(
                        entityNode.getId(),
                        GraphStore.Direction.BOTH
                );

                for (Relationship rel : relationships) {
                    Node connectedNode = graphStore.getNode(
                            rel.getStartNodeId().equals(entityNode.getId()) ?
                                    rel.getEndNodeId() : rel.getStartNodeId()
                    ).orElse(null);

                    if (connectedNode != null) {
                        chunks.add(nodeToRagChunk(connectedNode, rel.getType()));
                    }
                }
            }
        }

        return chunks;
    }

    /**
     * Multi-hop retrieval through graph.
     */
    public List<List<RagChunk>> retrieveMultiHop(String startEntity, String endEntity, int maxHops) {
        List<Node> startNodes = graphStore.findNodesByProperty("Entity", "name", startEntity);
        List<Node> endNodes = graphStore.findNodesByProperty("Entity", "name", endEntity);

        if (startNodes.isEmpty() || endNodes.isEmpty()) {
            return List.of();
        }

        List<List<Node>> paths = graphStore.findPaths(
                startNodes.get(0).getId(),
                endNodes.get(0).getId(),
                maxHops
        );

        return paths.stream()
                .map(path -> path.stream()
                        .map(node -> nodeToRagChunk(node, "path"))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve by relationship type.
     */
    public List<RagChunk> retrieveByRelationship(String entityId, String relationshipType) {
        List<Relationship> relationships = graphStore.getRelationships(
                entityId,
                GraphStore.Direction.OUTGOING
        );

        return relationships.stream()
                .filter(r -> relationshipType == null || r.getType().equals(relationshipType))
                .flatMap(r -> graphStore.getNode(r.getEndNodeId())
                        .map(node -> nodeToRagChunk(node, r.getType()))
                        .stream())
                .collect(Collectors.toList());
    }

    /**
     * Convert Node to RagChunk.
     */
    private RagChunk nodeToRagChunk(Node node, String relationshipType) {
        Object content = node.getProperty("content");
        String text = content != null ? content.toString() : node.getProperties().toString();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nodeId", node.getId());
        metadata.put("label", node.getLabel());
        metadata.put("relationship", relationshipType);
        metadata.put("source", "graph:" + node.getLabel());

        return RagChunk.of(
                "graph:" + node.getId(),
                0,
                text,
                metadata);
    }

    /**
     * Simple entity extraction (use NLP in production).
     */
    private List<String> extractEntities(String query) {
        // Simplified: extract capitalized words as entities
        return Arrays.stream(query.split("\\s+"))
                .filter(word -> word.length() > 1 && Character.isUpperCase(word.charAt(0)))
                .map(word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }
}
