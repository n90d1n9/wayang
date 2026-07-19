package tech.kayys.gamelan.tool;

/**
 * Result of a tool execution.
 *
 * @param toolName  the name of the tool that was executed
 * @param output    the tool output (stdout or error message)
 * @param success   whether the execution was successful
 * @param exitCode  the exit code (0 for success, non-zero for errors)
 */
public record ToolResult(String toolName, String output, boolean success, int exitCode) {

    public static ToolResult success(String toolName, String output) {
        return new ToolResult(toolName, output, true, 0);
    }

    public static ToolResult error(String toolName, String errorMessage) {
        return new ToolResult(toolName, errorMessage, false, 1);
    }

    public static ToolResult error(String toolName, String errorMessage, int exitCode) {
        return new ToolResult(toolName, errorMessage, false, exitCode);
    }

    /**
     * Returns the output, truncated to the given maximum length.
     */
    public String outputTruncated(int maxLength) {
        if (output == null) return "(no output)";
        if (output.length() <= maxLength) return output;
        return output.substring(0, maxLength / 2)
                + "\n\n...[TRUNCATED — " + output.length() + " chars total]...\n\n"
                + output.substring(output.length() - maxLength / 2);
    }

    /**
     * Returns XML representation for the legacy prompt injection.
     */
    public String toXml() {
        return "<tool_result name=\"" + toolName + "\" success=\"" + success + "\" exitCode=\"" + exitCode + "\">\n"
                + escapeXml(output) + "\n</tool_result>";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
