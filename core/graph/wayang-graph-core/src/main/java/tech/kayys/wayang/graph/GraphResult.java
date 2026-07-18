package tech.kayys.wayang.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps the result of a graph query or traversal operation.
 */
public class GraphResult {

    private List<Node> nodes;
    private List<Relationship> relationships;
    private List<List<Node>> paths;
    private Map<String, Object> metadata;

    public GraphResult() {
        this.nodes = new ArrayList<>();
        this.relationships = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public List<Node> getNodes() { return nodes; }
    public void setNodes(List<Node> nodes) { this.nodes = nodes; }

    public List<Relationship> getRelationships() { return relationships; }
    public void setRelationships(List<Relationship> relationships) { this.relationships = relationships; }

    public List<List<Node>> getPaths() { return paths; }
    public void setPaths(List<List<Node>> paths) { this.paths = paths; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public GraphResult addNode(Node node) { this.nodes.add(node); return this; }
    public GraphResult addRelationship(Relationship rel) { this.relationships.add(rel); return this; }
    public GraphResult addPath(List<Node> path) { this.paths.add(path); return this; }
    public GraphResult addMeta(String key, Object value) { this.metadata.put(key, value); return this; }

    public boolean isEmpty() {
        return nodes.isEmpty() && relationships.isEmpty() && paths.isEmpty();
    }

    public static GraphResult ofNodes(List<Node> nodes) {
        GraphResult r = new GraphResult();
        r.nodes = nodes;
        return r;
    }

    public static GraphResult ofPaths(List<List<Node>> paths) {
        GraphResult r = new GraphResult();
        r.paths = paths;
        return r;
    }

    @Override
    public String toString() {
        return "GraphResult{nodes=" + nodes.size() +
               ", relationships=" + relationships.size() +
               ", paths=" + paths.size() + "}";
    }
}
