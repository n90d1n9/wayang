package tech.kayys.wayang.embedding.node;

/**
 * Common JSON schemas for embedding nodes.
 */
public final class EmbeddingSchemas {

    public static final String EMBEDDING_GENERATE_CONFIG = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "model": {
                  "type": "string",
                  "description": "The embedding model name (e.g., text-embedding-3-small)."
                },
                "provider": {
                  "type": "string",
                  "description": "The embedding provider name."
                },
                "normalize": {
                  "type": "boolean",
                  "default": true,
                  "description": "Whether to L2-normalize the resulting vectors."
                },
                "dimensions": {
                  "type": "integer",
                  "description": "Optional expected vector dimensions."
                }
              }
            }
            """;

    private EmbeddingSchemas() {
    }
}
