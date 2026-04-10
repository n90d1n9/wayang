package tech.kayys.wayang.tools.spi;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.spi.Tool as WayangTool;

import java.util.Map;

/**
 * Adapter to convert Gollek Tool to Wayang Tool.
 *
 * <p>
 * Allows Gollek tools to be used with legacy Wayang ToolRegistry.
 * </p>
 *
 * @author golok Team
 * @version 1.0.0
 */
public class GollekToolAdapter implements WayangTool {

    private final Tool gollekTool;

    /**
     * Create adapter for a Gollek tool.
     *
     * @param gollekTool the Gollek tool to adapt
     */
    public GollekToolAdapter(Tool gollekTool) {
        this.gollekTool = gollekTool;
    }

    @Override
    public String id() {
        return gollekTool.id();
    }

    @Override
    public String name() {
        return gollekTool.name();
    }

    @Override
    public String description() {
        return gollekTool.description();
    }

    @Override
    public Map<String, Object> inputSchema() {
        return gollekTool.inputSchema();
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> arguments,
                                             Map<String, Object> context) {
        // Convert context
        ToolContext toolContext = convertToToolContext(arguments, context);

        // Execute gollek tool
        ToolResult result = gollekTool.execute(arguments, toolContext);

        // Convert result to Map
        return Uni.createFrom().item(result.toMap());
    }

    /**
     * Convert Map-based context to ToolContext.
     */
    private ToolContext convertToToolContext(Map<String, Object> arguments,
                                              Map<String, Object> context) {
        return new ToolContext(
            gollekTool.id(),
            arguments,
            null,  // working directory
            System.getenv(),
            null,  // timeout
            false, // dry run
            context != null ? context : Map.of()
        );
    }
}
