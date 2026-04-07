package tech.kayys.wayang.memory.node;

/**
 * Common JSON schemas for memory nodes.
 */
public final class MemorySchemas {

    public static final String MEMORY_CONFIG = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "operation": {
              "type": "string",
              "enum": ["STORE", "RETRIEVE", "SEARCH", "DELETE", "CLEAR"],
              "description": "The memory operation to perform."
            },
            "namespace": {
              "type": "string",
              "description": "Optional namespace to isolate memory entries."
            },
            "content": {
              "type": "string",
              "description": "Content to store (required for STORE)."
            },
            "query": {
              "type": "string",
              "description": "Query string (required for SEARCH/RETRIEVE)."
            },
            "minConfidence": {
              "type": "number",
              "minimum": 0,
              "maximum": 1,
              "default": 0.7,
              "description": "Minimum confidence score for retrieval."
            },
            "limit": {
              "type": "integer",
              "minimum": 1,
              "default": 10,
              "description": "Maximum number of results to return."
            },
            "minSimilarity": {
              "type": "number",
              "minimum": 0,
              "maximum": 1,
              "default": 0.0,
              "description": "Minimum similarity score for vector search."
            },
            "metadata": {
              "type": "object",
              "description": "Additional metadata for the memory entry."
            }
          },
          "required": ["operation"]
        }
        """;

    private MemorySchemas() {}
}
