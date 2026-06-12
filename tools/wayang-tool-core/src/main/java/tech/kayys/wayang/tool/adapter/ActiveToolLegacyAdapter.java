package tech.kayys.wayang.tool.adapter;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.Map;

/**
 * Adapts the active tool SPI back to the deprecated map-based tool SPI.
 */
@Deprecated(since = "2026-05-26", forRemoval = false)
public class ActiveToolLegacyAdapter implements tech.kayys.wayang.tool.spi.Tool {

    private final Tool activeTool;

    public ActiveToolLegacyAdapter(Tool activeTool) {
        this.activeTool = activeTool;
    }

    @Override
    public String id() {
        return activeTool.id();
    }

    @Override
    public String name() {
        return activeTool.name();
    }

    @Override
    public String description() {
        return activeTool.description();
    }

    @Override
    public Map<String, Object> inputSchema() {
        return activeTool.inputSchema();
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> arguments, Map<String, Object> context) {
        ToolContext toolContext = new ToolContext(
                activeTool.id(),
                arguments,
                null,
                System.getenv(),
                java.time.Duration.ofSeconds(30),
                false,
                context != null ? context : Map.of());

        return activeTool.executeAsync(arguments, toolContext)
                .map(ToolResult::toMap);
    }
}
