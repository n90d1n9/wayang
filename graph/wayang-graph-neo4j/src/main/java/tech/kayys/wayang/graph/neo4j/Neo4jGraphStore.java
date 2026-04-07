package tech.kayys.wayang.graph.neo4j;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.MapAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.graph.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j implementation of {@link GraphStore} using the official Neo4j Java Driver.
 *
 * <p>All node IDs are stored as a {@code _graphStoreId} property,
 * allowing the driver to work with any Neo4j version (element‑ID or legacy).
 */
public class Neo4jGraphStore extends AbstractGraphStore {

    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphStore.class);
    private static final String ID_PROP = "_graphStoreId";

    private final Neo4jConfig config;
    private Driver driver;

    public Neo4jGraphStore(Neo4jConfig config) {
        this.config = config;
    }

    // ── Lifecycle ───────────────────────────────────────────────────

    @Override
    protected void doInitialize() {
        log.info("Connecting to Neo4j at {}", config.getUri());
        driver = GraphDatabase.driver(
                config.getUri(),
                AuthTokens.basic(config.getUsername(), config.getPassword()));
        driver.verifyConnectivity();
        log.info("Neo4j connection established");

        // Ensure index on the ID property for fast lookups
        try (Session session = session()) {
            session.run("CREATE INDEX IF NOT EXISTS FOR (n:_GraphNode) ON (n." + ID_PROP + ")");
        }
    }

    @Override
    protected void doClose() {
        if (driver != null) {
            driver.close();
            driver = null;
            log.info("Neo4j connection closed");
        }
    }

    private Session session() {
        return driver.session(SessionConfig.forDatabase(config.getDatabase()));
    }

    // ── Node CRUD ───────────────────────────────────────────────────

    @Override
    public String addNode(Node node) {
        ensureInitialized();
        if (node.getId() == null) {
            node.setId(UUID.randomUUID().toString());
        }
        String label = node.getLabel() != null ? node.getLabel() : "_GraphNode";
        Map<String, Object> props = new HashMap<>(node.getProperties());
        props.put(ID_PROP, node.getId());
        if (!node.getMetadata().isEmpty()) {
            props.put("_metadata", node.getMetadata().toString());
        }

        String cypher = "CREATE (n:" + sanitize(label) + " $props) RETURN n." + ID_PROP + " AS id";
        try (Session session = session()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of("props", props));
                return result.single().get("id").asString();
            });
        }
    }

    @Override
    public Optional<Node> getNode(String nodeId) {
        ensureInitialized();
        String cypher = "MATCH (n {" + ID_PROP + ": $id}) RETURN n, labels(n) AS labels";
        try (Session session = session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("id", nodeId));
                if (!result.hasNext()) return Optional.<Node>empty();
                Record rec = result.single();
                return Optional.of(recordToNode(rec));
            });
        }
    }

    @Override
    public List<Node> findNodesByLabel(String label) {
        ensureInitialized();
        String cypher = "MATCH (n:" + sanitize(label) + ") RETURN n, labels(n) AS labels";
        try (Session session = session()) {
            return session.executeRead(tx ->
                    tx.run(cypher).list(this::recordToNode));
        }
    }

    @Override
    public List<Node> findNodesByProperty(String label, String property, Object value) {
        ensureInitialized();
        String cypher = "MATCH (n:" + sanitize(label) + " {" + sanitize(property) + ": $val}) RETURN n, labels(n) AS labels";
        try (Session session = session()) {
            return session.executeRead(tx ->
                    tx.run(cypher, Map.of("val", value)).list(this::recordToNode));
        }
    }

    @Override
    public boolean updateNode(String nodeId, Node node) {
        ensureInitialized();
        Map<String, Object> props = new HashMap<>(node.getProperties());
        props.put(ID_PROP, nodeId);

        String cypher = "MATCH (n {" + ID_PROP + ": $id}) SET n += $props RETURN count(n) AS cnt";
        try (Session session = session()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of("id", nodeId, "props", props));
                return result.single().get("cnt").asLong() > 0;
            });
        }
    }

    @Override
    public boolean deleteNode(String nodeId) {
        ensureInitialized();
        String cypher = "MATCH (n {" + ID_PROP + ": $id}) DETACH DELETE n RETURN count(n) AS cnt";
        try (Session session = session()) {
            return session.executeWrite(tx -> {
                // Count before delete
                Result check = tx.run("MATCH (n {" + ID_PROP + ": $id}) RETURN count(n) AS cnt",
                        Map.of("id", nodeId));
                long exists = check.single().get("cnt").asLong();
                if (exists == 0) return false;
                tx.run(cypher, Map.of("id", nodeId));
                return true;
            });
        }
    }

    // ── Relationship CRUD ───────────────────────────────────────────

    @Override
    public String addRelationship(Relationship relationship) {
        ensureInitialized();
        if (relationship.getId() == null) {
            relationship.setId(UUID.randomUUID().toString());
        }
        String type = sanitize(relationship.getType() != null ? relationship.getType() : "RELATED_TO");
        Map<String, Object> props = new HashMap<>(relationship.getProperties());
        props.put(ID_PROP, relationship.getId());

        String cypher = "MATCH (a {" + ID_PROP + ": $startId}), (b {" + ID_PROP + ": $endId}) " +
                         "CREATE (a)-[r:" + type + " $props]->(b) RETURN r." + ID_PROP + " AS id";
        try (Session session = session()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, Map.of(
                        "startId", relationship.getStartNodeId(),
                        "endId", relationship.getEndNodeId(),
                        "props", props));
                return result.single().get("id").asString();
            });
        }
    }

    @Override
    public List<Relationship> getRelationships(String nodeId, Direction direction) {
        ensureInitialized();
        String pattern = switch (direction) {
            case OUTGOING -> "(a {" + ID_PROP + ": $id})-[r]->(b)";
            case INCOMING -> "(a)<-[r]-(b {" + ID_PROP + ": $id})";
            case BOTH     -> "(a {" + ID_PROP + ": $id})-[r]-(b)";
        };
        String cypher = "MATCH " + pattern + " RETURN r, type(r) AS relType, " +
                         "a." + ID_PROP + " AS startId, b." + ID_PROP + " AS endId";
        try (Session session = session()) {
            return session.executeRead(tx ->
                    tx.run(cypher, Map.of("id", nodeId)).list(this::recordToRelationship));
        }
    }

    @Override
    public List<Relationship> findRelationshipsByType(String type) {
        ensureInitialized();
        String cypher = "MATCH (a)-[r:" + sanitize(type) + "]->(b) RETURN r, type(r) AS relType, " +
                         "a." + ID_PROP + " AS startId, b." + ID_PROP + " AS endId";
        try (Session session = session()) {
            return session.executeRead(tx ->
                    tx.run(cypher).list(this::recordToRelationship));
        }
    }

    @Override
    public boolean deleteRelationship(String relationshipId) {
        ensureInitialized();
        String cypher = "MATCH ()-[r {" + ID_PROP + ": $id}]-() DELETE r RETURN count(r) AS cnt";
        try (Session session = session()) {
            return session.executeWrite(tx -> {
                Result check = tx.run("MATCH ()-[r {" + ID_PROP + ": $id}]-() RETURN count(r) AS cnt",
                        Map.of("id", relationshipId));
                long exists = check.single().get("cnt").asLong();
                if (exists == 0) return false;
                tx.run(cypher, Map.of("id", relationshipId));
                return true;
            });
        }
    }

    // ── Cypher & Path Finding ───────────────────────────────────────

    @Override
    public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
        ensureInitialized();
        try (Session session = session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(query, parameters != null ? parameters : Map.of());
                return result.list(record -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String key : record.keys()) {
                        Value val = record.get(key);
                        try {
                            row.put(key, val.asNode().asMap());
                        } catch (Exception e1) {
                            try {
                                row.put(key, val.asRelationship().asMap());
                            } catch (Exception e2) {
                                row.put(key, val.asObject());
                            }
                        }
                    }
                    return row;
                });
            });
        }
    }

    @Override
    public List<List<Node>> findPaths(String startNodeId, String endNodeId, int maxDepth) {
        ensureInitialized();
        String cypher = "MATCH path = shortestPath((a {" + ID_PROP + ": $startId})-[*1.." + maxDepth +
                         "]-(b {" + ID_PROP + ": $endId})) " +
                         "RETURN [n IN nodes(path) | n] AS pathNodes";
        try (Session session = session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, Map.of("startId", startNodeId, "endId", endNodeId));
                List<List<Node>> paths = new ArrayList<>();
                while (result.hasNext()) {
                    Record rec = result.next();
                    List<Object> nodeList = rec.get("pathNodes").asList();
                    List<Node> path = nodeList.stream()
                            .filter(o -> o instanceof MapAccessor)
                            .map(o -> {
                                MapAccessor ma = (MapAccessor) o;
                                Map<String, Object> map = ma.asMap();
                                String id = map.getOrDefault(ID_PROP, UUID.randomUUID().toString()).toString();
                                Node node = new Node(id, "");
                                map.forEach((k, v) -> {
                                    if (!k.startsWith("_")) node.addProperty(k, v);
                                });
                                return node;
                            })
                            .collect(Collectors.toList());
                    paths.add(path);
                }
                return paths;
            });
        }
    }

    // ── Statistics ───────────────────────────────────────────────────

    @Override
    public GraphStatistics getStatistics() {
        ensureInitialized();
        try (Session session = session()) {
            return session.executeRead(tx -> {
                long nodeCount = tx.run("MATCH (n) RETURN count(n) AS cnt")
                        .single().get("cnt").asLong();
                long relCount = tx.run("MATCH ()-[r]->() RETURN count(r) AS cnt")
                        .single().get("cnt").asLong();
                long labelCount = tx.run("CALL db.labels() YIELD label RETURN count(label) AS cnt")
                        .single().get("cnt").asLong();
                long typeCount = tx.run("CALL db.relationshipTypes() YIELD relationshipType RETURN count(relationshipType) AS cnt")
                        .single().get("cnt").asLong();
                return new GraphStatistics(nodeCount, relCount, labelCount, typeCount);
            });
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Node recordToNode(Record record) {
        org.neo4j.driver.types.Node neo4jNode = record.get("n").asNode();
        Map<String, Object> props = new HashMap<>(neo4jNode.asMap());
        String id = props.getOrDefault(ID_PROP, neo4jNode.elementId()).toString();
        props.remove(ID_PROP);
        props.remove("_metadata");

        List<String> labels = record.containsKey("labels")
                ? record.get("labels").asList(Value::asString)
                : List.of();
        String label = labels.stream()
                .filter(l -> !l.startsWith("_"))
                .findFirst().orElse("");

        Node node = new Node(id, label);
        node.setProperties(props);
        return node;
    }

    private Relationship recordToRelationship(Record record) {
        org.neo4j.driver.types.Relationship neo4jRel = record.get("r").asRelationship();
        Map<String, Object> props = new HashMap<>(neo4jRel.asMap());
        String id = props.getOrDefault(ID_PROP, neo4jRel.elementId()).toString();
        props.remove(ID_PROP);

        String startId = record.get("startId").asString();
        String endId = record.get("endId").asString();
        String type = record.get("relType").asString();

        Relationship rel = new Relationship(startId, endId, type);
        rel.setId(id);
        rel.setProperties(props);
        return rel;
    }

    /**
     * Sanitize a label or property name to prevent Cypher injection.
     */
    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
