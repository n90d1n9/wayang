package tech.kayys.wayang.agent.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.Map;

/**
 * Think tool — allows the agent to reason internally before taking action.
 */
@ApplicationScoped
public class ThinkTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(ThinkTool.class);

    @Override
    public String id() { return "think"; }

    @Override
    public String name() { return "think"; }

    @Override
    public String description() {
        return """
                Use this tool to think step-by-step before taking action.
                Useful for: planning your approach, analyzing requirements,
                deciding which tool to use next, or reflecting on an error.
                The output is visible but takes no real action.
                """;
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "thought", Map.of("type", "string", "description", "Your internal reasoning or plan")),
                "required", java.util.List.of("thought"));
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String thought = String.valueOf(params.getOrDefault("thought", ""));
        log.debug("[think] {}", thought);
        return ToolResult.success("Thought recorded.");
    }
}
