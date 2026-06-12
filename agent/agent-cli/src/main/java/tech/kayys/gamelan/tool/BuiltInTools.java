package tech.kayys.gamelan.tool;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides descriptions of built-in tools for the system prompt.
 *
 * <p>This replaces the XML-based tool protocol with human-readable descriptions
 * that the LLM can use to understand available tools.
 */
@ApplicationScoped
public class BuiltInTools {

    private static final Map<String, ToolDescription> TOOLS = new LinkedHashMap<>();

    static {
        TOOLS.put("read_file", new ToolDescription(
                "read_file",
                "Read the contents of a file at the specified path.",
                Map.of(
                        "path", "Path to the file to read (required)",
                        "offset", "Optional: starting line number (0-based)",
                        "limit", "Optional: maximum number of lines to read"
                )
        ));

        TOOLS.put("write_file", new ToolDescription(
                "write_file",
                "Write content to a file at the specified path. Creates parent directories if needed.",
                Map.of(
                        "path", "Path to the file to write (required)",
                        "content", "Content to write to the file (required)"
                )
        ));

        TOOLS.put("shell", new ToolDescription(
                "shell",
                "Execute a shell command in the working directory. Returns stdout+stderr combined.",
                Map.of(
                        "command", "The shell command to execute (required)",
                        "timeout", "Optional: timeout in seconds (default: 120)"
                )
        ));

        TOOLS.put("list_dir", new ToolDescription(
                "list_dir",
                "List the contents of a directory. Shows files and subdirectories.",
                Map.of(
                        "path", "Path to the directory to list (default: current directory)"
                )
        ));

        TOOLS.put("search_files", new ToolDescription(
                "search_files",
                "Search for files matching a glob pattern within a directory.",
                Map.of(
                        "path", "Directory to search in (default: current directory)",
                        "pattern", "Glob pattern to match (e.g., '*.java', '**/*.py')"
                )
        ));

        TOOLS.put("grep", new ToolDescription(
                "grep",
                "Search for a text pattern within file contents.",
                Map.of(
                        "path", "File or directory to search in (default: current directory)",
                        "pattern", "Text pattern to search for (required)"
                )
        ));

        TOOLS.put("think", new ToolDescription(
                "think",
                "Record a thinking/reasoning step. Use to plan complex tasks before acting.",
                Map.of(
                        "thought", "Your reasoning or planning text (required)"
                )
        ));
    }

    /**
     * Returns a formatted description of all available tools for injection into the system prompt.
     */
    public String describeAll() {
        StringBuilder sb = new StringBuilder();
        sb.append("The following tools are available. Use them as needed:\n\n");
        for (ToolDescription tool : TOOLS.values()) {
            sb.append("### ").append(tool.name()).append("\n");
            sb.append(tool.description()).append("\n");
            sb.append("Parameters:\n");
            for (Map.Entry<String, String> param : tool.parameters().entrySet()) {
                String required = param.getValue().contains("(required)") ? "*" : " ";
                sb.append(String.format("  %s %s — %s\n", required, param.getKey(), param.getValue()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns the tool names as a list.
     */
    public List<String> getToolNames() {
        return List.copyOf(TOOLS.keySet());
    }

    /**
     * Returns true if the given tool name is a known built-in tool.
     */
    public boolean isKnownTool(String name) {
        return TOOLS.containsKey(name);
    }

    private record ToolDescription(String name, String description, Map<String, String> parameters) {}
}
