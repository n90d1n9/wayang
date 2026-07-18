package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;
import java.util.Map;

/**
 * A no-op scratchpad tool that lets the model externalize chain-of-thought reasoning.
 *
 * <p>When a model calls {@code think}, it provides a {@code thought} string that
 * is shown in the UI as a reasoning block (dim/italic) but is <em>not</em> stored
 * in the conversation history and has no side effects on the file system or process.
 *
 * <p>Purpose: models that do not support native thinking/reasoning blocks (extended
 * thinking) can still produce structured CoT by calling this tool. The TUI renders
 * it identically to a native thinking block — a 🤔 prefix with dim text.
 *
 * <p>This is safe to include in all agent configurations since it never mutates state.
 */
public final class ThinkTool implements Tool {

    @Override public String id() { return "think"; }
    @Override public String name() { return "think"; }

    @Override public String description() {
        return "Use this tool to think through a complex problem before taking action. " +
               "The thought is shown to the user but does not affect the file system or any state. " +
               "Useful for: planning a multi-step implementation, reasoning about tradeoffs, " +
               "checking your understanding before making changes, or decomposing a large task " +
               "into sub-tasks. Always prefer thinking before editing complex code.";
    }

    @Override public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "thought", Schema.string(
                        "Your reasoning, plan, or analysis. Write freely — this is your scratchpad.")
        ), "thought");
        
    }

    

    @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
        try {
        // The actual thought content is rendered by the TUI via onToolCallReady/onToolResult;
        // this tool itself just acknowledges receipt so the agent can continue.
        String thought = (String) params.get("thought");
        int wordCount = thought == null ? 0 : thought.split("\\s+").length;
        return ToolResult.success("Thinking recorded (" + wordCount + " words). Proceed with your plan.");
    
        } catch (Exception e) {
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
}
