package tech.kayys.wayang.tool.node;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Implementation of NodeProvider for common tool nodes.
 */
public class ToolNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "tech.kayys.wayang.tool.core";
    }

    @Override
    public String name() {
        return "Wayang Tool Core";
    }

    @Override
    public String version() {
        return "1.0.0-SNAPSHOT";
    }

    @Override
    public String description() {
        return "Provides core tool nodes like HTTP, Sandbox, MCP, UTCP, and REST.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
                // HTTP Tool Node
                new NodeDefinition(
                        ToolNodeTypes.TOOL_HTTP,
                        "HTTP Tool",
                        "Tool",
                        "Communication",
                        "Executes generic HTTP requests (GET, POST, etc.) to any URL.",
                        "globe", // Icon
                        "#3B82F6", // Blue
                        ToolSchemas.TOOL_HTTP_CONFIG,
                        "{}", // Input schema (not used for tool nodes config)
                        "{}", // Output schema
                        Map.of(
                                "method", "GET",
                                "timeout", 30
                        )),

                // Sandbox Tool Node
                new NodeDefinition(
                        ToolNodeTypes.TOOL_SANDBOX,
                        "Code Sandbox",
                        "Tool",
                        "Execution",
                        "Executes code (Python, JavaScript, Bash) in a secure sandbox.",
                        "code", // Icon
                        "#10B981", // Green
                        ToolSchemas.TOOL_SANDBOX_CONFIG,
                        "{}", 
                        "{}",
                        Map.of(
                                "language", "python",
                                "timeout", 60
                        )),

                // MCP Tool Node
                new NodeDefinition(
                        ToolNodeTypes.TOOL_MCP,
                        "MCP Tool",
                        "Tool",
                        "Protocol",
                        "Invokes a tool via the Model Context Protocol (MCP).",
                        "zap", // Icon
                        "#F59E0B", // Orange
                        ToolSchemas.TOOL_INVOCATION_CONFIG,
                        "{}", 
                        "{}",
                        Map.of()),

                // UTCP Tool Node
                new NodeDefinition(
                        ToolNodeTypes.TOOL_UTCP,
                        "UTCP Tool",
                        "Tool",
                        "Protocol",
                        "Invokes a tool via the Unified Tool Communication Protocol (UTCP).",
                        "share-2", // Icon
                        "#EC4899", // Pink
                        ToolSchemas.TOOL_INVOCATION_CONFIG,
                        "{}", 
                        "{}",
                        Map.of()),

                // REST Tool Node
                new NodeDefinition(
                        ToolNodeTypes.TOOL_REST,
                        "REST Tool",
                        "Tool",
                        "Communication",
                        "Executes RESTful API calls with structured JSON support.",
                        "database", // Icon
                        "#8B5CF6", // Purple
                        ToolSchemas.TOOL_REST_CONFIG,
                        "{}", 
                        "{}",
                        Map.of(
                                "method", "GET",
                                "timeout", 30
                        ))
        );
    }
}
