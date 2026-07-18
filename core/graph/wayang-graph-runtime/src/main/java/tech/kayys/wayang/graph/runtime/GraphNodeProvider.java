package tech.kayys.wayang.graph.runtime;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Registers the Graph Executor capabilities as workflow node definitions,
 * following the same SPI pattern as the VectorNodeProvider.
 */
public class GraphNodeProvider implements NodeProvider {

    public static final String GRAPH_QUERY = "graph-query-node";
    public static final String GRAPH_UPSERT = "graph-upsert-node";

    private static final String GRAPH_QUERY_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "storeType": { "type": "string", "enum": ["inmemory", "neo4j"], "default": "inmemory" },
                "queryType": { "type": "string", "enum": ["label", "property", "cypher", "path", "node", "relationships"], "default": "label" },
                "label": { "type": "string" },
                "propertyKey": { "type": "string" },
                "propertyValue": {},
                "cypher": { "type": "string" },
                "parameters": { "type": "object", "additionalProperties": true },
                "startNodeId": { "type": "string" },
                "endNodeId": { "type": "string" },
                "nodeId": { "type": "string" },
                "direction": { "type": "string", "enum": ["INCOMING", "OUTGOING", "BOTH"], "default": "BOTH" },
                "maxDepth": { "type": "integer", "minimum": 1, "default": 3 }
              },
              "additionalProperties": true
            }
            """;

    private static final String GRAPH_UPSERT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "storeType": { "type": "string", "enum": ["inmemory", "neo4j"], "default": "inmemory" },
                "node": {
                  "type": "object",
                  "properties": {
                    "id": { "type": "string" },
                    "label": { "type": "string" },
                    "properties": { "type": "object", "additionalProperties": true },
                    "metadata": { "type": "object", "additionalProperties": true }
                  }
                },
                "nodes": {
                  "type": "array",
                  "items": { "$ref": "#/properties/node" }
                },
                "relationship": {
                  "type": "object",
                  "properties": {
                    "startNodeId": { "type": "string" },
                    "endNodeId": { "type": "string" },
                    "type": { "type": "string" },
                    "properties": { "type": "object", "additionalProperties": true }
                  }
                },
                "relationships": {
                  "type": "array",
                  "items": { "$ref": "#/properties/relationship" }
                }
              },
              "additionalProperties": true
            }
            """;

    @Override
    public String id() {
        return "wayang-graph-runtime";
    }

    @Override
    public String name() {
        return "Graph Runtime Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Provides Graph Query and Graph Upsert node capabilities for knowledge graphs.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
            new NodeDefinition(
                GRAPH_QUERY,
                "Graph Query",
                "Data",
                "Graph Database",
                "Queries a graph database for nodes, relationships, or paths.",
                "share-2",    // Graph icon
                "#8B5CF6",    // Violet color
                GRAPH_QUERY_SCHEMA,
                "{}",
                "{}",
                Map.of(
                    "storeType", "inmemory",
                    "queryType", "label",
                    "maxDepth", 3
                )
            ),
            new NodeDefinition(
                GRAPH_UPSERT,
                "Graph Upsert",
                "Data",
                "Graph Database",
                "Creates or updates nodes and relationships in a graph database.",
                "share-2",
                "#8B5CF6",
                GRAPH_UPSERT_SCHEMA,
                "{}",
                "{}",
                Map.of(
                    "storeType", "inmemory"
                )
            )
        );
    }
}
