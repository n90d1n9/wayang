package tech.kayys.wayang.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a query against the graph store.
 * Supports filtering by label, properties, relationship types, and traversal depth.
 */
public class GraphQuery {

    private String label;
    private Map<String, Object> propertyFilters;
    private String relationshipType;
    private GraphStore.Direction direction;
    private int maxDepth;
    private int limit;

    public GraphQuery() {
        this.propertyFilters = new HashMap<>();
        this.direction = GraphStore.Direction.BOTH;
        this.maxDepth = 3;
        this.limit = 100;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Map<String, Object> getPropertyFilters() { return propertyFilters; }
    public void setPropertyFilters(Map<String, Object> propertyFilters) { this.propertyFilters = propertyFilters; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public GraphStore.Direction getDirection() { return direction; }
    public void setDirection(GraphStore.Direction direction) { this.direction = direction; }

    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final GraphQuery query = new GraphQuery();

        public Builder label(String label) { query.label = label; return this; }
        public Builder filter(String key, Object value) { query.propertyFilters.put(key, value); return this; }
        public Builder filters(Map<String, Object> filters) { query.propertyFilters.putAll(filters); return this; }
        public Builder relationshipType(String type) { query.relationshipType = type; return this; }
        public Builder direction(GraphStore.Direction dir) { query.direction = dir; return this; }
        public Builder maxDepth(int depth) { query.maxDepth = depth; return this; }
        public Builder limit(int limit) { query.limit = limit; return this; }

        public GraphQuery build() { return query; }
    }
}
