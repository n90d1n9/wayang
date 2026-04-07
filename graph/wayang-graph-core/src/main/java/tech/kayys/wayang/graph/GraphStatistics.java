package tech.kayys.wayang.graph;

/**
 * Graph statistics.
 */
public class GraphStatistics {

    private long nodeCount;
    private long relationshipCount;
    private long labelCount;
    private long relationshipTypeCount;

    public GraphStatistics() {
    }

    public GraphStatistics(long nodeCount, long relationshipCount, long labelCount, long relationshipTypeCount) {
        this.nodeCount = nodeCount;
        this.relationshipCount = relationshipCount;
        this.labelCount = labelCount;
        this.relationshipTypeCount = relationshipTypeCount;
    }

    public long getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(long nodeCount) {
        this.nodeCount = nodeCount;
    }

    public long getRelationshipCount() {
        return relationshipCount;
    }

    public void setRelationshipCount(long relationshipCount) {
        this.relationshipCount = relationshipCount;
    }

    public long getLabelCount() {
        return labelCount;
    }

    public void setLabelCount(long labelCount) {
        this.labelCount = labelCount;
    }

    public long getRelationshipTypeCount() {
        return relationshipTypeCount;
    }

    public void setRelationshipTypeCount(long relationshipTypeCount) {
        this.relationshipTypeCount = relationshipTypeCount;
    }

    @Override
    public String toString() {
        return "GraphStatistics{" +
                "nodeCount=" + nodeCount +
                ", relationshipCount=" + relationshipCount +
                ", labelCount=" + labelCount +
                ", relationshipTypeCount=" + relationshipTypeCount +
                '}';
    }
}
