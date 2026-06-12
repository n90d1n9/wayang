package tech.kayys.wayang.tool.utcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.Collections;
import java.util.Map;

/**
 * UTCP Tool implementation.
 */
public class UtcpTool implements Tool {

    private final String id;
    private final String name;

    public UtcpTool(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "Universal Tool Call Protocol Tool";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Collections.emptyMap();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        // TODO: Implement UTCP call
        return ToolResult.success(Map.of("status", "executed", "tool", "utcp"));
    }

    @Override
    public Uni<ToolResult> executeAsync(Map<String, Object> arguments, ToolContext context) {
        return Uni.createFrom().item(() -> execute(arguments, context));
    }
}
