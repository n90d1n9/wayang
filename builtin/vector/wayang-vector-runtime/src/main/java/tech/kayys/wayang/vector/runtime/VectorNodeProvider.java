package tech.kayys.wayang.vector.runtime;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;
import tech.kayys.wayang.schema.catalog.BuiltinSchemaCatalog;
import tech.kayys.wayang.schema.vector.VectorSearchConfig;
import tech.kayys.wayang.schema.vector.VectorUpsertConfig;

import java.util.List;
import java.util.Map;

/**
 * Exposes the Vector Executor capabilities (Search, Upsert) as Wayang nodes.
 */
public class VectorNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "wayang-vector-runtime";
    }

    @Override
    public String name() {
        return "Vector Runtime Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Provides Vector Search and Upsert node capabilities.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
            // Vector Search Node
            new NodeDefinition(
                BuiltinSchemaCatalog.VECTOR_SEARCH,
                "Vector Search",
                "Data",
                "Vector Database",
                "Searches a Vector Database for similar embeddings.",
                "database", // Icon
                "#10B981",  // Emerald color
                BuiltinSchemaCatalog.get(BuiltinSchemaCatalog.VECTOR_SEARCH),
                "{}", // Input schema (e.g. requires an embedding or query string)
                "{}", // Output schema (returns list of docs + scores)
                Map.of(
                    "storeType", "in-memory",
                    "topK", 10,
                    "minScore", 0.0
                )
            ),
            
            // Vector Upsert Node
            new NodeDefinition(
                BuiltinSchemaCatalog.VECTOR_UPSERT,
                "Vector Upsert",
                "Data",
                "Vector Database",
                "Inserts or updates documents in a Vector Database.",
                "database", // Icon
                "#10B981",
                BuiltinSchemaCatalog.get(BuiltinSchemaCatalog.VECTOR_UPSERT),
                "{}", // Input schema (requires list of documents with embeddings)
                "{}", // Output schema (status)
                Map.of(
                    "storeType", "in-memory"
                )
            )
        );
    }
}
