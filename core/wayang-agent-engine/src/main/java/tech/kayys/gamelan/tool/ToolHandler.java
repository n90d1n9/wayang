package tech.kayys.gamelan.tool;

import tech.kayys.gamelan.agent.ToolCall;

import java.util.List;

/**
 * SPI for built-in and skill-provided tools.
 *
 * <p>Implementations are discovered via CDI and registered automatically
 * by {@link ToolExecutor}.
 *
 * <p>A handler may support one or more tool names (e.g. {@code read_file} and
 * {@code read_files} could share the same handler).
 */
public interface ToolHandler {

    /**
     * The primary tool name this handler supports.
     */
    String toolName();

    /**
     * All tool names this handler supports (defaults to just {@link #toolName()}).
     */
    default List<String> toolNames() {
        return List.of(toolName());
    }

    /**
     * A short description of what this tool does, used in the system prompt.
     */
    String description();

    /**
     * Parameters this tool accepts, used to generate system prompt documentation.
     * Format: "param_name - description"
     */
    default List<String> parameters() {
        return List.of();
    }

    /**
     * Executes the tool call.
     *
     * @param call the tool call with parameters
     * @return the result
     */
    ToolResult execute(ToolCall call);
}
