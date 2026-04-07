package tech.kayys.wayang.memory;

import tech.kayys.wayang.memory.node.MemoryNodeTypes;
import tech.kayys.wayang.memory.node.MemorySchemas;
import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Exposes the Episodic Memory Executor capability as a Wayang node.
 */
public class EpisodicNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "tech.kayys.wayang.memory.episodic";
    }

    @Override
    public String name() {
        return "Episodic Memory Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Stores and retrieves event-based episodic memories.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
                // Episodic Memory Node
                new NodeDefinition(
                        MemoryNodeTypes.EPISODIC,
                        "Episodic Memory",
                        "Memory",
                        "Episodic",
                        "Stores and retrieves event-based experiences and timelines.",
                        "database", // Icon
                        "#8B5CF6", // Purple color
                        MemorySchemas.MEMORY_CONFIG,
                        "{}", // Input schema (e.g. requires query or content)
                        "{}", // Output schema
                        Map.of(
                                "operation", "RETRIEVE",
                                "eventType", "general",
                                "limit", 10,
                                "minSimilarity", 0.0)));
    }
}
