package tech.kayys.wayang.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a relationship between two nodes.
 */
public class Relationship {

    private String id;
    private String startNodeId;
    private String endNodeId;
    private String type;
    private Map<String, Object> properties;

    public Relationship() {
        this.id = UUID.randomUUID().toString();
        this.properties = new HashMap<>();
    }

    public Relationship(String startNodeId, String endNodeId, String type) {
        this();
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(String startNodeId) {
        this.startNodeId = startNodeId;
    }

    public String getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(String endNodeId) {
        this.endNodeId = endNodeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Relationship addProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relationship that = (Relationship) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Relationship{" +
                "id='" + id + '\'' +
                ", startNodeId='" + startNodeId + '\'' +
                ", endNodeId='" + endNodeId + '\'' +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                '}';
    }

    /**
     * Create a builder for Relationship.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String startNodeId;
        private String endNodeId;
        private String type;
        private Map<String, Object> properties = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder startNodeId(String startNodeId) {
            this.startNodeId = startNodeId;
            return this;
        }

        public Builder endNodeId(String endNodeId) {
            this.endNodeId = endNodeId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Relationship build() {
            Relationship rel = new Relationship(startNodeId, endNodeId, type);
            rel.id = this.id;
            rel.properties = this.properties;
            return rel;
        }
    }
}
