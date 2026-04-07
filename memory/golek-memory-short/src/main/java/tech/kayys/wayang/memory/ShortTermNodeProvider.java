package tech.kayys.wayang.memory;

import tech.kayys.wayang.memory.node.MemoryNodeTypes;
import tech.kayys.wayang.memory.node.MemorySchemas;
import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Exposes the Short Term Memory Executor capability as a Wayang node.
 */
public class ShortTermNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "tech.kayys.wayang.memory.shortterm";
    }

    @Override
    public String name() {
        return "Short Term Memory Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Temporary context storage for the current session.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
                // Short Term Memory Node
                new NodeDefinition(
                        MemoryNodeTypes.SHORT_TERM,
                        "Short Term Memory",
                        "Memory",
                        "Context",
                        "Temporary context storage for the immediate workflow or session.",
                        "database", // Icon
                        "#8B5CF6",
                        MemorySchemas.MEMORY_CONFIG,
                        "{}",
                        "{}",
                        Map.of(
                                "operation", "RETRIEVE",
                                "maxCapacity", 10,
                                "limit", 10,
                                "minSimilarity", 0.0)));
    }
}
