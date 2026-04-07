# Graph Executor Implementation Guide

## Overview

The Graph Executor provides graph-based storage as an alternative to vector storage for AI agent data, enabling:
- Knowledge graph storage and querying
- Relationship-based reasoning
- Structured data representation
- Path finding and graph traversal
- Integration with Neo4j and in-memory storage

## Module Structure

```
wayang/executors/graph/
├── pom.xml                              # Parent POM
├── wayang-graph-core/                   # Core interfaces and models
│   ├── pom.xml
│   └── src/main/java/tech/kayys/wayang/graph/
│       ├── GraphStore.java              # Core interface
│       ├── AbstractGraphStore.java      # Abstract base class
│       ├── Node.java                    # Node model
│       ├── Relationship.java            # Relationship model
│       └── GraphStatistics.java         # Statistics model
├── wayang-graph-inmemory/               # In-memory implementation
│   ├── pom.xml
│   └── src/main/java/tech/kayys/wayang/graph/inmemory/
│       └── InMemoryGraphStore.java      # In-memory store
├── wayang-graph-neo4j/                  # Neo4j implementation
│   ├── pom.xml
│   └── src/main/java/tech/kayys/wayang/graph/neo4j/
│       └── Neo4jGraphStore.java         # Neo4j store
└── wayang-graph-runtime/                # Runtime executors
    ├── pom.xml
    └── src/main/java/tech/kayys/wayang/graph/runtime/
        ├── GraphNodeProvider.java       # CDI provider
        ├── GraphQueryExecutor.java      # Query executor
        └── GraphUpsertExecutor.java     # Upsert executor
```

## Usage Examples

### Basic Graph Operations

```java
@Inject
GraphStore graphStore;

// Initialize
graphStore.initialize();

// Add nodes
Node person = Node.builder()
    .label("Person")
    .property("name", "John")
    .property("age", 30)
    .build();

String nodeId = graphStore.addNode(person);

// Add relationship
Relationship knows = Relationship.builder()
    .startNodeId(nodeId)
    .endNodeId(otherNodeId)
    .type("KNOWS")
    .property("since", "2020")
    .build();

graphStore.addRelationship(knows);

// Query nodes
List<Node> people = graphStore.findNodesByLabel("Person");

// Execute Cypher (Neo4j)
List<Map<String, Object>> results = graphStore.executeCypher(
    "MATCH (p:Person) WHERE p.age > $age RETURN p",
    Map.of("age", 25)
);
```

### Agent Knowledge Graph

```java
// Store agent knowledge as graph
Node concept = Node.builder()
    .label("Concept")
    .property("name", "Machine Learning")
    .property("description", "AI subset...")
    .build();

Node related = Node.builder()
    .label("Concept")
    .property("name", "Neural Networks")
    .build();

Relationship relates = Relationship.builder()
    .startNodeId(concept.getId())
    .endNodeId(related.getId())
    .type("RELATED_TO")
    .property("strength", 0.9)
    .build();

graphStore.addNode(concept);
graphStore.addNode(related);
graphStore.addRelationship(relates);

// Find related concepts
List<Relationship> relationships = graphStore.getRelationships(
    concept.getId(), 
    GraphStore.Direction.OUTGOING
);
```

## Configuration

```properties
# In-Memory Graph Store
wayang.graph.store.type=inmemory
wayang.graph.store.inmemory.enabled=true

# Neo4j Graph Store
wayang.graph.store.type=neo4j
wayang.graph.store.neo4j.uri=bolt://localhost:7687
wayang.graph.store.neo4j.username=neo4j
wayang.graph.store.neo4j.password=password
wayang.graph.store.neo4j.database=neo4j
```

## API Reference

### GraphStore Interface

| Method | Description |
|--------|-------------|
| `addNode(Node)` | Add a node to the graph |
| `addNodes(List<Node>)` | Add multiple nodes |
| `getNode(String)` | Get node by ID |
| `findNodesByLabel(String)` | Find nodes by label |
| `findNodesByProperty(...)` | Find nodes by property |
| `updateNode(String, Node)` | Update a node |
| `deleteNode(String)` | Delete a node |
| `addRelationship(Relationship)` | Add relationship |
| `getRelationships(String, Direction)` | Get node relationships |
| `executeCypher(String, Map)` | Execute Cypher query |
| `findPaths(String, String, int)` | Find paths between nodes |
| `getStatistics()` | Get graph statistics |

### Node Model

```java
Node node = Node.builder()
    .id("custom-id")           // Optional, auto-generated if not provided
    .label("Person")            // Node label/type
    .property("name", "John")   // Add properties
    .property("age", 30)
    .metadata("source", "import") // Add metadata
    .build();
```

### Relationship Model

```java
Relationship rel = Relationship.builder()
    .startNodeId("node-1")      // Start node ID
    .endNodeId("node-2")        // End node ID
    .type("KNOWS")              // Relationship type
    .property("since", 2020)    // Add properties
    .build();
```

## Integration with Wayang Agents

The graph executor integrates with Wayang agents for:
- **Memory Storage**: Store agent memories as graph nodes
- **Knowledge Representation**: Represent concepts and relationships
- **Reasoning**: Use graph traversal for inference
- **Context Management**: Maintain conversation context as graph

## Performance Considerations

| Operation | In-Memory | Neo4j |
|-----------|-----------|-------|
| Node Insert | O(1) | O(log n) |
| Relationship Insert | O(1) | O(log n) |
| Node Lookup | O(1) | O(log n) |
| Path Finding | O(V+E) | O(V+E) |
| Cypher Query | N/A | Optimized |

## Best Practices

1. **Use labels consistently** for node types
2. **Index frequently queried properties**
3. **Use relationships** to represent connections
4. **Batch operations** for better performance
5. **Close GraphStore** when done to release resources

## Resources

- [Neo4j Documentation](https://neo4j.com/docs/)
- [Cypher Query Language](https://neo4j.com/docs/cypher-manual/)
- [Graph Theory Basics](https://en.wikipedia.org/wiki/Graph_theory)
