package tech.kayys.wayang.embedding.node;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Implementation of NodeProvider for embedding nodes.
 */
public class EmbeddingNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "tech.kayys.wayang.embedding";
    }

    @Override
    public String name() {
        return "Embedding Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Generates vector embeddings for input text.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
                new NodeDefinition(
                        EmbeddingNodeTypes.EMBEDDING_GENERATE,
                        "Generate Embedding",
                        "AI",
                        "Transformation",
                        "Generates vector embeddings for input text using configured model and provider.",
                        "layers", // Icon
                        "#8B5CF6", // Purple
                        EmbeddingSchemas.EMBEDDING_GENERATE_CONFIG,
                        "{}", // Input schema (managed by task)
                        "{}", // Output schema
                        Map.of(
                                "normalize", true)));
    }
}
