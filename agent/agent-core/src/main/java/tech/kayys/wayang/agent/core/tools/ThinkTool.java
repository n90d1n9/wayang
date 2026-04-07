package tech.kayys.wayang.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.gollek.tools.*;
import tech.kayys.gollek.tools.spi.Tool;
import tech.kayys.gollek.tools.spi.ToolContext;
import tech.kayys.gollek.tools.spi.ToolResult;

import java.util.Map;

/**
 * Think tool — allows the agent to reason internally before taking action.
 */
@ApplicationScoped
public class ThinkTool implements CodeTool {

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
        return ToolRegistry.schema("thought", "Your internal reasoning or plan");
    }

    @Override
    public ToolResult execute(Map<String, Object> params, CodeToolContext context) {
        String thought = String.valueOf(params.getOrDefault("thought", ""));
        log.debug("[think] {}", thought);
        System.out.println("\n  💭 " + thought.replace("\n", "\n     ") + "\n");
        return ToolResult.success("Thought recorded.");
    }
}
