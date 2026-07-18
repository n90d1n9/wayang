package tech.kayys.gamelan.tool;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Immutable result from executing a single tool call.
 *
 * <h2>XML rendering</h2>
 * {@link #toXml()} produces the block that is injected back into the LLM
 * context as a tool result. Large outputs are truncated at 50 KB to prevent
 * context-window overflows. The truncation marker tells the model to use
 * targeted reads (e.g. {@code read_file} with line ranges) to get the rest.
 *
 * @param toolName  name of the tool that produced this result
 * @param output    combined stdout/result content (may be multi-line)
 * @param exitCode  0 = success, non-zero = failure
 * @param error     human-readable error description (null on success)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolResult(
        String toolName,
        String output,
        int    exitCode,
        String error
) {
    /** Maximum characters of output included in the XML block. */
    private static final int MAX_XML_CHARS = 50_000;

    // ── Factory methods ────────────────────────────────────────────────────

    public static ToolResult success(String toolName, String output) {
        return new ToolResult(toolName, output != null ? output : "", 0, null);
    }

    public static ToolResult failure(String toolName, String error) {
        return new ToolResult(toolName, "", 1, error != null ? error : "unknown error");
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public boolean isSuccess()  { return exitCode == 0; }
    public boolean isFailure()  { return exitCode != 0; }

    /**
     * Returns output truncated to {@value #MAX_XML_CHARS} characters with a
     * trailing notice when truncation occurred.
     */
    public String truncatedOutput() {
        if (output == null || output.length() <= MAX_XML_CHARS) return output;
        return output.substring(0, MAX_XML_CHARS)
                + "\n\n... OUTPUT TRUNCATED ("
                + (output.length() - MAX_XML_CHARS)
                + " more chars). Use targeted reads to get the rest.";
    }

    /**
     * Renders this result as an XML block for LLM context injection.
     *
     * <p>Success:
     * <pre>{@code
     * <tool_result name="read_file">
     *   [content]
     * </tool_result>
     * }</pre>
     *
     * <p>Failure:
     * <pre>{@code
     * <tool_result name="read_file" status="error">
     *   [error message]
     * </tool_result>
     * }</pre>
     */
    public String toXml() {
        if (isSuccess()) {
            return "<tool_result name=\"" + toolName + "\">\n"
                    + truncatedOutput()
                    + "\n</tool_result>";
        } else {
            // Include truncated output if present (e.g. failed command still produces output)
            String body = (output != null && !output.isBlank())
                    ? error + "\n\nPartial output:\n" + truncatedOutput()
                    : error;
            return "<tool_result name=\"" + toolName + "\" status=\"error\">\n"
                    + body
                    + "\n</tool_result>";
        }
    }
}
