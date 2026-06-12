package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.util.List;

/**
 * Think tool — a scratchpad for the LLM to reason step by step before acting.
 *
 * <h2>Why this matters</h2>
 * Research (Anthropic's "think" tool paper, 2024) shows that giving the model
 * a dedicated "thinking" step before tool calls significantly improves accuracy
 * on complex tasks, especially multi-step refactoring and debugging. Claude Code
 * uses extended thinking. We implement a lightweight equivalent.
 *
 * <h2>How it works</h2>
 * The LLM calls {@code think} before doing anything ambiguous. The tool simply
 * echoes the reasoning back — it acts as a "scratchpad" whose output is visible
 * in the message history, helping the model stay on track across iterations.
 *
 * <pre>{@code
 * <tool_call>
 *   <n>think</n>
 *   <reasoning>
 *     The user wants to add caching to UserService. Let me think through this:
 *     1. I should read UserService.java first to see what methods to cache.
 *     2. Check if there's already a caching dependency in pom.xml.
 *     3. Decide between Caffeine vs Spring Cache abstraction.
 *     4. The method getUserById() is called frequently — good cache candidate.
 *   </reasoning>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class ThinkTool implements ToolHandler {

    @Override public String toolName() { return "think"; }

    @Override public String description() {
        return "Think through a problem step-by-step before acting. "
                + "Use this before complex multi-step tasks, ambiguous requests, "
                + "or when you need to reason about trade-offs. "
                + "The reasoning is recorded and helps you stay consistent.";
    }

    @Override public List<String> parameters() {
        return List.of("reasoning - Your step-by-step reasoning and plan");
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String reasoning = call.param("reasoning").strip();
        if (reasoning.isBlank()) {
            return ToolResult.failure(toolName(), "'reasoning' is required");
        }
        // Echo it back — the result appears in the message history
        // which the model can reference in subsequent iterations
        return ToolResult.success(toolName(),
                "[Reasoning recorded]\n\n" + reasoning);
    }
}
