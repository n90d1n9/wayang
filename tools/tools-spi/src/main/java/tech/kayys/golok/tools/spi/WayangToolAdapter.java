package tech.kayys.wayang.tools.spi;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.spi.Tool as WayangTool;

import java.util.Map;

/**
 * Adapter to convert Wayang Tool to Gollek Tool.
 *
 * <p>
 * Allows existing Wayang tools to be used with the Gollek ToolRegistry.
 * </p>
 *
 * @author golok Team
 * @version 1.0.0
 */
public class WayangToolAdapter implements Tool {

    private final WayangTool wayangTool;

    /**
     * Create adapter for a Wayang tool.
     *
     * @param wayangTool the Wayang tool to adapt
     */
    public WayangToolAdapter(WayangTool wayangTool) {
        this.wayangTool = wayangTool;
    }

    @Override
    public String id() {
        return wayangTool.id();
    }

    @Override
    public String name() {
        return wayangTool.name();
    }

    @Override
    public String description() {
        return wayangTool.description();
    }

    @Override
    public Map<String, Object> inputSchema() {
        return wayangTool.inputSchema();
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
            // Execute wayang tool (reactive)
            Map<String, Object> result = wayangTool.execute(params, context.asMap())
                .await().atMost(context.timeout());

            return ToolResult.success(result);

        } catch (Exception e) {
            return ToolResult.error(e.getMessage());
        }
    }

    @Override
    public Uni<ToolResult> executeAsync(Map<String, Object> params, ToolContext context) {
        // Wayang tool is already reactive
        return wayangTool.execute(params, context.asMap())
            .map(ToolResult::success)
            .onFailure().recoverWithItem(e -> ToolResult.error(e.getMessage()));
    }
}
