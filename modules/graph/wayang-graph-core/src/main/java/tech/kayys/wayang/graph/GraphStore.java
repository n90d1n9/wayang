package tech.kayys.wayang.graph;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core interface for graph-based storage and querying.
 * Provides an abstraction for storing and querying knowledge graphs,
 * relationship networks, and structured data for AI agents.
 */
public interface GraphStore {

    /**
     * Initialize the graph store.
     */
    void initialize();

    /**
     * Close the graph store and release resources.
     */
    void close();

    /**
     * Add a node to the graph.
     *
     * @param node The node to add
     * @return The ID of the created node
     */
    String addNode(Node node);

    /**
     * Add multiple nodes to the graph.
     *
     * @param nodes List of nodes to add
     * @return List of created node IDs
     */
    List<String> addNodes(List<Node> nodes);

    /**
     * Get a node by ID.
     *
     * @param nodeId The node ID
     * @return Optional containing the node if found
     */
    Optional<Node> getNode(String nodeId);

    /**
     * Find nodes by label.
     *
     * @param label The node label
     * @return List of matching nodes
     */
    List<Node> findNodesByLabel(String label);

    /**
     * Find nodes by property.
     *
     * @param label The node label
     * @param property The property name
     * @param value The property value
     * @return List of matching nodes
     */
    List<Node> findNodesByProperty(String label, String property, Object value);

    /**
     * Update a node.
     *
     * @param nodeId The node ID
     * @param node The updated node
     * @return true if updated successfully
     */
    boolean updateNode(String nodeId, Node node);

    /**
     * Delete a node.
     *
     * @param nodeId The node ID
     * @return true if deleted successfully
     */
    boolean deleteNode(String nodeId);

    /**
     * Add a relationship between two nodes.
     *
     * @param relationship The relationship to add
     * @return The ID of the created relationship
     */
    String addRelationship(Relationship relationship);

    /**
     * Add multiple relationships.
     *
     * @param relationships List of relationships to add
     * @return List of created relationship IDs
     */
    List<String> addRelationships(List<Relationship> relationships);

    /**
     * Get relationships for a node.
     *
     * @param nodeId The node ID
     * @param direction The direction (INCOMING, OUTGOING, BOTH)
     * @return List of relationships
     */
    List<Relationship> getRelationships(String nodeId, Direction direction);

    /**
     * Find relationships by type.
     *
     * @param type The relationship type
     * @return List of matching relationships
     */
    List<Relationship> findRelationshipsByType(String type);

    /**
     * Delete a relationship.
     *
     * @param relationshipId The relationship ID
     * @return true if deleted successfully
     */
    boolean deleteRelationship(String relationshipId);

    /**
     * Execute a Cypher query (for Cypher-compatible stores).
     *
     * @param query The Cypher query
     * @param parameters Query parameters
     * @return List of result maps
     */
    List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters);

    /**
     * Find paths between two nodes.
     *
     * @param startNodeId Start node ID
     * @param endNodeId End node ID
     * @param maxDepth Maximum path depth
     * @return List of paths (each path is a list of nodes)
     */
    List<List<Node>> findPaths(String startNodeId, String endNodeId, int maxDepth);

    /**
     * Get graph statistics.
     *
     * @return Graph statistics
     */
    GraphStatistics getStatistics();

    /**
     * Direction enum for relationship traversal.
     */
    enum Direction {
        INCOMING,
        OUTGOING,
        BOTH
    }
}
