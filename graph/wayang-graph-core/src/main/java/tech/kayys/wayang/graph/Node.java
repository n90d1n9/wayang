package tech.kayys.wayang.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a node in the graph.
 */
public class Node {

    private String id;
    private String label;
    private Map<String, Object> properties;
    private Map<String, Object> metadata;

    public Node() {
        this.id = UUID.randomUUID().toString();
        this.properties = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    public Node(String label) {
        this();
        this.label = label;
    }

    public Node(String id, String label) {
        this.id = id;
        this.label = label;
        this.properties = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Node addProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Node addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", properties=" + properties +
                '}';
    }

    /**
     * Create a builder for Node.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String label;
        private Map<String, Object> properties = new HashMap<>();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Node build() {
            Node node = new Node(id, label);
            node.properties = this.properties;
            node.metadata = this.metadata;
            return node;
        }
    }
}
