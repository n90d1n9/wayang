package tech.kayys.wayang.tool.adapter;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.Map;

/**
 * Adapts the deprecated map-based Wayang tool SPI to the active tool SPI.
 */
public class LegacyWayangToolAdapter implements Tool {

    private final tech.kayys.wayang.tool.spi.Tool legacyTool;

    public LegacyWayangToolAdapter(tech.kayys.wayang.tool.spi.Tool legacyTool) {
        this.legacyTool = legacyTool;
    }

    @Override
    public String id() {
        return legacyTool.id();
    }

    @Override
    public String name() {
        return legacyTool.name();
    }

    @Override
    public String description() {
        return legacyTool.description();
    }

    @Override
    public Map<String, Object> inputSchema() {
        return legacyTool.inputSchema();
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
            Map<String, Object> result = legacyTool.execute(params, context.asMap())
                    .await().atMost(context.timeout());
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.error(e.getMessage());
        }
    }

    @Override
    public Uni<ToolResult> executeAsync(Map<String, Object> params, ToolContext context) {
        return legacyTool.execute(params, context.asMap())
                .map(ToolResult::success)
                .onFailure().recoverWithItem(e -> ToolResult.error(e.getMessage()));
    }
}
