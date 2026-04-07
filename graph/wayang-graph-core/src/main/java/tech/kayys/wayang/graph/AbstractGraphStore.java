package tech.kayys.wayang.graph;

import java.util.*;

/**
 * Abstract base implementation of GraphStore.
 * Provides default implementations for batch operations,
 * an initialization guard, and a BFS-based path finder.
 */
public abstract class AbstractGraphStore implements GraphStore {

    protected boolean initialized = false;

    @Override
    public void initialize() {
        if (!initialized) {
            doInitialize();
            initialized = true;
        }
    }

    protected abstract void doInitialize();

    @Override
    public void close() {
        doClose();
        initialized = false;
    }

    protected abstract void doClose();

    protected void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("GraphStore has not been initialized. Call initialize() first.");
        }
    }

    // ── Batch defaults ──────────────────────────────────────────────

    @Override
    public List<String> addNodes(List<Node> nodes) {
        List<String> ids = new ArrayList<>(nodes.size());
        for (Node n : nodes) ids.add(addNode(n));
        return ids;
    }

    @Override
    public List<String> addRelationships(List<Relationship> relationships) {
        List<String> ids = new ArrayList<>(relationships.size());
        for (Relationship r : relationships) ids.add(addRelationship(r));
        return ids;
    }

    // ── Optional‑operation stubs ────────────────────────────────────

    @Override
    public Optional<Node> getNode(String nodeId) { return Optional.empty(); }

    @Override
    public List<Node> findNodesByLabel(String label) { return List.of(); }

    @Override
    public List<Node> findNodesByProperty(String label, String property, Object value) { return List.of(); }

    @Override
    public boolean updateNode(String nodeId, Node node) { return false; }

    @Override
    public boolean deleteNode(String nodeId) { return false; }

    @Override
    public List<Relationship> getRelationships(String nodeId, Direction direction) { return List.of(); }

    @Override
    public List<Relationship> findRelationshipsByType(String type) { return List.of(); }

    @Override
    public boolean deleteRelationship(String relationshipId) { return false; }

    @Override
    public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
        throw new UnsupportedOperationException("Cypher queries not supported by this store");
    }

    // ── BFS path finder ─────────────────────────────────────────────

    /**
     * Default BFS implementation for finding all paths between two nodes,
     * up to a given depth. Sub‑classes with native path‑finding (e.g. Neo4j)
     * should override this for better performance.
     */
    @Override
    public List<List<Node>> findPaths(String startNodeId, String endNodeId, int maxDepth) {
        List<List<Node>> result = new ArrayList<>();
        Optional<Node> startOpt = getNode(startNodeId);
        if (startOpt.isEmpty()) return result;

        // BFS with path tracking
        Deque<List<String>> queue = new ArrayDeque<>();
        queue.add(List.of(startNodeId));

        while (!queue.isEmpty()) {
            List<String> currentPath = queue.poll();
            if (currentPath.size() > maxDepth + 1) continue;

            String lastId = currentPath.get(currentPath.size() - 1);

            if (lastId.equals(endNodeId) && currentPath.size() > 1) {
                // Convert ID path to Node path
                List<Node> nodePath = new ArrayList<>();
                for (String id : currentPath) {
                    getNode(id).ifPresent(nodePath::add);
                }
                result.add(nodePath);
                continue;
            }

            // Expand neighbours
            for (Relationship rel : getRelationships(lastId, Direction.BOTH)) {
                String neighbourId = rel.getStartNodeId().equals(lastId)
                        ? rel.getEndNodeId()
                        : rel.getStartNodeId();

                if (!currentPath.contains(neighbourId)) {
                    List<String> newPath = new ArrayList<>(currentPath);
                    newPath.add(neighbourId);
                    queue.add(newPath);
                }
            }
        }
        return result;
    }

    @Override
    public GraphStatistics getStatistics() {
        return new GraphStatistics(0, 0, 0, 0);
    }
}
