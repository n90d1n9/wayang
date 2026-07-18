package tech.kayys.wayang.graph.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import tech.kayys.wayang.graph.GraphStore;
import tech.kayys.wayang.graph.inmemory.InMemoryGraphStore;
import tech.kayys.wayang.graph.neo4j.Neo4jConfig;
import tech.kayys.wayang.graph.neo4j.Neo4jGraphStore;

/**
 * CDI factory for {@link GraphStore} instances.
 * Reads the {@code wayang.graph.store.type} configuration property
 * and produces the appropriate implementation.
 */
@ApplicationScoped
public class GraphStoreProvider {

    @ConfigProperty(name = "wayang.graph.store.type", defaultValue = "inmemory")
    String storeType;

    @ConfigProperty(name = "wayang.graph.store.neo4j.uri", defaultValue = "bolt://localhost:7687")
    String neo4jUri;

    @ConfigProperty(name = "wayang.graph.store.neo4j.username", defaultValue = "neo4j")
    String neo4jUsername;

    @ConfigProperty(name = "wayang.graph.store.neo4j.password", defaultValue = "password")
    String neo4jPassword;

    @ConfigProperty(name = "wayang.graph.store.neo4j.database", defaultValue = "neo4j")
    String neo4jDatabase;

    private volatile GraphStore graphStore;

    @Produces
    @ApplicationScoped
    public GraphStore getGraphStore() {
        if (graphStore == null) {
            synchronized (this) {
                if (graphStore == null) {
                    graphStore = createGraphStore(storeType);
                    graphStore.initialize();
                }
            }
        }
        return graphStore;
    }

    private GraphStore createGraphStore(String type) {
        return switch (type.toLowerCase()) {
            case "inmemory", "in-memory" -> new InMemoryGraphStore();
            case "neo4j" -> new Neo4jGraphStore(
                    new Neo4jConfig(neo4jUri, neo4jUsername, neo4jPassword, neo4jDatabase));
            default -> throw new IllegalArgumentException("Unknown graph store type: " + type
                    + ". Supported: inmemory, neo4j");
        };
    }
}
