package tech.kayys.wayang.memory;

import tech.kayys.wayang.memory.node.MemoryNodeTypes;
import tech.kayys.wayang.memory.node.MemorySchemas;
import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Exposes the Working Memory Executor capability as a Wayang node.
 */
public class WorkingNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "tech.kayys.wayang.memory.working";
    }

    @Override
    public String name() {
        return "Working Memory Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Short-lived scratchpad for the current reasoning chain.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
                // Working Memory Node
                new NodeDefinition(
                        MemoryNodeTypes.WORKING,
                        "Working Memory",
                        "Memory",
                        "Active",
                        "Short-lived scratchpad for the current reasoning chain.",
                        "database", // Icon
                        "#8B5CF6",
                        MemorySchemas.MEMORY_CONFIG,
                        "{}",
                        "{}",
                        Map.of(
                                "operation", "RETRIEVE",
                                "capacity", 5,
                                "limit", 5,
                                "minSimilarity", 0.0)));
    }
}
