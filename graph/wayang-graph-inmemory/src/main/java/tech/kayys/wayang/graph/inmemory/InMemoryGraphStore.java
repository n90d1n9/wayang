package tech.kayys.wayang.graph.inmemory;

import tech.kayys.wayang.graph.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory implementation of {@link GraphStore}.
 * Suitable for testing, prototyping, and lightweight agent deployments.
 *
 * <p>Supports:
 * <ul>
 *   <li>Full CRUD for nodes and relationships</li>
 *   <li>BFS-based path finding (inherited + direct)</li>
 *   <li>Basic Cypher-like query parsing for simple MATCH patterns</li>
 * </ul>
 */
public class InMemoryGraphStore extends AbstractGraphStore {

    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, Relationship> relationships = new ConcurrentHashMap<>();
    // nodeId → set of relationship IDs touching that node
    private final Map<String, Set<String>> nodeRelationships = new ConcurrentHashMap<>();

    @Override
    protected void doInitialize() {
        // Nothing to initialize for in-memory store
    }

    @Override
    protected void doClose() {
        nodes.clear();
        relationships.clear();
        nodeRelationships.clear();
    }

    // ── Node CRUD ───────────────────────────────────────────────────

    @Override
    public String addNode(Node node) {
        if (node.getId() == null) {
            node.setId(UUID.randomUUID().toString());
        }
        nodes.put(node.getId(), node);
        nodeRelationships.computeIfAbsent(node.getId(), k -> ConcurrentHashMap.newKeySet());
        return node.getId();
    }

    @Override
    public Optional<Node> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public List<Node> findNodesByLabel(String label) {
        return nodes.values().stream()
                .filter(n -> label.equals(n.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Node> findNodesByProperty(String label, String property, Object value) {
        return nodes.values().stream()
                .filter(n -> label.equals(n.getLabel()))
                .filter(n -> value.equals(n.getProperty(property)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateNode(String nodeId, Node node) {
        if (!nodes.containsKey(nodeId)) return false;
        node.setId(nodeId);
        nodes.put(nodeId, node);
        return true;
    }

    @Override
    public boolean deleteNode(String nodeId) {
        Set<String> relIds = nodeRelationships.remove(nodeId);
        if (relIds != null) {
            for (String relId : relIds) {
                Relationship rel = relationships.remove(relId);
                if (rel != null) {
                    // Clean up the other end's index
                    String otherId = rel.getStartNodeId().equals(nodeId)
                            ? rel.getEndNodeId() : rel.getStartNodeId();
                    Set<String> otherRels = nodeRelationships.get(otherId);
                    if (otherRels != null) otherRels.remove(relId);
                }
            }
        }
        return nodes.remove(nodeId) != null;
    }

    // ── Relationship CRUD ───────────────────────────────────────────

    @Override
    public String addRelationship(Relationship relationship) {
        if (relationship.getId() == null) {
            relationship.setId(UUID.randomUUID().toString());
        }
        relationships.put(relationship.getId(), relationship);
        nodeRelationships
                .computeIfAbsent(relationship.getStartNodeId(), k -> ConcurrentHashMap.newKeySet())
                .add(relationship.getId());
        nodeRelationships
                .computeIfAbsent(relationship.getEndNodeId(), k -> ConcurrentHashMap.newKeySet())
                .add(relationship.getId());
        return relationship.getId();
    }

    @Override
    public List<Relationship> getRelationships(String nodeId, Direction direction) {
        Set<String> relIds = nodeRelationships.get(nodeId);
        if (relIds == null) return List.of();

        return relIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(r -> switch (direction) {
                    case OUTGOING -> r.getStartNodeId().equals(nodeId);
                    case INCOMING -> r.getEndNodeId().equals(nodeId);
                    case BOTH -> true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Relationship> findRelationshipsByType(String type) {
        return relationships.values().stream()
                .filter(r -> type.equals(r.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteRelationship(String relationshipId) {
        Relationship rel = relationships.remove(relationshipId);
        if (rel == null) return false;
        Set<String> startRels = nodeRelationships.get(rel.getStartNodeId());
        if (startRels != null) startRels.remove(relationshipId);
        Set<String> endRels = nodeRelationships.get(rel.getEndNodeId());
        if (endRels != null) endRels.remove(relationshipId);
        return true;
    }

    // ── Cypher-like query support ────────────────────────────────────

    /**
     * Executes a simplified Cypher-like query against the in-memory graph.
     *
     * <p>Supported patterns:
     * <ul>
     *   <li>{@code MATCH (n:Label) RETURN n} – finds all nodes by label</li>
     *   <li>{@code MATCH (n:Label) WHERE n.prop = $val RETURN n} – property filter</li>
     * </ul>
     *
     * <p>For complex Cypher queries, use the Neo4j implementation.
     */
    @Override
    public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
        if (query == null || query.isBlank()) return List.of();

        String normalized = query.trim().toUpperCase();

        // Pattern: MATCH (n:Label) WHERE n.prop = $param RETURN n
        Pattern matchWhere = Pattern.compile(
                "MATCH\\s+\\(\\w+:(\\w+)\\)\\s+WHERE\\s+\\w+\\.(\\w+)\\s*=\\s*\\$(\\w+)\\s+RETURN",
                Pattern.CASE_INSENSITIVE);
        Matcher mw = matchWhere.matcher(query.trim());
        if (mw.find()) {
            String label = mw.group(1);
            String prop = mw.group(2);
            String paramName = mw.group(3);
            Object value = parameters != null ? parameters.get(paramName) : null;

            return findNodesByProperty(label, prop, value).stream()
                    .map(this::nodeToMap)
                    .collect(Collectors.toList());
        }

        // Pattern: MATCH (n:Label) RETURN n
        Pattern matchLabel = Pattern.compile(
                "MATCH\\s+\\(\\w+:(\\w+)\\)\\s+RETURN",
                Pattern.CASE_INSENSITIVE);
        Matcher ml = matchLabel.matcher(query.trim());
        if (ml.find()) {
            String label = ml.group(1);
            return findNodesByLabel(label).stream()
                    .map(this::nodeToMap)
                    .collect(Collectors.toList());
        }

        throw new UnsupportedOperationException(
                "In-memory store only supports simple MATCH patterns. Use Neo4j for full Cypher.");
    }

    private Map<String, Object> nodeToMap(Node node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.getId());
        map.put("label", node.getLabel());
        map.putAll(node.getProperties());
        return map;
    }

    // ── Statistics ───────────────────────────────────────────────────

    @Override
    public GraphStatistics getStatistics() {
        long labels = nodes.values().stream()
                .map(Node::getLabel)
                .filter(Objects::nonNull)
                .distinct().count();
        long types = relationships.values().stream()
                .map(Relationship::getType)
                .filter(Objects::nonNull)
                .distinct().count();
        return new GraphStatistics(nodes.size(), relationships.size(), labels, types);
    }
}
